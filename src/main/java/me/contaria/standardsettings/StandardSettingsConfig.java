package me.contaria.standardsettings;

import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Monitor;
import com.mojang.blaze3d.platform.VideoMode;
import com.mojang.blaze3d.platform.Window;
import com.mojang.serialization.Codec;
import me.contaria.speedrunapi.config.SpeedrunConfigAPI;
import me.contaria.speedrunapi.config.SpeedrunConfigContainer;
import me.contaria.speedrunapi.config.api.SpeedrunConfig;
import me.contaria.speedrunapi.config.api.SpeedrunOption;
import me.contaria.speedrunapi.config.api.annotations.Config;
import me.contaria.standardsettings.compat.SodiumCompat;
import me.contaria.standardsettings.gui.StandardSettingsLanguageScreen;
import me.contaria.standardsettings.interfaces.StandardSettingsSimpleOption;
import me.contaria.standardsettings.mixin.accessors.OptionListAccessor;
import me.contaria.standardsettings.mixin.accessors.PieChartAccessor;
import me.contaria.standardsettings.mixin.accessors.SimpleOptionAccessor;
import me.contaria.standardsettings.options.*;
import net.minecraft.client.*;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.debug.DebugScreenEntries;
import net.minecraft.client.gui.components.debug.DebugScreenEntry;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.LanguageInfo;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.profiling.ProfileResults;
import net.minecraft.world.entity.player.PlayerModelPart;
import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import javax.swing.*;
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

        Options options = Minecraft.getInstance().options;

        this.register("fov", "menu.options", options.fov());

        // Online Options
        this.register("realmsNotifications", "options.online", options.realmsNotifications());
        this.register("allowServerListing", "options.online", options.allowServerListing());

        // Video Settings
        this.register(CustomStandardSetting.ofString(
                "fullscreenResolution",
                "options.video",
                () -> Minecraft.getInstance().getWindow().getPreferredFullscreenVideoMode().map(VideoMode::write).orElse(null),
                value -> Minecraft.getInstance().getWindow().setPreferredFullscreenVideoMode(VideoMode.read(value)),
                value -> VideoMode.read(value).map(mode -> Component.literal(mode.toString())).orElse(Component.translatable("options.fullscreen.current")),
                setting -> {
                    // see FullScreenOption and VideoOptionsScreen#init
                    Window window = Minecraft.getInstance().getWindow();
                    Monitor monitor = window.findBestMonitor();

                    int index;
                    if (monitor == null) {
                        index = -1;
                    } else {
                        index = VideoMode.read(setting.get()).map(monitor::getVideoModeIndex).orElse(-1);
                    }

                    OptionInstance<Integer> simpleOption = new OptionInstance<>("options.fullscreen.resolution", OptionInstance.noTooltip(), (prefix, value) -> {
                        if (monitor == null) {
                            return Component.translatable("options.fullscreen.unavailable");
                        }
                        if (value == -1) {
                            return Component.translatable("options.fullscreen.current");
                        }
                        return Component.literal(monitor.getMode(value).toString());
                    }, new OptionInstance.IntRange(-1, monitor != null ? monitor.getModeCount() - 1 : -1), index, value -> {
                        if (monitor == null) {
                            return;
                        }
                        if (value == -1) {
                            setting.set(null);
                        } else {
                            setting.set(monitor.getMode(value).write());
                        }
                    });

                    return simpleOption.createButton(Minecraft.getInstance().options, 0, 0, 120);
                })
        );

        this.register("biomeBlendRadius", "options.video", options.biomeBlendRadius());
        this.register("renderDistance", "options.video", options.renderDistance());
        this.register("prioritizeChunkUpdates", "options.video", options.prioritizeChunkUpdates());
        this.register("simulationDistance", "options.video", options.simulationDistance());
        this.register("ao", "options.video", options.ambientOcclusion());
        this.register("maxFps", "options.video", options.framerateLimit());
        this.register("enableVsync", "options.video", options.enableVsync());
        this.register("inactivityFpsLimit", "options.video", options.inactivityFpsLimit());
        this.register(new SimpleOptionStandardSetting<>("guiScale", "options.video", options.guiScale()) {
            @Override
            public void set(OptionInstance<Integer> option, Integer value) {
                // bypass vanillas dynamic bounds enforcing for gui scale
                //noinspection unchecked
                ((SimpleOptionAccessor<Integer>) (Object) option).standardsettings$setValue(Math.max(0, value));
            }
        });
        this.register("attackIndicator", "options.video", options.attackIndicator());
        this.register(new SimpleOptionStandardSetting<>("gamma", "options.video", options.gamma()) {
            @Override
            protected @NotNull OptionInstance<Double> copySimpleOption(OptionInstance<Double> option) {
                //noinspection unchecked
                return ((StandardSettingsSimpleOption<Double>) (Object) option).standardsettings$copy(
                        // allows brightness of up to 500% in StandardSettings
                        // Planifolia is still needed to allow the 500% to apply
                        new OptionInstance.SliderableValueSet<>() {
                            @Override
                            public double toSliderValue(Double value) {
                                return value / 5.0;
                            }

                            @Override
                            public Double fromSliderValue(double sliderProgress) {
                                return sliderProgress * 5.0;
                            }

                            @Override
                            public Optional<Double> validateValue(Double value) {
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
                if (this.option.values().validateValue(value).isPresent()) {
                    super.setVanilla(value);
                } else {
                    super.setVanilla(Math.min(1.0, value));
                }
            }
        });
        this.register("renderClouds", "options.video", options.cloudStatus());
        this.register("cloudRange", "options.video", options.cloudRange());
        this.register("cutoutLeaves", "options.video", options.cutoutLeaves());
        this.register("improvedTransparency", "options.video", options.improvedTransparency());
        this.register("textureFiltering", "options.video", options.textureFiltering());
        this.register("maxAnisotropyBit", "options.video", options.maxAnisotropyBit());
        this.register("weatherRadius", "options.video", options.weatherRadius());
        this.register("fullscreen", "options.video", options.fullscreen());
        this.register("exclusiveFullscreen", "options.video", options.exclusiveFullscreen());
        this.register("particles", "options.video", options.particles());
        this.register("mipmapLevels", "options.video", options.mipmapLevels());
        this.register("entityShadows", "options.video", options.entityShadows());
        this.register("screenEffectScale", "options.video", options.screenEffectScale());
        this.register("entityDistanceScaling", "options.video", options.entityDistanceScaling());
        this.register("fovEffectScale", "options.video", options.fovEffectScale());
        this.register("showAutosaveIndicator", "options.video", options.showAutosaveIndicator());
        this.register("vignette", "options.video", options.vignette());
        this.register("chunkSectionFadeInTime", "options.video", options.chunkSectionFadeInTime());
        this.register("glintSpeed", "options.video", options.glintSpeed());
        this.register("glintStrength", "options.video", options.glintStrength());
        this.register("menuBackgroundBlurriness", "options.video", options.menuBackgroundBlurriness());
        this.register("bobView", "options.video", options.bobView());
        this.register("entityCulling", "options.video", () -> StandardSettings.HAS_SODIUM && SodiumCompat.getEntityCulling(), value -> {
            if (StandardSettings.HAS_SODIUM) {
                SodiumCompat.setEntityCulling(value);
            }
        }).setVisible(StandardSettings.HAS_SODIUM);

        // Skin Customizations
        for (PlayerModelPart playerModelPart : PlayerModelPart.values()) {
            this.register(new PlayerModelPartStandardSetting("modelPart_" + playerModelPart.getId(), "options.skinCustomisation", playerModelPart));
        }
        this.register("mainHand", "options.skinCustomisation", options.mainHand());

        // Music & Sounds
        for (SoundSource soundCategory : SoundSource.values()) {
            this.register("soundCategory_" + soundCategory.getName(), "options.sounds", options.getSoundSourceOptionInstance(soundCategory));
        }
        this.register("showSubtitles", "options.sounds", options.showSubtitles());
        this.register(new SimpleOptionStandardSetting<>("soundDevice", "options.sounds", options.soundDevice()) {
            @Override
            protected @NotNull OptionInstance<String> copySimpleOption(OptionInstance<String> option) {
                //noinspection unchecked
                return ((StandardSettingsSimpleOption<String>) (Object) option).standardsettings$copy(
                        // sound devices are validated by checking if they are in SoundManager#getDevices
                        // to ensure this works before the sound manager is initialized, vanilla checks MinecraftClient#isRunning
                        // StandardSettings loads after running has been set to true but before the sound manager is loaded
                        new OptionInstance.ValueSet<>() {
                            @Override
                            public Function<OptionInstance<String>, AbstractWidget> createButton(OptionInstance.TooltipSupplier<String> tooltipFactory, Options gameOptions, int x, int y, int width, Consumer<String> changeCallback) {
                                return option.values().createButton(tooltipFactory, gameOptions, x, y, width, changeCallback);
                            }

                            @Override
                            public Optional<String> validateValue(String value) {
                                // skip validating sound device
                                return Optional.of(value);
                            }

                            @Override
                            public Codec<String> codec() {
                                return option.codec();
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
        this.register("directionalAudio", "options.sounds", options.directionalAudio());
        this.register("musicToast", "options.sounds", options.musicToast());
        this.register("musicFrequency", "options.sounds", options.musicFrequency());

        // Language
        this.register(CustomStandardSetting.ofString(
                "language", "options.language",
                () -> options.languageCode,
                value -> options.languageCode = value,
                value -> {
                    LanguageInfo language = Minecraft.getInstance().getLanguageManager().getLanguage(value);
                    if (language != null) {
                        return language.toComponent();
                    }
                    return Component.literal(value);
                },
                setting -> Button.builder(setting.getText(), button -> Minecraft.getInstance().setScreen(
                        new StandardSettingsLanguageScreen(Minecraft.getInstance(), setting)
                )).size(120, 20).build()
        ));
        this.register("forceUnicodeFont", "options.language", options.forceUnicodeFont());
        this.register("japaneseGlyphVariants", "options.language", options.japaneseGlyphVariants());

        // Mouse Settings
        this.register("mouseSensitivity", "options.mouse_settings", options.sensitivity());
        this.register("invertXMouse", "options.mouse_settings", options.invertMouseX());
        this.register("invertYMouse", "options.mouse_settings", options.invertMouseY());
        this.register("mouseWheelSensitivity", "options.mouse_settings", options.mouseWheelSensitivity());
        this.register("discrete_mouse_scroll", "options.mouse_settings", options.discreteMouseScroll());
        this.register("touchscreen", "options.mouse_settings", options.touchscreen());
        this.register("rawMouseInput", "options.mouse_settings", options.rawMouseInput()).setVisible(InputConstants::isRawMouseInputSupported);
        this.register("allowCursorChanges", "options.mouse_settings", options.allowCursorChanges());

        // Controls
        this.register("toggleCrouch", "options.controls", options.toggleCrouch());
        this.register("toggleSprint", "options.controls", options.toggleSprint());
        this.register("toggleAttack", "options.controls", options.toggleAttack());
        this.register("toggleUse", "options.controls", options.toggleUse());
        this.register("autoJump", "options.controls", options.autoJump());
        this.register("sprintWindow", "options.controls", options.sprintWindow());
        this.register("operatorItemsTab", "options.controls", options.operatorItemsTab());
        KeyMapping[] keyBindings = ArrayUtils.clone(Minecraft.getInstance().options.keyMappings);
        Arrays.sort(keyBindings);
        for (KeyMapping keyBinding : keyBindings) {
            // TODO: category
            this.register(new KeyBindingStandardSetting("key_" + keyBinding.getName(), keyBinding.getCategory().id().toLanguageKey("key.category"), keyBinding));
        }

        // Chat Settings
        this.register("chatVisibility", "options.chat.title", options.chatVisibility());
        this.register("chatColors", "options.chat.title", options.chatColors());
        this.register("chatLinks", "options.chat.title", options.chatLinks());
        this.register("chatLinksPrompt", "options.chat.title", options.chatLinksPrompt());
        this.register("chatOpacity", "options.chat.title", options.chatOpacity());
        this.register("textBackgroundOpacity", "options.chat.title", options.textBackgroundOpacity());
        this.register("chatScale", "options.chat.title", options.chatScale());
        this.register("chatLineSpacing", "options.chat.title", options.chatLineSpacing());
        this.register("chatDelay", "options.chat.title", options.chatDelay());
        this.register("chatWidth", "options.chat.title", options.chatWidth());
        this.register("chatHeightFocused", "options.chat.title", options.chatHeightFocused());
        this.register("chatHeightUnfocused", "options.chat.title", options.chatHeightUnfocused());
        this.register("narrator", "options.chat.title", options.narrator());
        this.register("autoSuggestions", "options.chat.title", options.autoSuggestions());
        this.register("hideMatchedNames", "options.chat.title", options.hideMatchedNames());
        this.register("reducedDebugInfo", "options.chat.title", options.reducedDebugInfo());
        this.register("onlyShowSecureChat", "options.chat.title", options.onlyShowSecureChat());
        this.register("saveChatDrafts", "options.chat.title", options.saveChatDrafts());

        // Accessibility Settings
        this.register("highContrast", "options.accessibility.title", options.highContrast());
        this.register("backgroundForChatOnly", "options.accessibility.title", options.backgroundForChatOnly());
        this.register("notificationDisplayTime", "options.accessibility.title", options.notificationDisplayTime());
        this.register("darknessEffectScale", "options.accessibility.title", options.darknessEffectScale());
        this.register("damageTiltStrength", "options.accessibility.title", options.damageTiltStrength());
        this.register("hideLightningFlashes", "options.accessibility.title", options.hideLightningFlash());
        this.register("darkMojangStudiosBackground", "options.accessibility.title", options.darkMojangStudiosBackground());
        this.register("panoramaScrollSpeed", "options.accessibility.title", options.panoramaSpeed());
        this.register("hideSplashTexts", "options.accessibility.title", options.hideSplashTexts());
        this.register("narratorHotkey", "options.accessibility.title", options.narratorHotkey());
        this.register("rotateWithMinecart", "options.accessibility.title", options.rotateWithMinecart());
        this.register("highContrastBlockOutline", "options.accessibility.title", options.highContrastBlockOutline());

        // Telemetry Data
        this.register("telemetryOptInExtra", "options.telemetry", options.telemetryOptInExtra());

        // F3 Settings
        this.register("pauseOnLostFocus", "f3", () -> options.pauseOnLostFocus, value -> options.pauseOnLostFocus = value);
        this.register("advancedItemTooltips", "f3", () -> options.advancedItemTooltips, value -> options.advancedItemTooltips = value);

        List<java.util.Map.Entry<Identifier, DebugScreenEntry>> all = new ArrayList<>(DebugScreenEntries.allEntries().entrySet());
        all.sort(OptionListAccessor.getComparator());
        for (Map.Entry<Identifier, DebugScreenEntry> entry : all) {
            this.register(new DebugStandardSetting("debug_" + entry.getKey(), ((TranslatableContents) entry.getValue().category().label().getContents()).getKey(), entry.getKey()));
        }

        this.register(CustomStandardSetting.ofString(
                "pieDirectory", "f3",
                () -> ProfileResults.demanglePath(((PieChartAccessor) Minecraft.getInstance().getDebugOverlay().getProfilerPieChart()).standardsettings$getCurrentPath()),
                value -> ((PieChartAccessor) Minecraft.getInstance().getDebugOverlay().getProfilerPieChart()).standardsettings$setCurrentPath(value),
                value -> value.startsWith("root") ? value.replace('.', '\u001e').trim() : "root",
                Component::literal,
                setting -> {
                    EditBox widget = new EditBox(Minecraft.getInstance().font, 0, 0, 120, 20, setting.getName());
                    widget.setMaxLength(128);
                    widget.setValue(setting.get().replace('\u001e', '.'));
                    widget.setResponder(string -> {
                        // allow for backspace / ctrl + x
                        if (!string.startsWith("root")) {
                            widget.setValue("root");
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
        this.register(CustomStandardSetting.ofEnum("perspective", "more", options::getCameraType, options::setCameraType, CameraType.class)).disable();
        this.register("f1", "more", () -> options.hideGui, value -> options.hideGui = value).disable();
        this.register("sneaking", "more", options.keyShift::isDown, value -> {
            if (options.toggleCrouch().get() && options.keyShift.isDown() != value) {
                // pressing the sneak key toggles it
                options.keyShift.setDown(true);
            }
        }).disable();
        this.register("sprinting", "more", options.keySprint::isDown, value -> {
            if (options.toggleSprint().get() && options.keySprint.isDown() != value) {
                // pressing the sprint key toggles it
                options.keySprint.setDown(true);
            }
        }).disable();

        // OnWorldJoin Settings
        this.onWorldJoin(new SimpleOptionStandardSetting<>("fovOnWorldJoin", "onWorldJoin", options.fov())).disable();
        this.onWorldJoin(new SimpleOptionStandardSetting<>("renderDistanceOnWorldJoin", "onWorldJoin", options.renderDistance())).disable();
        this.onWorldJoin(new SimpleOptionStandardSetting<>("entityDistanceScalingOnWorldJoin", "onWorldJoin", options.entityDistanceScaling())).disable();
        this.onWorldJoin(new SimpleOptionStandardSetting<>("guiScaleOnWorldJoin", "onWorldJoin", options.guiScale()) {
            @Override
            public void set(OptionInstance<Integer> option, Integer value) {
                // bypass vanillas dynamic bounds enforcing for gui scale
                //noinspection unchecked
                ((SimpleOptionAccessor<Integer>) (Object) option).standardsettings$setValue(Math.max(0, value));
            }
        });
    }

    private <T> SimpleOptionStandardSetting<T> register(String id, String category, OptionInstance<T> option) {
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

    private void confirmToggleAll(Button button) {
        Minecraft client = Minecraft.getInstance();
        Screen screen = client.screen;
        client.setScreen(new ConfirmScreen(
                confirmed -> {
                    if (confirmed) {
                        this.toggleAll = !this.toggleAll;
                        this.toggleAll(this.toggleAll);
                    }
                    client.setScreen(screen);
                },
                Component.translatable("speedrunapi.config.standardsettings.option.toggleAll"),
                Component.translatable("speedrunapi.config.standardsettings.option.toggleAll.description")
                        .append(" ")
                        .append(Component.translatable("speedrunapi.config.standardsettings.option.toggleAll.confirm")),
                CommonComponents.GUI_PROCEED,
                CommonComponents.GUI_CANCEL
        ));
    }

    @Override
    public @Nullable SpeedrunOption<?> parseField(Field field, SpeedrunConfig config, String... idPrefix) {
        if ("toggleAll".equals(field.getName())) {
            return new SpeedrunConfigAPI.CustomOption.Builder<Boolean>(this, this, field, idPrefix)
                    .createWidget((option, innerConfig, configStorage, optionField) ->
                            Button.builder(CommonComponents.optionStatus(!option.get()), this::confirmToggleAll).bounds(0, 0, 150, 20).build()
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
    public @Nullable Predicate<InputConstants.Key> createInputListener() {
        return key -> {
            if (this.focusedKeyBinding != null) {
                if (key.getType() == InputConstants.Type.KEYSYM && key.getValue() == GLFW.GLFW_KEY_ESCAPE) {
                    key = InputConstants.UNKNOWN;
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

    @Override
    public void handleLoadException(Exception e, SpeedrunOption<?> option, JsonElement jsonElement) throws Exception {
        if (option instanceof StandardSetting) {
            // fail-soft for standardsettings as their encoding format can change between minecraft versions
            StandardSettings.LOGGER.warn("Failed to load the value for standardsetting {}: {}", option.getID(), jsonElement);
            return;
        }
        SpeedrunConfig.super.handleLoadException(e, option, jsonElement);
    }
}
