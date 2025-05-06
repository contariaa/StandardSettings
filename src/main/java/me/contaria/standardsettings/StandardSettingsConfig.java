package me.contaria.standardsettings;

import com.google.gson.JsonParseException;
import me.contaria.speedrunapi.config.SpeedrunConfigAPI;
import me.contaria.speedrunapi.config.SpeedrunConfigContainer;
import me.contaria.speedrunapi.config.api.SpeedrunConfig;
import me.contaria.speedrunapi.config.api.SpeedrunOption;
import me.contaria.speedrunapi.config.api.annotations.Config;
import me.contaria.speedrunapi.util.TextUtil;
import me.contaria.standardsettings.mixin.accessors.CyclingButtonWidget$BuilderAccessor;
import me.contaria.standardsettings.mixin.accessors.CyclingOptionAccessor;
import me.contaria.standardsettings.mixin.accessors.OptionAccessor;
import me.contaria.standardsettings.options.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ConfirmScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ScreenTexts;
import net.minecraft.client.gui.screen.option.LanguageOptionsScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.option.*;
import net.minecraft.client.render.entity.PlayerModelPart;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.Monitor;
import net.minecraft.client.util.VideoMode;
import net.minecraft.client.util.Window;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import net.minecraft.util.Arm;
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
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;

@Config(init = Config.InitPoint.POSTLAUNCH)
public class StandardSettingsConfig implements SpeedrunConfig {
    @Config.Ignored
    public final StandardGameOptions options = new StandardGameOptions(MinecraftClient.getInstance(), null);
    @Config.Ignored
    public final StandardGameOptions optionsOnWorldJoin = new StandardGameOptions(MinecraftClient.getInstance(), null);
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

        this.register("fov", "menu.options", Option.FOV);

        // Online Options
        this.registerBoolean("realmsNotifications", "menu.options", Option.REALMS_NOTIFICATIONS);
        this.registerBoolean("allowServerListing", "options.online", Option.ALLOW_SERVER_LISTING);

        // Video Settings
        this.register("fullscreenResolution", "options.video", StandardGameOptions::getFullscreenResolution, StandardGameOptions::setFullscreenResolution, option -> VideoMode.fromString(option.get()).map(mode -> TextUtil.literal(mode.toString())).orElse(TextUtil.translatable("options.fullscreen.current")), option -> {
            // see FullScreenOption and VideoOptionsScreen#init
            Window window = MinecraftClient.getInstance().getWindow();
            Monitor monitor = window.getMonitor();
            return new DoubleOption("options.fullscreen.resolution", -1.0, monitor != null ? monitor.getVideoModeCount() - 1.0 : -1.0, 1.0f, options -> {
                if (monitor == null) {
                    return -1.0;
                }
                return VideoMode.fromString(option.get()).map(monitor::findClosestVideoModeIndex).orElse(-1).doubleValue();
            }, (options, value) -> {
                if (monitor == null) {
                    return;
                }
                if (value == -1.0) {
                    option.set(null);
                } else {
                    option.set(monitor.getVideoMode(value.intValue()).asString());
                }
            }, (options, doubleOption) -> option.getText()).createButton(options, 0, 0, 120);
        });
        this.register("biomeBlendRadius", "options.video", Option.BIOME_BLEND_RADIUS);
        this.register(new EnumOptionStandardSetting<GraphicsMode>("graphicsMode", "options.video", this.options, Option.GRAPHICS, GraphicsMode.class) {
            @Override
            public void set(GameOptions options, GraphicsMode value) {
                // see Option.GRAPHICS's setter
                options.graphicsMode = value;
                if (!(options instanceof StandardGameOptions)) {
                    if (options.graphicsMode == GraphicsMode.FABULOUS && MinecraftClient.getInstance().getVideoWarningManager().hasCancelledAfterWarning()) {
                        StandardSettings.LOGGER.warn("Set Graphics Mode to 'Fancy' because 'Fabulous!' is not supported on this device.");
                        options.graphicsMode = GraphicsMode.FANCY;
                    }
                    MinecraftClient.getInstance().worldRenderer.reload();
                }
            }
        });
        this.register("renderDistance", "options.video", Option.RENDER_DISTANCE);
        this.register("prioritizeChunkUpdates", "options.video", Option.CHUNK_BUILDER_MODE);
        this.register("simulationDistance", "options.video", Option.SIMULATION_DISTANCE);
        this.registerEnum("ao", "options.video", Option.AO, AoMode.class);
        this.register("maxFps", "options.video", Option.FRAMERATE_LIMIT);
        this.registerBoolean("enableVsync", "options.video", Option.VSYNC);
        this.registerBoolean("bobView", "options.video", Option.VIEW_BOBBING);
        this.register(new CyclingOptionStandardSetting<Integer>("guiScale", "options.video", this.options, (CyclingOption<Integer>) Option.GUI_SCALE) {
            @Override
            public void set(GameOptions options, Integer value) {
                options.guiScale = Math.max(0, value);
            }

            @Override
            protected Integer fromInt(int i) {
                return i;
            }

            @Override
            protected int toInt(Integer value) {
                return value;
            }
        });
        this.registerEnum("attackIndicator", "options.video", Option.ATTACK_INDICATOR, AttackIndicator.class);
        this.register("gamma", "options.video", new DoubleOption("options.gamma", 0.0, 5.0, 0.0f, options -> options.gamma, (options, value) -> options.gamma = value, (options, option) -> TextUtil.literal((int) (option.get(options) * 100.0) + "%")));
        this.registerEnum("renderClouds", "options.video", Option.CLOUDS, CloudRenderMode.class);
        this.registerBoolean("fullscreen", "options.video", Option.FULLSCREEN);
        this.registerEnum("particles", "options.video", Option.PARTICLES, ParticlesMode.class);
        this.register("mipmapLevels", "options.video", Option.MIPMAP_LEVELS);
        this.registerBoolean("entityShadows", "options.video", Option.ENTITY_SHADOWS);
        this.register("screenEffectScale", "options.video", Option.DISTORTION_EFFECT_SCALE);
        this.register("entityDistanceScaling", "options.video", Option.ENTITY_DISTANCE_SCALING);
        this.register("fovEffectScale", "options.video", Option.FOV_EFFECT_SCALE);
        this.registerBoolean("showAutosaveIndicator", "options.video", Option.SHOW_AUTOSAVE_INDICATOR);
        this.register(new BooleanOptionStandardSetting("entityCulling", "options.video", this.options, CyclingOption.create("standardsettings.options.entityCulling", StandardGameOptions::getEntityCulling, (options, option, value) -> StandardGameOptions.setEntityCulling(options, value))) {
            @Override
            public boolean hasWidget() {
                return super.hasWidget() && StandardSettings.HAS_SODIUM;
            }
        });

