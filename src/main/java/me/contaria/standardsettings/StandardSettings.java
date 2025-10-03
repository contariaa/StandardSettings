package me.contaria.standardsettings;

import me.contaria.standardsettings.mixin.accessors.SpriteAtlasTextureAccessor;
import me.contaria.standardsettings.options.StandardSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.resource.language.LanguageDefinition;
import net.minecraft.client.resource.language.LanguageManager;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.client.util.Window;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class StandardSettings {
    public static final Logger LOGGER = LogManager.getLogger();
    public static StandardSettingsConfig config;

    @Nullable
    private static StandardSettingsCache settingsCache;

    public static String lastWorld;
    public static boolean onWorldJoinPending;
    public static boolean autoF3EscPending;

    protected static boolean anaglyph3dUpdated;

    public static void reset() {
        config.update();
        for (StandardSetting<?> setting : config.standardSettings) {
            setting.resetOption();
        }
        updateSettings();
        LOGGER.info("Loaded StandardSettings");
    }

    public static void onWorldJoin() {
        onWorldJoinPending = false;
        for (StandardSetting<?> setting : config.standardSettingsOnWorldJoin) {
            setting.resetOption();
        }
        updateSettings();
        LOGGER.info("Loaded StandardSettings on World Join");
    }

    public static void updateSettings() {
        MinecraftClient client = MinecraftClient.getInstance();

        Window window = new Window(client);
        Screen screen = client.currentScreen;
        if (screen != null && (screen.width != window.getWidth() || screen.height != window.getHeight())) {
            screen.init(client, window.getWidth(), window.getHeight());
        }

        LanguageManager languageManager = client.getLanguageManager();
        if (!languageManager.getLanguage().getCode().equals(client.options.language)) {
            LanguageDefinition language = getLanguage(languageManager, client.options.language);
            if (language == null) {
                language = getLanguage(languageManager, "en_US");
            }
            languageManager.setLanguage(language);
            languageManager.reload(client.getResourceManager());
        }

        TextureManager textureManager = client.getTextureManager();
        SpriteAtlasTexture atlasTexture = client.getSpriteAtlasTexture();
        if (((SpriteAtlasTextureAccessor) atlasTexture).standardsettings$getMaxTextureSize() != client.options.mipmapLevels) {
            atlasTexture.setMaxTextureSize(client.options.mipmapLevels);
            textureManager.bindTexture(SpriteAtlasTexture.BLOCK_ATLAS_TEX);
            atlasTexture.setFilter(false, client.options.mipmapLevels > 0);
            textureManager.reload(client.getResourceManager());
            anaglyph3dUpdated = false;
        } else if (anaglyph3dUpdated) {
            textureManager.reload(client.getResourceManager());
            anaglyph3dUpdated = false;
        }

        KeyBinding.updateKeysByCode();

        client.options.save();
    }

    public static LanguageDefinition getLanguage(LanguageManager manager, String code) {
        for (LanguageDefinition language : manager.getAllLanguages()) {
            if (language.getCode().equals(code)) {
                return language;
            }
        }
        return null;
    }

    public static void createCache() {
        if (lastWorld != null && !(settingsCache != null && lastWorld.equals(settingsCache.getId()))) {
            settingsCache = new StandardSettingsCache(lastWorld);
            LOGGER.info("Cached options for '{}'", lastWorld);
        }
    }

    public static void loadCache(String worldName) {
        if (settingsCache != null && worldName.equals(settingsCache.getId())) {
            settingsCache.load();
            settingsCache = null;
            LOGGER.info("Restored cached options for '{}'", worldName);
        }
    }

    public static void resetPendingActions() {
        onWorldJoinPending = false;
        autoF3EscPending = false;
    }

    public static void saveToWorldFile(String worldName) {
        List<String> options = new ArrayList<>();
        for (StandardSetting<?> setting : config.standardSettings) {
            options.add(setting.getID() + ":" + setting.getOption());
        }
        for (StandardSetting<?> setting : config.standardSettingsOnWorldJoin) {
            options.add(setting.getID() + ":" + setting.get());
        }
        try {
            Files.write(Paths.get("saves", worldName, "standardoptions.txt"), options, StandardCharsets.UTF_8);
            LOGGER.info("Saved standardoptions to world file.");
        } catch (IOException e) {
            LOGGER.warn("Failed to save standardoptions to world file.", e);
        }
    }

    public static boolean isEnabled() {
        return config.toggleStandardSettings;
    }
}
