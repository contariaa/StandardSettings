package me.contaria.standardsettings;

import com.mojang.blaze3d.platform.Window;
import me.contaria.standardsettings.options.StandardSetting;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.TextureFilteringMethod;
import net.minecraft.client.resources.language.LanguageManager;
import net.minecraft.server.packs.resources.SimpleReloadInstance;
import net.minecraft.util.Unit;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class StandardSettings {
    public static final Logger LOGGER = LogManager.getLogger();
    public static final boolean HAS_SODIUM = FabricLoader.getInstance().isModLoaded("sodium");
    public static StandardSettingsConfig config;

    @Nullable
    private static StandardSettingsCache settingsCache;

    public static String lastWorld;
    public static boolean onWorldJoinPending;
    public static boolean autoF3EscPending;

    private static int oldMipmaps;
    private static int oldAnisotropyBit;
    private static TextureFilteringMethod oldTextureFiltering;

    public static void reset() {
        config.update();
        cacheMipMap();
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
        Minecraft client = Minecraft.getInstance();
        Window window = client.getWindow();

        window.changeFullscreenVideoMode();

        if (window.getGuiScale() != client.options.guiScale().get()) {
            client.resizeGui();
        }

        LanguageManager languageManager = client.getLanguageManager();
        if (!languageManager.getSelected().equals(client.options.languageCode)) {
            if (languageManager.getLanguage(client.options.languageCode) == null) {
                client.options.languageCode = "en_us";
            }
            languageManager.setSelected(client.options.languageCode);
            languageManager.onResourceManagerReload(client.getResourceManager());
        }

        if (compareMipMap()) {
            client.updateMaxMipLevel(client.options.mipmapLevels().get());
            SimpleReloadInstance.create(
                    client.getResourceManager(),
                    List.of(client.getAtlasManager(), client.getModelManager()),
                    Runnable::run,
                    Runnable::run,
                    CompletableFuture.completedFuture(Unit.INSTANCE),
                    false
            ).done().join();
        }

        KeyMapping.resetMapping();

        client.options.save();
        client.debugEntries.save();
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
            options.add(setting.getID() + ":" + setting.getVanilla());
        }
        for (StandardSetting<?> setting : config.standardSettingsOnWorldJoin) {
            options.add(setting.getID() + ":" + setting.get());
        }
        try {
            Files.write(Minecraft.getInstance().getLevelSource().getBaseDir().resolve(worldName).resolve("standardoptions.txt"), options, StandardCharsets.UTF_8);
            LOGGER.info("Saved standardoptions to world file.");
        } catch (IOException e) {
            LOGGER.warn("Failed to save standardoptions to world file.", e);
        }
    }

    public static boolean isEnabled() {
        return config.toggleStandardSettings;
    }

    private static void cacheMipMap() {
        Options options = Minecraft.getInstance().options;
        oldMipmaps = options.mipmapLevels().get();
        oldAnisotropyBit = options.maxAnisotropyBit().get();
        oldTextureFiltering = options.textureFiltering().get();
    }

    private static boolean compareMipMap() {
        Options options = Minecraft.getInstance().options;
        return options.mipmapLevels().get() != oldMipmaps
                || options.maxAnisotropyBit().get() != oldAnisotropyBit
                || options.textureFiltering().get() != oldTextureFiltering;
    }
}