        // Skin Customizations
        for (PlayerModelPart playerModelPart : PlayerModelPart.values()) {
            this.register(new PlayerModelPartStandardSetting("modelPart_" + playerModelPart.getName(), "options.skinCustomisation", this.options, playerModelPart));
        }
        this.registerEnum("mainHand", "options.skinCustomisation", Option.MAIN_HAND, Arm.class);

        // Music & Sounds
        for (SoundCategory soundCategory : SoundCategory.values()) {
            this.register(new SoundCategoryStandardSetting("soundCategory_" + soundCategory.getName(), "options.sounds", this.options, soundCategory));
        }
        this.registerBoolean("showSubtitles", "options.sounds", Option.SUBTITLES);
        this.register(new StringOptionStandardSetting("soundDevice", "options.sounds", this.options,
                options -> (String) ((CyclingOptionAccessor) Option.AUDIO_DEVICE).standardsettings$getGetter().apply(options),
                (options, value) -> ((CyclingOptionAccessor) Option.AUDIO_DEVICE).standardsettings$getSetter().accept(options, Option.AUDIO_DEVICE, value),
                setting -> ((CyclingButtonWidget$BuilderAccessor) ((CyclingOptionAccessor) Option.AUDIO_DEVICE).standardsettings$getButtonBuilderFactory().get()).standardsettings$getValueToText().apply(setting.get()),
                setting -> ((CyclingOptionAccessor) Option.AUDIO_DEVICE).standardsettings$getButtonBuilderFactory().get()
                        .omitKeyText()
                        .initially(setting.get())
                        .build(0, 0, 120, 20, setting.getName(), (button, value) -> setting.set((String) value))
        ) {
            @Override
            public @NotNull Text getName() {
                return ((OptionAccessor) Option.AUDIO_DEVICE).standardsettings$getDisplayPrefix();
            }
        });

        // Language
        this.register("language", "options.language", StandardGameOptions::getLanguage, StandardGameOptions::setLanguage, option -> TextUtil.literal(MinecraftClient.getInstance().getLanguageManager().getLanguage(option.get()).toString()), option -> new ButtonWidget(0, 0, 120, 20, option.getText(), button -> MinecraftClient.getInstance().setScreen(new LanguageOptionsScreen(MinecraftClient.getInstance().currentScreen, options, MinecraftClient.getInstance().getLanguageManager()))));
        this.registerBoolean("forceUnicodeFont", "options.language", Option.FORCE_UNICODE_FONT);

