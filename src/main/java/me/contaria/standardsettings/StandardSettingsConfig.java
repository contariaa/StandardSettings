package me.contaria.standardsettings;

import com.google.gson.JsonParseException;
import com.mojang.serialization.Codec;
import me.contaria.speedrunapi.config.SpeedrunConfigAPI;
import me.contaria.speedrunapi.config.SpeedrunConfigContainer;
import me.contaria.speedrunapi.config.api.SpeedrunConfig;
import me.contaria.speedrunapi.config.api.SpeedrunOption;
import me.contaria.speedrunapi.config.api.annotations.Config;
import me.contaria.speedrunapi.util.TextUtil;
import me.contaria.standardsettings.compat.SodiumCompat;
import me.contaria.standardsettings.gui.StandardSettingsLanguageScreen;
import me.contaria.standardsettings.interfaces.StandardSettingsSimpleOption;
import me.contaria.standardsettings.mixin.accessors.MinecraftClientAccessor;
import me.contaria.standardsettings.mixin.accessors.SimpleOptionAccessor;
import me.contaria.standardsettings.options.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ConfirmScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.option.*;
import net.minecraft.client.resource.language.LanguageDefinition;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.Monitor;
import net.minecraft.client.util.VideoMode;
import net.minecraft.client.util.Window;
import net.minecraft.entity.player.PlayerModelPart;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.profiler.ProfileResult;
import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

@Config(init = Config.InitPoint.POSTLAUNCH)
public class StandardSettingsConfig implements SpeedrunConfig {
    @Config.Ignored
    public final List<StandardSetting<?>> standardSettings = new ArrayList<>();
    @Config.Ignored
    public final List<StandardSetting<?>> standardSettingsOnWorldJoin = new ArrayList<>();

    @Config.Ignored
    private SpeedrunConfigContainer<?> configContainer;
    @Config.Ignored
    private long fileLastModified;

    public boolean toggleStandardSettings = true;

    @SuppressWarnings("unused")
    public boolean toggleAll = true;

    @Config.Category("f3")
    public boolean autoF3Esc = false;

    @SuppressWarnings("all")
    @Config.Category("f3")
    @Config.Numbers.Whole.Bounds(max = 100)
    private int firstAutoF3EscDelay = 22;

    @Config.Ignored
    public int autoF3EscDelay;

    @Config.Category("onWorldJoin")
    public boolean triggerOnResize = false;

    @Config.Ignored
    @Nullable
    private KeyBindingStandardSetting focusedKeyBinding;

