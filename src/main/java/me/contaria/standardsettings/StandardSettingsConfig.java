package me.contaria.standardsettings;

import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import me.contaria.speedrunapi.config.SpeedrunConfigAPI;
import me.contaria.speedrunapi.config.SpeedrunConfigContainer;
import me.contaria.speedrunapi.config.api.SpeedrunConfig;
import me.contaria.speedrunapi.config.api.SpeedrunOption;
import me.contaria.speedrunapi.config.api.annotations.Config;
import me.contaria.speedrunapi.config.api.gui.CallbackButtonWidget;
import me.contaria.standardsettings.gui.StandardOptionSliderWidget;
import me.contaria.standardsettings.mixin.accessors.ButtonWidgetAccessor;
import me.contaria.standardsettings.options.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ConfirmScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.options.LanguageOptionsScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.PagedEntryListWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.entity.PlayerModelPart;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.client.resource.language.LanguageDefinition;
import net.minecraft.client.resource.language.LanguageManager;
import net.minecraft.client.sound.SoundCategory;
import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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

    @Config.Category("onWorldJoin")
    public boolean triggerOnResize = false;

    @Config.Ignored
    @Nullable
    private KeyBindingStandardSetting focusedKeyBinding;

    {
        StandardSettings.config = this;

        this.register("fov", "menu.options", GameOptions.Option.FIELD_OF_VIEW);
        this.register("realmsNotifications", "menu.options", GameOptions.Option.REALMS_NOTIFICATIONS);

        // Video Settings
        this.register("fancyGraphics", "options.video", GameOptions.Option.GRAPHICS);
        this.register("renderDistance", "options.video", GameOptions.Option.RENDER_DISTANCE);
        this.register("ao", "options.video", GameOptions.Option.AMBIENT_OCCLUSION, options -> options.ao);
        this.register("maxFps", "options.video", GameOptions.Option.MAX_FPS);
        this.register(new BooleanOptionStandardSetting("anaglyph3d", "options.video", this.options, GameOptions.Option.ANAGLYPH) {
            @Override
            protected void set(GameOptions options, Boolean value) {
                if (value != this.get()) {
                    super.set(options, value);
                    StandardSettings.anaglyph3dUpdated = true;
                }
            }
        });
        this.register("bobView", "options.video", GameOptions.Option.VIEW_BOBBING);
        this.register(new CyclingOptionStandardSetting("guiScale", "options.video", this.options, GameOptions.Option.GUI_SCALE, options -> options.guiScale) {
            @Override
            public void set(GameOptions options, Integer value) {
                options.guiScale = Math.max(0, value);
            }
        });
        this.register(new FloatOptionStandardSetting("gamma", "options.video", this.options, GameOptions.Option.BRIGHTNESS) {
            @Override
            public void set(GameOptions options, Float value) {
                options.gamma = Math.max(0.0f, Math.min(5.0f, value));
            }

            @Override
            public @NotNull ButtonWidget createMainWidget() {
                StandardOptionSliderWidget widget = new StandardOptionSliderWidget(this.option.getOrdinal(), 0, 0, this.option, this, this.options) {
                    @Override
                    protected void mouseDragged(MinecraftClient client, int mouseX, int mouseY) {
                        super.mouseDragged(client, mouseX, mouseY);
                        this.setting.set(this.setting.get() * 5.0f);
                        this.message = this.setting.getText();
                    }

                    @Override
                    public boolean isMouseOver(MinecraftClient client, int mouseX, int mouseY) {
                        boolean isMouseOver = super.isMouseOver(client, mouseX, mouseY);
                        this.setting.set(this.setting.get() * 5.0f);
                        this.message = this.setting.getText();
                        return isMouseOver;
                    }
                };
                ((ButtonWidgetAccessor) widget).standardsettings$setWidth(120);
                return widget;
            }

            @Override
            public @NotNull String getDisplayText() {
                float value = this.get();
                if (value == 0.0f) {
                    return I18n.translate("options.gamma.min");
                }
                if (value == 1.0f) {
                    return I18n.translate("options.gamma.max");
                }
                return "+" + (int) (value * 100.0f) + "%";
            }
        });
        this.register("renderClouds", "options.video", GameOptions.Option.SHOW_CLOUDS, options -> options.cloudMode);
        this.register("particles", "options.video", GameOptions.Option.PARTICLES, options -> options.particle);
        this.register("fullscreen", "options.video", GameOptions.Option.USE_FULLSCREEN);
        this.register("enableVsync", "options.video", GameOptions.Option.ENABLE_VSYNC);
        this.register("mipmapLevels", "options.video", GameOptions.Option.MIPMAP_LEVELS);
        this.register("allowBlockAlternatives", "options.video", GameOptions.Option.BLOCK_ALTERNATIVES);
        this.register("useVbo", "options.video", GameOptions.Option.USE_VBO);
        this.register("entityShadows", "options.video", GameOptions.Option.ENTITY_SHADOWS);

        // Skin Customizations
        for (PlayerModelPart playerModelPart : PlayerModelPart.values()) {
            this.register(new PlayerModelPartStandardSetting("modelPart_" + playerModelPart.getName(), "options.skinCustomisation", this.options, playerModelPart));
        }

        // Music & Sounds
        for (SoundCategory soundCategory : SoundCategory.values()) {
            this.register(new SoundCategoryStandardSetting("soundCategory_" + soundCategory.getName(), "options.sounds", this.options, soundCategory));
        }

        // Language
        this.register("language", "options.language", options -> options.language, (options, value) -> options.language = value, option -> {
            LanguageManager manager = MinecraftClient.getInstance().getLanguageManager();
            LanguageDefinition language = StandardSettings.getLanguage(manager, option.get());
            if (language == null) {
                language = Objects.requireNonNull(StandardSettings.getLanguage(manager, "en_US"));
            }
            return language.toString();
        }, option -> new CallbackButtonWidget(120, 20, option.getText(), button -> MinecraftClient.getInstance().setScreen(new LanguageOptionsScreen(MinecraftClient.getInstance().currentScreen, this.options, MinecraftClient.getInstance().getLanguageManager()))));
        this.register("forceUnicodeFont", "options.language", GameOptions.Option.FORCE_UNICODE);

        // Controls
        this.register("mouseSensitivity", "options.controls", GameOptions.Option.SENSITIVITY);
        this.register("invertYMouse", "options.controls", GameOptions.Option.INVERT_MOUSE);
        this.register("touchscreen", "options.controls", GameOptions.Option.TOUCHSCREEN);
        KeyBinding[] keyBindings = ArrayUtils.clone(MinecraftClient.getInstance().options.allKeys);
        Arrays.sort(keyBindings);
        for (KeyBinding keyBinding : keyBindings) {
            this.register(new KeyBindingStandardSetting("key_" + keyBinding.getTranslationKey(), keyBinding.getCategory(), keyBinding));
        }

        // Chat Settings
        this.register("chatVisibility", "options.chat.title", GameOptions.Option.CHAT_VISIBILITY, options -> options.chatVisibilityType.getId());
        this.register("chatColors", "options.chat.title", GameOptions.Option.CHAT_COLOR);
        this.register("chatLinks", "options.chat.title", GameOptions.Option.CHAT_LINKS);
        this.register("chatOpacity", "options.chat.title", GameOptions.Option.CHAT_OPACITY);
        this.register("chatLinksPrompt", "options.chat.title", GameOptions.Option.CHAT_LINKS_PROMPT);
        this.register("chatScale", "options.chat.title", GameOptions.Option.CHAT_SCALE);
        this.register("chatHeightFocused", "options.chat.title", GameOptions.Option.CHAT_HEIGHT_FOCUSED);
        this.register("chatHeightUnfocused", "options.chat.title", GameOptions.Option.SATURATION);
        this.register("chatWidth", "options.chat.title", GameOptions.Option.CHAT_WIDTH);
        this.register("reducedDebugInfo", "options.chat.title", GameOptions.Option.REDUCED_DEBUG_INFO);

        // Snooper Settings
        this.register("snooperEnabled", "options.snooper.view", GameOptions.Option.SNOOPER_ENABLED);

        // F3 Settings
        this.registerBoolean("pauseOnLostFocus", "f3", options -> options.pauseOnLostFocus, (options, value) -> options.pauseOnLostFocus = value);
        this.registerBoolean("hitboxes", "f3", StandardGameOptions::getHitBoxes, StandardGameOptions::setHitBoxes).disable();
        this.register("pieDirectory", "f3", StandardGameOptions::getPieDirectory, StandardGameOptions::setPieDirectory, StandardSetting::get, option -> {
            TextFieldWidget widget = new TextFieldWidget(-1, MinecraftClient.getInstance().textRenderer, 0, 0, 120, 20);
            widget.setMaxLength(128);
            widget.setTextPredicate(string -> string != null && (string.startsWith("root") || string.isEmpty()));
            widget.setText(option.get());
            widget.setListener(new PagedEntryListWidget.Listener() {
                @Override
                public void setBooleanValue(int id, boolean value) {
                }

                @Override
                public void setFloatValue(int id, float value) {
                }

                @Override
                public void setStringValue(int id, String text) {
                    if (text.isEmpty()) {
                        widget.setText("root");
                        return;
                    }
                    option.set(text);
                }
            });
            return widget;
        }).disable();

        // More Settings
        this.registerCycling("perspective", "more", options -> options.perspective, (options, value) -> options.perspective = value % 3).disable();
        this.registerBoolean("f1", "more", options -> options.hudHidden, (options, value) -> options.hudHidden = value);

        // OnWorldJoin Settings
        this.onWorldJoin(new FloatOptionStandardSetting("fovOnWorldJoin", "onWorldJoin", this.optionsOnWorldJoin, GameOptions.Option.FIELD_OF_VIEW)).disable();
        this.onWorldJoin(new FloatOptionStandardSetting("renderDistanceOnWorldJoin", "onWorldJoin", this.optionsOnWorldJoin, GameOptions.Option.RENDER_DISTANCE)).disable();
        this.onWorldJoin(new CyclingOptionStandardSetting("guiScaleOnWorldJoin", "onWorldJoin", this.optionsOnWorldJoin, GameOptions.Option.GUI_SCALE, options -> options.guiScale) {
            @Override
            public void set(GameOptions options, Integer value) {
                options.guiScale = Math.max(0, value);
            }
        }).disable();
    }

    private StandardSetting<?> register(String id, String category, GameOptions.Option option) {
        if (option.isNumeric()) {
            return this.register(new FloatOptionStandardSetting(id, category, this.options, option));
        }
        return this.register(new BooleanOptionStandardSetting(id, category, this.options, option));
    }

    private CyclingOptionStandardSetting register(String id, String category, GameOptions.Option option, ToIntFunction<GameOptions> optionGetter) {
        return this.register(new CyclingOptionStandardSetting(id, category, this.options, option, optionGetter));
    }

    private BooleanStandardSetting registerBoolean(String id, String category, Function<GameOptions, Boolean> getter, BiConsumer<GameOptions, Boolean> setter) {
        return this.register(new BooleanStandardSetting(id, category, this.options, getter, setter));
    }

    private CyclingStandardSetting registerCycling(String id, String category, Function<GameOptions, Integer> getter, BiConsumer<GameOptions, Integer> setter) {
        return this.register(new CyclingStandardSetting(id, category, this.options, getter, setter));
    }

    private StringOptionStandardSetting register(String id, String category, Function<GameOptions, String> getter, BiConsumer<GameOptions, String> setter, Function<StringOptionStandardSetting, String> getText, Function<StringOptionStandardSetting, Object> createMainWidget) {
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
                (confirmed, id) -> {
                    if (confirmed) {
                        this.toggleAll = !this.toggleAll;
                        this.toggleAll(this.toggleAll);
                    }
                    client.setScreen(screen);
                },
                I18n.translate("speedrunapi.config.standardsettings.option.toggleAll"),
                I18n.translate("speedrunapi.config.standardsettings.option.toggleAll.description")
                        + " "
                        + I18n.translate("speedrunapi.config.standardsettings.option.toggleAll.confirm"),
                I18n.translate("gui.yes"),
                I18n.translate("gui.cancel"),
                -1
        ));
    }

    @Override
    public @Nullable SpeedrunOption<?> parseField(Field field, SpeedrunConfig config, String... idPrefix) {
        if ("toggleAll".equals(field.getName())) {
            return new SpeedrunConfigAPI.CustomOption.Builder<Boolean>(this, this, field, idPrefix)
                    .createWidget((option, innerConfig, configStorage, optionField) ->
                            new CallbackButtonWidget(I18n.translate(option.get() ? "options.off" : "options.on"), this::confirmToggleAll)
                    ).build();
        }
        return SpeedrunConfig.super.parseField(field, config, idPrefix);
    }

    @Override
    public void finishInitialization(SpeedrunConfigContainer<?> container) {
        this.configContainer = container;
        this.fileLastModified = this.getConfigFile().lastModified();
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
    public @Nullable Predicate<Integer> createInputListener() {
        return code -> {
            if (this.focusedKeyBinding != null) {
                if (code == 1) {
                    code = 0;
                }
                this.focusedKeyBinding.set(code);
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