        // Mouse Settings
        this.register("mouseSensitivity", "options.mouse_settings", Option.SENSITIVITY);
        this.registerBoolean("invertYMouse", "options.mouse_settings", Option.INVERT_MOUSE);
        this.register("mouseWheelSensitivity", "options.mouse_settings", Option.MOUSE_WHEEL_SENSITIVITY);
        this.registerBoolean("discrete_mouse_scroll", "options.mouse_settings", Option.DISCRETE_MOUSE_SCROLL);
        this.registerBoolean("touchscreen", "options.mouse_settings", Option.TOUCHSCREEN);
        this.register(new BooleanOptionStandardSetting("rawMouseInput", "options.mouse_settings", this.options, Option.RAW_MOUSE_INPUT) {
            @Override
            public boolean hasWidget() {
                return super.hasWidget() && InputUtil.isRawMouseMotionSupported();
            }
        });

        // Controls
        this.registerBoolean("autoJump", "options.controls", Option.AUTO_JUMP);
        this.registerBoolean("toggleCrouch", "options.controls", Option.SNEAK_TOGGLED);
        this.registerBoolean("toggleSprint", "options.controls", Option.SPRINT_TOGGLED);
        KeyBinding[] keyBindings = ArrayUtils.clone(MinecraftClient.getInstance().options.allKeys);
        Arrays.sort(keyBindings);
        for (KeyBinding keyBinding : keyBindings) {
            this.register(new KeyBindingStandardSetting("key_" + keyBinding.getTranslationKey(), keyBinding.getCategory(), keyBinding));
        }

        // Chat Settings
        this.registerEnum("chatVisibility", "options.chat.title", Option.VISIBILITY, ChatVisibility.class);
        this.registerBoolean("chatColors", "options.chat.title", Option.CHAT_COLOR);
        this.registerBoolean("chatLinks", "options.chat.title", Option.CHAT_LINKS);
        this.registerBoolean("chatLinksPrompt", "options.chat.title", Option.CHAT_LINKS_PROMPT);
        this.register("chatOpacity", "options.chat.title", Option.CHAT_OPACITY);
        this.register("textBackgroundOpacity", "options.chat.title", Option.TEXT_BACKGROUND_OPACITY);
        this.register("chatScale", "options.chat.title", Option.CHAT_SCALE);
        this.register("chatLineSpacing", "options.chat.title", Option.CHAT_LINE_SPACING);
        this.register("chatDelay", "options.chat.title", Option.CHAT_DELAY_INSTANT);
        this.register("chatWidth", "options.chat.title", Option.CHAT_WIDTH);
        this.register("chatHeightFocused", "options.chat.title", Option.CHAT_HEIGHT_FOCUSED);
        this.register("chatHeightUnfocused", "options.chat.title", Option.SATURATION);
        this.register(new EnumOptionStandardSetting<NarratorMode>("narrator", "options.chat.title", this.options, Option.NARRATOR, NarratorMode.class) {
            @Override
            protected void set(GameOptions options, NarratorMode value) {
                if (this.get(options) != value) {
                    super.set(options, value);
                }
            }
        });
        this.registerBoolean("autoSuggestions", "options.chat.title", Option.AUTO_SUGGESTIONS);
        this.registerBoolean("hideMatchedNames", "options.chat.title", Option.HIDE_MATCHED_NAMES);
        this.registerBoolean("reducedDebugInfo", "options.chat.title", Option.REDUCED_DEBUG_INFO);

        // Accessibility Settings
        this.registerBoolean("backgroundForChatOnly", "options.accessibility.title", Option.TEXT_BACKGROUND);
        this.registerBoolean("darkMojangStudiosBackground", "options.accessibility.title", Option.MONOCHROME_LOGO);
        this.registerBoolean("hideLightningFlashes", "options.accessibility.title", Option.HIDE_LIGHTNING_FLASHES);