    {
        StandardSettings.config = this;

        GameOptions options = MinecraftClient.getInstance().options;

        this.register("fov", "menu.options", options.getFov());

        // Online Options
        this.register("realmsNotifications", "options.online", options.getRealmsNotifications());
        this.register("allowServerListing", "options.online", options.getAllowServerListing());

        // Video Settings
        this.register(CustomStandardSetting.ofString(
                "fullscreenResolution",
                "options.video",
                () -> MinecraftClient.getInstance().getWindow().getFullscreenVideoMode().map(VideoMode::asString).orElse(null),
                value -> MinecraftClient.getInstance().getWindow().setFullscreenVideoMode(VideoMode.fromString(value)),
                value -> VideoMode.fromString(value).map(mode -> TextUtil.literal(mode.toString())).orElse(TextUtil.translatable("options.fullscreen.current")),
                setting -> {
                    // see FullScreenOption and VideoOptionsScreen#init
                    Window window = MinecraftClient.getInstance().getWindow();
                    Monitor monitor = window.getMonitor();

                    int index;
                    if (monitor == null) {
                        index = -1;
                    } else {
                        index = VideoMode.fromString(setting.get()).map(monitor::findClosestVideoModeIndex).orElse(-1);
                    }

                    SimpleOption<Integer> simpleOption = new SimpleOption<>("options.fullscreen.resolution", SimpleOption.emptyTooltip(), (prefix, value) -> {
                        if (monitor == null) {
                            return TextUtil.translatable("options.fullscreen.unavailable");
                        }
                        if (value == -1) {
                            return TextUtil.translatable("options.fullscreen.current");
                        }
                        return TextUtil.literal(monitor.getVideoMode(value).toString());
                    }, new SimpleOption.ValidatingIntSliderCallbacks(-1, monitor != null ? monitor.getVideoModeCount() - 1 : -1), index, value -> {
                        if (monitor == null) {
                            return;
                        }
                        if (value == -1) {
                            setting.set(null);
                        } else {
                            setting.set(monitor.getVideoMode(value).asString());
                        }
                    });

                    return simpleOption.createWidget(MinecraftClient.getInstance().options, 0, 0, 120);
                })
        );

        this.register("biomeBlendRadius", "options.video", options.getBiomeBlendRadius());
        this.register(new SimpleOptionStandardSetting<GraphicsMode>("graphicsMode", "options.video", options.getGraphicsMode()) {
            @Override
            public void setVanilla(GraphicsMode value) {
                // see Option.GRAPHICS's setter
                if (MinecraftClient.getInstance().isRunning() && MinecraftClient.getInstance().getVideoWarningManager().hasCancelledAfterWarning() && value == GraphicsMode.FABULOUS) {
                    StandardSettings.LOGGER.warn("Set Graphics Mode to 'Fancy' because 'Fabulous!' is not supported on this device.");
                    super.setVanilla(GraphicsMode.FANCY);
                } else {
                    super.setVanilla(value);
                }
            }
        });
        this.register("renderDistance", "options.video", options.getViewDistance());
        this.register("prioritizeChunkUpdates", "options.video", options.getChunkBuilderMode());
        this.register("simulationDistance", "options.video", options.getSimulationDistance());
        this.register("ao", "options.video", options.getAo());
        this.register("maxFps", "options.video", options.getMaxFps());
        this.register("enableVsync", "options.video", options.getEnableVsync());
        this.register("bobView", "options.video", options.getBobView());
        this.register(new SimpleOptionStandardSetting<>("guiScale", "options.video", options.getGuiScale()) {
            @Override
            public void set(SimpleOption<Integer> option, Integer value) {
                // bypass vanillas dynamic bounds enforcing for gui scale
                //noinspection unchecked
                ((SimpleOptionAccessor<Integer>) (Object) option).standardsettings$setValue(Math.max(0, value));
            }
        });
        this.register("attackIndicator", "options.video", options.getAttackIndicator());
        this.register(new SimpleOptionStandardSetting<>("gamma", "options.video", options.getGamma()) {
            @Override
            protected @NotNull SimpleOption<Double> copySimpleOption(SimpleOption<Double> option) {
                //noinspection unchecked
                return ((StandardSettingsSimpleOption<Double>) (Object) option).standardsettings$copy(
                        // allows brightness of up to 500% in StandardSettings
                        // Planifolia is still needed to allow the 500% to apply
                        new SimpleOption.SliderCallbacks<>() {
                            @Override
                            public double toSliderProgress(Double value) {
                                return value / 5.0;
                            }

                            @Override
                            public Double toValue(double sliderProgress) {
                                return sliderProgress * 5.0;
                            }

                            @Override
                            public Optional<Double> validate(Double value) {
                                return value >= 0.0 && value <= 5.0 ? Optional.of(value) : Optional.empty();
                            }

                            @Override
                            public Codec<Double> codec() {
                                return Codec.doubleRange(0.0, 5.0);
                            }
                        }
                );
            }

            @Override
            public void setVanilla(Double value) {
                // limits value to 1.0 if it's invalid
                if (this.option.getCallbacks().validate(value).isPresent()) {
                    super.setVanilla(value);
                } else {
                    super.setVanilla(Math.min(1.0, value));
                }
            }
        });
        this.register("renderClouds", "options.video", options.getCloudRenderMode());
        this.register("fullscreen", "options.video", options.getFullscreen());
        this.register("particles", "options.video", options.getParticles());
        this.register("mipmapLevels", "options.video", options.getMipmapLevels());
        this.register("entityShadows", "options.video", options.getEntityShadows());
        this.register("screenEffectScale", "options.video", options.getDistortionEffectScale());
        this.register("entityDistanceScaling", "options.video", options.getEntityDistanceScaling());
        this.register("fovEffectScale", "options.video", options.getFovEffectScale());
        this.register("showAutosaveIndicator", "options.video", options.getShowAutosaveIndicator());
        this.register("glintSpeed", "options.video", options.getGlintSpeed());
        this.register("glintStrength", "options.video", options.getGlintStrength());
        this.register("menuBackgroundBlurriness", "options.video", options.getMenuBackgroundBlurriness());
        this.register("entityCulling", "options.video", () -> StandardSettings.HAS_SODIUM && SodiumCompat.getEntityCulling(), value -> {
            if (StandardSettings.HAS_SODIUM) {
                SodiumCompat.setEntityCulling(value);
            }
        }).setVisible(StandardSettings.HAS_SODIUM);

        // Skin Customizations
        for (PlayerModelPart playerModelPart : PlayerModelPart.values()) {
            this.register(new PlayerModelPartStandardSetting("modelPart_" + playerModelPart.getName(), "options.skinCustomisation", playerModelPart));
        }
        this.register("mainHand", "options.skinCustomisation", options.getMainArm());

        // Music & Sounds
        for (SoundCategory soundCategory : SoundCategory.values()) {
            this.register("soundCategory_" + soundCategory.getName(), "options.sounds", options.getSoundVolumeOption(soundCategory));
        }
        this.register("showSubtitles", "options.sounds", options.getShowSubtitles());
        this.register(new SimpleOptionStandardSetting<>("soundDevice", "options.sounds", options.getSoundDevice()) {
            @Override
            protected @NotNull SimpleOption<String> copySimpleOption(SimpleOption<String> option) {
                //noinspection unchecked
                return ((StandardSettingsSimpleOption<String>) (Object) option).standardsettings$copy(
                        // sound devices are validated by checking if they are in SoundManager#getDevices
                        // to ensure this works before the sound manager is initialized, vanilla checks MinecraftClient#isRunning
                        // StandardSettings loads after running has been set to true but before the sound manager is loaded
                        new SimpleOption.Callbacks<>() {
                            @Override
                            public Function<SimpleOption<String>, ClickableWidget> getWidgetCreator(SimpleOption.TooltipFactory<String> tooltipFactory, GameOptions gameOptions, int x, int y, int width, Consumer<String> changeCallback) {
                                return option.getCallbacks().getWidgetCreator(tooltipFactory, gameOptions, x, y, width, changeCallback);
                            }

                            @Override
                            public Optional<String> validate(String value) {
                                // skip validating sound device
                                return Optional.of(value);
                            }

                            @Override
                            public Codec<String> codec() {
                                return option.getCodec();
                            }
                        }
                );
            }

            @Override
            public void set(String value) {
                //noinspection unchecked
                ((SimpleOptionAccessor<String>) (Object) this.copy).standardsettings$setValue(value);
            }
        });
        this.register("directionalAudio", "options.sounds", options.getDirectionalAudio());

        // Language
        this.register(CustomStandardSetting.ofString(
                "language", "options.language",
                () -> MinecraftClient.getInstance().getLanguageManager().getLanguage(),
                value -> MinecraftClient.getInstance().options.language = value,
                value -> {
                    LanguageDefinition language = MinecraftClient.getInstance().getLanguageManager().getLanguage(value);
                    if (language != null) {
                        return language.getDisplayText();
                    }
                    return TextUtil.literal(value);
                },
                setting -> ButtonWidget.builder(setting.getText(), button -> MinecraftClient.getInstance().setScreen(
                        new StandardSettingsLanguageScreen(MinecraftClient.getInstance(), setting)
                )).size(120, 20).build()
        ));
        this.register("forceUnicodeFont", "options.language", options.getForceUnicodeFont());
        this.register("japaneseGlyphVariants", "options.language", options.getJapaneseGlyphVariants());

        // Mouse Settings
        this.register("mouseSensitivity", "options.mouse_settings", options.getMouseSensitivity());
        this.register("invertYMouse", "options.mouse_settings", options.getInvertYMouse());
        this.register("mouseWheelSensitivity", "options.mouse_settings", options.getMouseWheelSensitivity());
        this.register("discrete_mouse_scroll", "options.mouse_settings", options.getDiscreteMouseScroll());
        this.register("touchscreen", "options.mouse_settings", options.getTouchscreen());
        this.register("rawMouseInput", "options.mouse_settings", options.getRawMouseInput()).setVisible(InputUtil::isRawMouseMotionSupported);

        // Controls
        this.register("toggleCrouch", "options.controls", options.getSneakToggled());
        this.register("toggleSprint", "options.controls", options.getSprintToggled());
        this.register("autoJump", "options.controls", options.getAutoJump());
        this.register("operatorItemsTab", "options.controls", options.getOperatorItemsTab());
        KeyBinding[] keyBindings = ArrayUtils.clone(MinecraftClient.getInstance().options.allKeys);
        Arrays.sort(keyBindings);
        for (KeyBinding keyBinding : keyBindings) {
            this.register(new KeyBindingStandardSetting("key_" + keyBinding.getTranslationKey(), keyBinding.getCategory(), keyBinding));
        }

        // Chat Settings
        this.register("chatVisibility", "options.chat.title", options.getChatVisibility());
        this.register("chatColors", "options.chat.title", options.getChatColors());
        this.register("chatLinks", "options.chat.title", options.getChatLinks());
        this.register("chatLinksPrompt", "options.chat.title", options.getChatLinksPrompt());
        this.register("chatOpacity", "options.chat.title", options.getChatOpacity());
        this.register("textBackgroundOpacity", "options.chat.title", options.getTextBackgroundOpacity());
        this.register("chatScale", "options.chat.title", options.getChatScale());
        this.register("chatLineSpacing", "options.chat.title", options.getChatLineSpacing());
        this.register("chatDelay", "options.chat.title", options.getChatDelay());
        this.register("chatWidth", "options.chat.title", options.getChatWidth());
        this.register("chatHeightFocused", "options.chat.title", options.getChatHeightFocused());
        this.register("chatHeightUnfocused", "options.chat.title", options.getChatHeightUnfocused());
        this.register("narrator", "options.chat.title", options.getNarrator());
        this.register("autoSuggestions", "options.chat.title", options.getAutoSuggestions());
        this.register("hideMatchedNames", "options.chat.title", options.getHideMatchedNames());
        this.register("reducedDebugInfo", "options.chat.title", options.getReducedDebugInfo());
        this.register("onlyShowSecureChat", "options.chat.title", options.getOnlyShowSecureChat());

        // Accessibility Settings
        this.register("highContrast", "options.accessibility.title", options.getHighContrast());
        this.register("backgroundForChatOnly", "options.accessibility.title", options.getBackgroundForChatOnly());
        this.register("notificationDisplayTime", "options.accessibility.title", options.getNotificationDisplayTime());
        this.register("darknessEffectScale", "options.accessibility.title", options.getDarknessEffectScale());
        this.register("damageTiltStrength", "options.accessibility.title", options.getDamageTiltStrength());
        this.register("hideLightningFlashes", "options.accessibility.title", options.getHideLightningFlashes());
        this.register("darkMojangStudiosBackground", "options.accessibility.title", options.getMonochromeLogo());
        this.register("panoramaScrollSpeed", "options.accessibility.title", options.getPanoramaSpeed());

        // Telemetry Data
        this.register("telemetryOptInExtra", "options.telemetry", options.getTelemetryOptInExtra());

        // F3 Settings
        this.register("pauseOnLostFocus", "f3", () -> options.pauseOnLostFocus, value -> options.pauseOnLostFocus = value);
        this.register("advancedItemTooltips", "f3", () -> options.advancedItemTooltips, value -> options.advancedItemTooltips = value);
        this.register("hitboxes", "f3", () -> MinecraftClient.getInstance().getEntityRenderDispatcher().shouldRenderHitboxes(), value -> MinecraftClient.getInstance().getEntityRenderDispatcher().setRenderHitboxes(value));
        this.register("chunkborders", "f3", () -> {
            MinecraftClient.getInstance().debugRenderer.toggleShowChunkBorder();
            return MinecraftClient.getInstance().debugRenderer.toggleShowChunkBorder();
        }, value -> {
            if (MinecraftClient.getInstance().debugRenderer.toggleShowChunkBorder() != value) {
                MinecraftClient.getInstance().debugRenderer.toggleShowChunkBorder();
            }
        });
        this.register(CustomStandardSetting.ofString(
                "pieDirectory", "f3",
                () -> ProfileResult.getHumanReadableName(((MinecraftClientAccessor) MinecraftClient.getInstance()).standardsettings$getOpenProfilerSection()),
                value -> ((MinecraftClientAccessor) MinecraftClient.getInstance()).standardsettings$setOpenProfilerSection(value),
                value -> value.startsWith("root") ? value.replace('.', '\u001e').trim() : "root",
                TextUtil::literal,
                setting -> {
                    TextFieldWidget widget = new TextFieldWidget(MinecraftClient.getInstance().textRenderer, 0, 0, 120, 20, setting.getName());
                    widget.setMaxLength(128);
                    widget.setTextPredicate(string -> string != null && (string.startsWith("root") || string.isEmpty()));
                    widget.setText(setting.get());
                    widget.setChangedListener(string -> {
                        // allow for backspace / ctrl + x
                        if (string.isEmpty()) {
                            widget.setText("root");
                            return;
                        }
                        setting.set(string);
                        for (String suggestion : new String[]{
                                "root.gameRenderer.level.entities",
                                "root.tick.level.entities.blockEntities"
                        }) {
                            if (string.length() > 5 && !suggestion.equals(string) && suggestion.startsWith(string)) {
                                widget.setSuggestion(suggestion.replaceFirst(string, "").split("\\.")[0]);
                                return;
                            }
                        }
                        widget.setSuggestion(null);
                    });
                    return widget;
                })
        ).disable();

        // More Settings
        this.register(CustomStandardSetting.ofEnum("perspective", "more", options::getPerspective, options::setPerspective, Perspective.class)).disable();
        this.register("f1", "more", () -> options.hudHidden, value -> options.hudHidden = value).disable();
        this.register("sneaking", "more", options.sneakKey::isPressed, value -> {
            if (options.getSneakToggled().getValue() && options.sneakKey.isPressed() != value) {
                // pressing the sneak key toggles it
                options.sneakKey.setPressed(true);
            }
        }).disable();
        this.register("sprinting", "more", options.sprintKey::isPressed, value -> {
            if (options.getSprintToggled().getValue() && options.sprintKey.isPressed() != value) {
                // pressing the sprint key toggles it
                options.sprintKey.setPressed(true);
            }
        }).disable();

        // OnWorldJoin Settings
        this.onWorldJoin(new SimpleOptionStandardSetting<>("fovOnWorldJoin", "onWorldJoin", options.getFov())).disable();
        this.onWorldJoin(new SimpleOptionStandardSetting<>("renderDistanceOnWorldJoin", "onWorldJoin", options.getViewDistance())).disable();
        this.onWorldJoin(new SimpleOptionStandardSetting<>("entityDistanceScalingOnWorldJoin", "onWorldJoin", options.getEntityDistanceScaling())).disable();
        this.onWorldJoin(new SimpleOptionStandardSetting<>("guiScaleOnWorldJoin", "onWorldJoin", options.getGuiScale()) {
            @Override
            public void set(SimpleOption<Integer> option, Integer value) {
                // bypass vanillas dynamic bounds enforcing for gui scale
                //noinspection unchecked
                ((SimpleOptionAccessor<Integer>) (Object) option).standardsettings$setValue(Math.max(0, value));
            }
        });
    }

