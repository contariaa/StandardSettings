package me.contaria.standardsettings;

import com.google.gson.JsonParseException;
import com.mojang.blaze3d.platform.GlStateManager;
import me.contaria.standardsettings.options.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ConfirmScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ScreenTexts;
import net.minecraft.client.gui.screen.options.LanguageOptionsScreen;
import net.minecraft.client.gui.widget.AbstractButtonWidget;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.options.*;
import net.minecraft.client.render.entity.PlayerModelPart;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.Monitor;
import net.minecraft.client.util.VideoMode;
import net.minecraft.client.util.Window;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;
import org.mcsr.speedrunapi.config.SpeedrunConfigAPI;
import org.mcsr.speedrunapi.config.SpeedrunConfigContainer;
import org.mcsr.speedrunapi.config.api.SpeedrunConfig;
import org.mcsr.speedrunapi.config.api.SpeedrunOption;
import org.mcsr.speedrunapi.config.api.annotations.Config;
import org.mcsr.speedrunapi.config.api.annotations.InitializeOn;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;

@InitializeOn(InitializeOn.InitPoint.POSTLAUNCH)
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
        this.register("realmsNotifications", "menu.options", Option.REALMS_NOTIFICATIONS);

        // Video Settings
        this.register("fullscreenResolution", "options.video", StandardGameOptions::getFullscreenResolution, StandardGameOptions::setFullscreenResolution, option -> VideoMode.fromString(option.get()).map(mode -> (Text) new LiteralText(mode.toString())).orElse(new TranslatableText("options.fullscreen.current")), option -> {
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
        this.register(new CyclingOptionStandardSetting("graphicsMode", "options.video", this.options, Option.GRAPHICS, options -> options.graphicsMode.getId()) {
            @Override
            public void set(GameOptions options, Integer value) {
                // see Option.GRAPHICS's setter
                options.graphicsMode = GraphicsMode.byId(value);
                if (!(options instanceof StandardGameOptions)) {
                    if (options.graphicsMode == GraphicsMode.FABULOUS && (!GlStateManager.supportsGl30() || MinecraftClient.getInstance().getVideoWarningManager().hasCancelledAfterWarning())) {
                        StandardSettings.LOGGER.warn("Set Graphics Mode to 'Fancy' because 'Fabulous!' is not supported on this device.");
                        options.graphicsMode = GraphicsMode.FANCY;
                    }
                    MinecraftClient.getInstance().worldRenderer.reload();
                }
            }
        });
        this.register("renderDistance", "options.video", Option.RENDER_DISTANCE);
        this.register("ao", "options.video", Option.AO, options -> options.ao.getValue());
        this.register("maxFps", "options.video", Option.FRAMERATE_LIMIT);
        this.register("enableVsync", "options.video", Option.VSYNC);
        this.register("bobView", "options.video", Option.VIEW_BOBBING);
        this.register(new CyclingOptionStandardSetting("guiScale", "options.video", this.options, Option.GUI_SCALE, options -> options.guiScale) {
            @Override
            public void set(GameOptions options, Integer value) {
                options.guiScale = Math.max(0, value);
            }
        });
        this.register("attackIndicator", "options.video", Option.ATTACK_INDICATOR, options -> options.attackIndicator.getId());
        this.register("gamma", "options.video", new DoubleOption("options.gamma", 0.0, 5.0, 0.0f, options -> options.gamma, (options, value) -> options.gamma = value, (options, option) -> new LiteralText((int) (option.get(options) * 100.0) + "%")));
        this.register("renderClouds", "options.video", Option.CLOUDS, options -> options.cloudRenderMode.getValue());
        this.register("fullscreen", "options.video", Option.FULLSCREEN);
        this.register("particles", "options.video", Option.PARTICLES, options -> options.particles.getId());
        this.register("mipmapLevels", "options.video", Option.MIPMAP_LEVELS);
        this.register("entityShadows", "options.video", Option.ENTITY_SHADOWS);
        this.register("entityDistanceScaling", "options.video", Option.ENTITY_DISTANCE_SCALING);
        this.register(new BooleanOptionStandardSetting("entityCulling", "options.video", this.options, new BooleanOption("standardsettings.options.entityCulling", StandardGameOptions::getEntityCulling, StandardGameOptions::setEntityCulling)) {
            @Override
            public boolean hasWidget() {
                return super.hasWidget() && StandardSettings.HAS_SODIUM;
            }
        });

        // Skin Customizations
        for (PlayerModelPart playerModelPart : PlayerModelPart.values()) {
            this.register(new PlayerModelPartStandardSetting("modelPart_" + playerModelPart.getName(), "options.skinCustomisation", this.options, playerModelPart));
        }
        this.register("mainHand", "options.skinCustomisation", Option.MAIN_HAND, options -> options.mainArm.ordinal());

        // Music & Sounds
        for (SoundCategory soundCategory : SoundCategory.values()) {
            this.register(new SoundCategoryStandardSetting("soundCategory_" + soundCategory.getName(), "options.sounds", this.options, soundCategory));
        }
        this.register("showSubtitles", "options.sounds", Option.SUBTITLES);

        // Language
        this.register("language", "options.language", options -> options.language, (options, value) -> options.language = value, option -> new LiteralText(MinecraftClient.getInstance().getLanguageManager().getLanguage(option.get()).toString()), option -> new ButtonWidget(0, 0, 120, 20, option.getText(), button -> MinecraftClient.getInstance().openScreen(new LanguageOptionsScreen(MinecraftClient.getInstance().currentScreen, options, MinecraftClient.getInstance().getLanguageManager()))));
        this.register("forceUnicodeFont", "options.language", Option.FORCE_UNICODE_FONT);

        // Mouse Settings
        this.register("mouseSensitivity", "options.mouse_settings", Option.SENSITIVITY);
        this.register("invertYMouse", "options.mouse_settings", Option.INVERT_MOUSE);
        this.register("mouseWheelSensitivity", "options.mouse_settings", Option.MOUSE_WHEEL_SENSITIVITY);
        this.register("discrete_mouse_scroll", "options.mouse_settings", Option.DISCRETE_MOUSE_SCROLL);
        this.register("touchscreen", "options.mouse_settings", Option.TOUCHSCREEN);
        this.register(new BooleanOptionStandardSetting("rawMouseInput", "options.mouse_settings", this.options, Option.RAW_MOUSE_INPUT) {
            @Override
            public boolean hasWidget() {
                return super.hasWidget() && InputUtil.isRawMouseMotionSupported();
            }
        });

        // Controls
        this.register("autoJump", "options.controls", Option.AUTO_JUMP);
        KeyBinding[] keyBindings = ArrayUtils.clone(MinecraftClient.getInstance().options.keysAll);
        Arrays.sort(keyBindings);
        for (KeyBinding keyBinding : keyBindings) {
            this.register(new KeyBindingStandardSetting("key_" + keyBinding.getTranslationKey(), keyBinding.getCategory(), keyBinding));
        }

        // Chat Settings
        this.register("chatVisibility", "options.chat.title", Option.VISIBILITY, options -> options.chatVisibility.getId());
        this.register("chatColors", "options.chat.title", Option.CHAT_COLOR);
        this.register("chatLinks", "options.chat.title", Option.CHAT_LINKS);
        this.register("chatLinksPrompt", "options.chat.title", Option.CHAT_LINKS_PROMPT);
        this.register("chatOpacity", "options.chat.title", Option.CHAT_OPACITY);
        this.register("textBackgroundOpacity", "options.chat.title", Option.TEXT_BACKGROUND_OPACITY);
        this.register("chatScale", "options.chat.title", Option.CHAT_SCALE);
        this.register("chatLineSpacing", "options.chat.title", Option.CHAT_LINE_SPACING);
        this.register("chatWidth", "options.chat.title", Option.CHAT_WIDTH);
        this.register("chatHeightFocused", "options.chat.title", Option.CHAT_HEIGHT_FOCUSED);
        this.register("chatHeightUnfocused", "options.chat.title", Option.CHAT_HEIGHT_UNFOCUSED);
        this.register("narrator", "options.chat.title", Option.NARRATOR, options -> options.narrator.getId());
        this.register("autoSuggestions", "options.chat.title", Option.AUTO_SUGGESTIONS);
        this.register("reducedDebugInfo", "options.chat.title", Option.REDUCED_DEBUG_INFO);

        // Accessibility Settings
        this.register("backgroundForChatOnly", "options.accessibility.title", Option.TEXT_BACKGROUND, options -> options.backgroundForChatOnly ? 1 : 0);
        this.register("chatDelay", "options.accessibility.title", Option.CHAT_DELAY_INSTANT);
        this.register("toggleCrouch", "options.accessibility.title", Option.SNEAK_TOGGLED, options -> options.sneakToggled ? 1 : 0);
        this.register("toggleSprint", "options.accessibility.title", Option.SPRINT_TOGGLED, options -> options.sprintToggled ? 1 : 0);

        // F3 Settings
        this.register("pauseOnLostFocus", "f3", new BooleanOption("standardsettings.options.pauseOnLostFocus", options -> options.pauseOnLostFocus, (options, value) -> options.pauseOnLostFocus = value));
        this.register("advancedItemTooltips", "f3", new BooleanOption("standardsettings.options.advancedItemTooltips", options -> options.advancedItemTooltips, (options, value) -> options.advancedItemTooltips = value));
        this.register("hitboxes", "f3", new BooleanOption("standardsettings.options.hitboxes", StandardGameOptions::getHitBoxes, StandardGameOptions::setHitBoxes)).disable();
        this.register("chunkborders", "f3", new BooleanOption("standardsettings.options.chunkborders", StandardGameOptions::getChunkBorders, StandardGameOptions::setChunkBorders)).disable();
        this.register("pieDirectory", "f3", StandardGameOptions::getPieDirectory, StandardGameOptions::setPieDirectory, option -> new LiteralText(option.get()), option -> {
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
        this.register("perspective", "more", new CyclingOption("standardsettings.options.perspective", (options, amount) -> options.perspective = (options.perspective + amount) % 3, (options, option) -> new TranslatableText("standardsettings.options.perspective." + options.perspective)), options -> options.perspective).disable();
        this.register("f1", "more", new BooleanOption("standardsettings.options.f1", options -> options.hudHidden, (options, value) -> options.hudHidden = value)).disable();
        this.register("sneaking", "more", new BooleanOption("standardsettings.options.sneaking", StandardGameOptions::getSneaking, StandardGameOptions::setSneaking)).disable();
        this.register("sprinting", "more", new BooleanOption("standardsettings.options.sprinting", StandardGameOptions::getSprinting, StandardGameOptions::setSprinting)).disable();

        // OnWorldJoin Settings
        this.onWorldJoin(new DoubleOptionStandardSetting("fovOnWorldJoin", "onWorldJoin", this.optionsOnWorldJoin, Option.FOV)).disable();
        this.onWorldJoin(new DoubleOptionStandardSetting("renderDistanceOnWorldJoin", "onWorldJoin", this.optionsOnWorldJoin, Option.RENDER_DISTANCE)).disable();
        this.onWorldJoin(new DoubleOptionStandardSetting("entityDistanceScalingOnWorldJoin", "onWorldJoin", this.optionsOnWorldJoin, Option.ENTITY_DISTANCE_SCALING)).disable();
        this.onWorldJoin(new CyclingOptionStandardSetting("guiScaleOnWorldJoin", "onWorldJoin", this.optionsOnWorldJoin, Option.GUI_SCALE, options -> options.guiScale) {
            @Override
            public void set(GameOptions options, Integer value) {
                options.guiScale = Math.max(0, value);
            }
        }).disable();
    }

    private DoubleOptionStandardSetting register(String id, String category, DoubleOption option) {
        return this.register(new DoubleOptionStandardSetting(id, category, this.options, option));
    }

    private BooleanOptionStandardSetting register(String id, String category, BooleanOption option) {
        return this.register(new BooleanOptionStandardSetting(id, category, this.options, option));
    }

    private CyclingOptionStandardSetting register(String id, String category, CyclingOption option, ToIntFunction<GameOptions> optionGetter) {
        return this.register(new CyclingOptionStandardSetting(id, category, this.options, option, optionGetter));
    }

    private StringOptionStandardSetting register(String id, String category, Function<GameOptions, String> getter, BiConsumer<GameOptions, String> setter, Function<StringOptionStandardSetting, Text> getText, Function<StringOptionStandardSetting, AbstractButtonWidget> createMainWidget) {
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
        client.openScreen(new ConfirmScreen(
                confirmed -> {
                    if (confirmed) {
                        this.toggleAll = !this.toggleAll;
                        this.toggleAll(this.toggleAll);
                    }
                    client.openScreen(screen);
                },
                new TranslatableText("speedrunapi.config.standardsettings.option.toggleAll"),
                new TranslatableText("speedrunapi.config.standardsettings.option.toggleAll.description")
                        .append(" ")
                        .append(new TranslatableText("speedrunapi.config.standardsettings.option.toggleAll.confirm")),
                ScreenTexts.PROCEED,
                ScreenTexts.CANCEL
        ));
    }

    @Override
    public @Nullable SpeedrunOption<?> parseField(Field field, SpeedrunConfig config, String... idPrefix) {
        if ("toggleAll".equals(field.getName())) {
            return new SpeedrunConfigAPI.CustomOption.Builder<Boolean>(this, this, field, idPrefix)
                    .createWidget((option, innerConfig, configStorage, optionField) ->
                            new ButtonWidget(0, 0, 150, 20, ScreenTexts.getToggleText(!option.get()), this::confirmToggleAll)
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
