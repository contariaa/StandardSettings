package me.contaria.standardsettings;

import me.contaria.standardsettings.mixin.accessors.BakedModelManagerAccessor;
import me.contaria.standardsettings.mixin.accessors.SpriteAtlasTextureAccessor;
import me.contaria.standardsettings.mixin.accessors.WindowAccessor;
import me.contaria.standardsettings.options.StandardSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.options.KeyBinding;
import net.minecraft.client.resource.language.LanguageDefinition;
import net.minecraft.client.resource.language.LanguageManager;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.util.Monitor;
import net.minecraft.client.util.VideoMode;
import net.minecraft.client.util.Window;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class StandardSettings {
    public static final Logger LOGGER = LogManager.getLogger();
    public static StandardSettingsConfig config;

    @Nullable
    private static StandardSettingsCache settingsCache;

    public static String lastWorld;
    public static boolean onWorldJoinPending;

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
        Window window = client.window;

        window.method_4475();

        if (window.getScaleFactor() != client.options.guiScale) {
            client.onResolutionChanged();
        }

        LanguageManager languageManager = client.getLanguageManager();
        if (!languageManager.getLanguage().getCode().equals(client.options.language)) {
            LanguageDefinition language = languageManager.getLanguage(client.options.language);
            if (language == null) {
                language = languageManager.getLanguage(client.options.language = "en_us");
            }
            languageManager.setLanguage(language);
            languageManager.apply(client.getResourceManager());
        }

        BakedModelManagerAccessor bakedModelManager = (BakedModelManagerAccessor) client.getBakedModelManager();
        if (((SpriteAtlasTextureAccessor) client.getSpriteAtlas()).standardsettings$getMipLevel() != client.options.mipmapLevels) {
            client.getSpriteAtlas().setMipLevel(client.options.mipmapLevels);
            client.getTextureManager().bindTexture(SpriteAtlasTexture.BLOCK_ATLAS_TEX);
            client.getSpriteAtlas().setFilter(false, client.options.mipmapLevels > 0);
            bakedModelManager.standardsettings$apply(bakedModelManager.standardsettings$prepare(client.getResourceManager(), client.getProfiler()), client.getResourceManager(), client.getProfiler());
        }

        KeyBinding.updateKeysByCode();

        client.options.write();
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
            Files.write(MinecraftClient.getInstance().getLevelStorage().getSavesDirectory().resolve(worldName).resolve("standardoptions.txt"), options, StandardCharsets.UTF_8);
            LOGGER.info("Saved standardoptions to world file.");
        } catch (IOException e) {
            LOGGER.warn("Failed to save standardoptions to world file.", e);
        }
    }

    public static boolean isEnabled() {
        return config.toggleStandardSettings;
    }

    public static int findClosestVideoModeIndex(VideoMode videoMode) {
        Monitor monitor = ((WindowAccessor) (Object) MinecraftClient.getInstance().window).standardsettings$getMonitor();
        for (int i = 0; i < monitor.getVideoModeCount(); i++) {
            if (videoMode.equals(monitor.getVideoMode(i))) {
                return i;
            }
        }
        return -1;
    }
}