    private <T> SimpleOptionStandardSetting<T> register(String id, String category, SimpleOption<T> option) {
        return this.register(new SimpleOptionStandardSetting<>(id, category, option));
    }

    private CustomStandardSetting<Boolean> register(String id, String category, Supplier<Boolean> getter, Consumer<Boolean> setter) {
        return this.register(CustomStandardSetting.ofBoolean(id, category, getter, setter));
    }

    private <T extends StandardSetting<?>> T register(T standardSetting) {
        this.standardSettings.add(standardSetting);
        return standardSetting;
    }

    private <T extends StandardSetting<?>> T onWorldJoin(T standardSetting) {
        this.standardSettingsOnWorldJoin.add(standardSetting);
        return standardSetting;
    }

    @Override
    public String modID() {
        return "standardsettings";
    }

    @Override
    public Map<String, SpeedrunOption<?>> init() throws ReflectiveOperationException {
        Map<String, SpeedrunOption<?>> options = new LinkedHashMap<>();
        for (StandardSetting<?> setting : this.standardSettings) {
            if (options.put(setting.getID(), setting) != null) {
                throw new IllegalStateException("Tried to register " + setting.getID() + " twice!");
            }
        }
        for (StandardSetting<?> setting : this.standardSettingsOnWorldJoin) {
            if (options.put(setting.getID(), setting) != null) {
                throw new IllegalStateException("Tried to register " + setting.getID() + " twice!");
            }
        }
        options.putAll(SpeedrunConfig.super.init());
        return options;
    }