        // F3 Settings
        this.registerBoolean("pauseOnLostFocus", "f3", CyclingOption.create("standardsettings.options.pauseOnLostFocus", options -> options.pauseOnLostFocus, (options, option, value) -> options.pauseOnLostFocus = value));
        this.registerBoolean("advancedItemTooltips", "f3", CyclingOption.create("standardsettings.options.advancedItemTooltips", options -> options.advancedItemTooltips, (options, option, value) -> options.advancedItemTooltips = value));
        this.registerBoolean("hitboxes", "f3", CyclingOption.create("standardsettings.options.hitboxes", StandardGameOptions::getHitBoxes, (options, option, value) -> StandardGameOptions.setHitBoxes(options, value))).disable();
        this.registerBoolean("chunkborders", "f3", CyclingOption.create("standardsettings.options.chunkborders", StandardGameOptions::getChunkBorders, (options, option, value) -> StandardGameOptions.setChunkBorders(options, value))).disable();
        this.register("pieDirectory", "f3", StandardGameOptions::getPieDirectory, StandardGameOptions::setPieDirectory, option -> TextUtil.literal(option.get()), option -> {
            TextFieldWidget widget = new TextFieldWidget(MinecraftClient.getInstance().textRenderer, 0, 0, 120, 20, option.getName());
            widget.setMaxLength(128);
            widget.setTextPredicate(string -> string != null && (string.startsWith("root") || string.isEmpty()));
            widget.setText(option.get());
            widget.setChangedListener(string -> {
                if (string.isEmpty()) {
                    widget.setText("root");
                    return;
                }
                option.set(string);
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
        }).disable();

        // More Settings
        this.register("perspective", "more", CyclingOption.create("standardsettings.options.perspective", Perspective.values(), value -> TextUtil.translatable("standardsettings.options.perspective." + value.ordinal()), GameOptions::getPerspective, (options, option, value) -> options.setPerspective(value))).disable();
        this.register("f1", "more", CyclingOption.create("standardsettings.options.f1", options -> options.hudHidden, (options, option, value) -> options.hudHidden = value)).disable();
        this.register("sneaking", "more", CyclingOption.create("standardsettings.options.sneaking", StandardGameOptions::getSneaking, (options, option, value) -> StandardGameOptions.setSneaking(options, value))).disable();
        this.register("sprinting", "more", CyclingOption.create("standardsettings.options.sprinting", StandardGameOptions::getSprinting, (options, option, value) -> StandardGameOptions.setSprinting(options, value))).disable();

        // OnWorldJoin Settings
        this.onWorldJoin(new DoubleOptionStandardSetting("fovOnWorldJoin", "onWorldJoin", this.optionsOnWorldJoin, Option.FOV)).disable();
        this.onWorldJoin(new DoubleOptionStandardSetting("renderDistanceOnWorldJoin", "onWorldJoin", this.optionsOnWorldJoin, Option.RENDER_DISTANCE)).disable();
        this.onWorldJoin(new DoubleOptionStandardSetting("entityDistanceScalingOnWorldJoin", "onWorldJoin", this.optionsOnWorldJoin, Option.ENTITY_DISTANCE_SCALING)).disable();
        this.onWorldJoin(new CyclingOptionStandardSetting<Integer>("guiScaleOnWorldJoin", "options.video", this.options, (CyclingOption<Integer>) Option.GUI_SCALE) {
            @Override
            public void set(GameOptions options, Integer value) {
                options.guiScale = Math.max(0, value);
            }

            @Override
            protected Integer fromInt(int i) {
                return i;
            }

            @Override
            protected int toInt(Integer value) {
                return value;
            }
        });
    }

    private DoubleOptionStandardSetting register(String id, String category, DoubleOption option) {
        return this.register(new DoubleOptionStandardSetting(id, category, this.options, option));
    }

    private BooleanOptionStandardSetting registerBoolean(String id, String category, CyclingOption<Boolean> option) {
        return this.register(new BooleanOptionStandardSetting(id, category, this.options, option));
    }

    private <T extends Enum<T>> EnumOptionStandardSetting<T> registerEnum(String id, String category, CyclingOption<T> option, Class<T> enumClass) {
        return this.register(new EnumOptionStandardSetting<>(id, category, this.options, option, enumClass));
    }

    private <T> CyclingOptionStandardSetting<T> register(String id, String category, CyclingOption<T> option) {
        return this.register(new CyclingOptionStandardSetting<>(id, category, this.options, option));
    }

    private StringOptionStandardSetting register(String id, String category, Function<GameOptions, String> getter, BiConsumer<GameOptions, String> setter, Function<StringOptionStandardSetting, Text> getText, Function<StringOptionStandardSetting, ClickableWidget> createMainWidget) {
        return this.register(new StringOptionStandardSetting(id, category, this.options, getter, setter, getText, createMainWidget));
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
                            new ButtonWidget(0, 0, 150, 20, ScreenTexts.onOrOff(!option.get()), this::confirmToggleAll)
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