    private void toggleAll(boolean enabled) {
        for (StandardSetting<?> setting : StandardSettings.config.standardSettings) {
            setting.setEnabled(enabled);
        }
        for (StandardSetting<?> setting : StandardSettings.config.standardSettingsOnWorldJoin) {
            setting.setEnabled(enabled);
        }
    }

    private void confirmToggleAll(ButtonWidget button) {
        MinecraftClient client = MinecraftClient.getInstance();
        Screen screen = client.currentScreen;
        client.setScreen(new ConfirmScreen(
                confirmed -> {
                    if (confirmed) {
                        this.toggleAll = !this.toggleAll;
                        this.toggleAll(this.toggleAll);
                    }
                    client.setScreen(screen);
                },
                TextUtil.translatable("speedrunapi.config.standardsettings.option.toggleAll"),
                TextUtil.translatable("speedrunapi.config.standardsettings.option.toggleAll.description")
                        .append(" ")
                        .append(TextUtil.translatable("speedrunapi.config.standardsettings.option.toggleAll.confirm")),
                ScreenTexts.PROCEED,
                ScreenTexts.CANCEL
        ));
    }

    @Override
    public @Nullable SpeedrunOption<?> parseField(Field field, SpeedrunConfig config, String... idPrefix) {
        if ("toggleAll".equals(field.getName())) {
            return new SpeedrunConfigAPI.CustomOption.Builder<Boolean>(this, this, field, idPrefix)
                    .createWidget((option, innerConfig, configStorage, optionField) ->
                            ButtonWidget.builder(ScreenTexts.onOrOff(!option.get()), this::confirmToggleAll).dimensions(0, 0, 150, 20).build()
                    ).build();
        }
        return SpeedrunConfig.super.parseField(field, config, idPrefix);
    }

    @Override
    public void finishInitialization(SpeedrunConfigContainer<?> container) {
        this.configContainer = container;
        this.fileLastModified = this.getConfigFile().lastModified();
        this.autoF3EscDelay = this.firstAutoF3EscDelay;
    }

    @Override
    public File getConfigFile() {
        Path globalRedirect = SpeedrunConfigAPI.getConfigDir().resolve("standardsettings.global");
        if (Files.exists(globalRedirect)) {
            try {
                File file = new File(new String(Files.readAllBytes(globalRedirect)).trim());
                if (file.isFile()) {
                    return file;
                }
                StandardSettings.LOGGER.warn("Failed to redirect to global StandardSettings");
            } catch (IOException e) {
                StandardSettings.LOGGER.warn("Failed to read StandardSettings global redirect");
            }
        }
        return SpeedrunConfig.super.getConfigFile();
    }

    public void update() {
        long fileLastModified = this.getConfigFile().lastModified();
        if (this.fileLastModified != fileLastModified) {
            try {
                StandardSettings.LOGGER.info("StandardSettings config has been modified, reloading StandardSettings...");
                this.configContainer.load();
                this.fileLastModified = fileLastModified;
                StandardSettings.LOGGER.info("Finished reloading StandardSettings");
            } catch (IOException | JsonParseException e) {
                StandardSettings.LOGGER.warn("Failed to reload StandardSettings");
            }
        }
    }

    @Override
    public void finishSaving() {
        this.focusedKeyBinding = null;
    }

    @Override
    public @Nullable Predicate<InputUtil.Key> createInputListener() {
        return key -> {
            if (this.focusedKeyBinding != null) {
                if (key.getCategory() == InputUtil.Type.KEYSYM && key.getCode() == GLFW.GLFW_KEY_ESCAPE) {
                    key = InputUtil.UNKNOWN_KEY;
                }
                this.focusedKeyBinding.set(key);
                this.focusedKeyBinding = null;
                return true;
            }
            return false;
        };
    }

    public void setFocusedKeyBinding(@Nullable KeyBindingStandardSetting keyBinding) {
        this.focusedKeyBinding = keyBinding;
    }

    public boolean isFocusedKeyBinding(KeyBindingStandardSetting keyBinding) {
        return this.focusedKeyBinding == keyBinding;
    }

    public boolean hasFocusedKeyBinding() {
        return this.focusedKeyBinding != null;
    }

    @Override
    public @NotNull Screen createConfigScreen(Screen parent) {
        this.update();
        return SpeedrunConfig.super.createConfigScreen(parent);
    }
}
