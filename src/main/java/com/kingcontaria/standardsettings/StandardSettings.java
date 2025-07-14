package com.kingcontaria.standardsettings;

import com.google.common.io.Files;
import com.kingcontaria.standardsettings.mixins.accessors.BakedModelManagerAccessor;
import com.kingcontaria.standardsettings.mixins.accessors.MinecraftClientAccessor;
import me.jellysquid.mods.sodium.client.SodiumClientMod;
import me.jellysquid.mods.sodium.client.gui.SodiumGameOptions;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.*;
import net.minecraft.client.render.entity.PlayerModelPart;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.VideoMode;
import net.minecraft.client.util.Window;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.Arm;
import net.minecraft.util.math.MathHelper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Environment(value= EnvType.CLIENT)
public class StandardSettings {

    public static final int[] version = new int[]{1,2,4};
    public static final Logger LOGGER = LogManager.getLogger();
    public static final MinecraftClient client = MinecraftClient.getInstance();
    public static final GameOptions options = client.options;
    private static final Window window = client.getWindow();
    public static final File standardoptionsFile = new File(FabricLoader.getInstance().getConfigDir().resolve("standardoptions.txt").toUri());
    public static boolean changeOnWindowActivation = false;
    public static boolean changeOnResize = false;
    public static boolean hasWP = false;
    public static boolean f3PauseSoon = false;
    public static boolean f3PauseOnWorldLoad = false;
    public static int firstWorldF3PauseDelay = 22;
    private static Optional<Integer> renderDistanceOnWorldJoin = Optional.empty();
    private static Optional<Float> entityDistanceScalingOnWorldJoin = Optional.empty();
    private static Optional<Double> fovOnWorldJoin = Optional.empty();
    private static Optional<Integer> guiScaleOnWorldJoin = Optional.empty();
    public static OptionsCache optionsCache = new OptionsCache(client);
    public static String lastWorld;
    public static String[] standardoptionsCache;
    public static Map<File, Long> filesLastModifiedMap;
    private static final Field[] entityCulling = new Field[2];

    public static void load() {
        long start = System.nanoTime();

        emptyOnWorldJoinOptions();

        try {
            if (!standardoptionsFile.exists()) {
                standardoptionsCache = null;
                LOGGER.error("standardoptions.txt is missing");
                return;
            }

            // caches options for last world before applying standardoptions to reload later if necessary
            // allows for verifiability when rejoining a world after accidentally quitting with Atum
            if (lastWorld != null) {
                optionsCache.save(lastWorld);
                lastWorld = null;
            }

            // reload and cache standardoptions if necessary
            if (standardoptionsCache == null || wereFilesModified(filesLastModifiedMap)) {
                LOGGER.info("Reloading & caching StandardSettings...");
                List<String> lines = resolveGlobalFile(standardoptionsFile);
                if (lines == null) {
                    LOGGER.error("standardoptions.txt is empty");
                    return;
                }
                standardoptionsCache = lines.toArray(new String[0]);
            }
            load(standardoptionsCache);
            LOGGER.info("Finished loading StandardSettings ({} ms)", (System.nanoTime() - start) / 1000000.0f);
        } catch (Exception e) {
            standardoptionsCache = null;
            LOGGER.error("Failed to load StandardSettings", e);
        }
    }

    // checks if standardoptions file chain has been modified
    private static boolean wereFilesModified(Map<File, Long> map) {
        if (map == null) {
            return true;
        }
        boolean wereFilesModified = false;
        for (Map.Entry<File, Long> entry : map.entrySet()) {
            wereFilesModified |= !entry.getKey().exists() || entry.getKey().lastModified() != entry.getValue();
        }
        return wereFilesModified;
    }

    // creates a standardoptions file chain by checking if the first line of a file points to another file directory
    public static List<String> resolveGlobalFile(File file) {
        filesLastModifiedMap = new HashMap<>();
        List<String> lines = null;
        do {
            // save the last modified time of each file to be checked later
            filesLastModifiedMap.put(file, file.lastModified());

            try {
                lines = Files.readLines(file, StandardCharsets.UTF_8);
            } catch (IOException e) {
                break;
            }
        } while (lines != null && lines.size() > 0 && (file = new File(lines.get(0))).exists() && !filesLastModifiedMap.containsKey(file));
        return lines;
    }

    // load standardoptions from cache, the heart of the mod if you will
    private static void load(String[] lines) {
        for (String line : lines) {
            try {
                String[] strings = line.split(":", 2);

                // skip line if value is empty
                if (strings.length < 2 || (strings[1] = strings[1].trim()).equals("") && !strings[0].equals("fullscreenResolution")) {
                    continue;
                }
                String[] string0_split = strings[0].split("_", 2);

                switch (string0_split[0]) {
                    case "autoJump" -> options.autoJump = Boolean.parseBoolean(strings[1]);
                    case "autoSuggestions" -> options.autoSuggestions = Boolean.parseBoolean(strings[1]);
                    case "chatColors" -> options.chatColors = Boolean.parseBoolean(strings[1]);
                    case "chatLinks" -> options.chatLinks = Boolean.parseBoolean(strings[1]);
                    case "chatLinksPrompt" -> options.chatLinksPrompt = Boolean.parseBoolean(strings[1]);
                    case "enableVsync" -> window.setVsync(options.enableVsync = Boolean.parseBoolean(strings[1]));
                    case "entityShadows" -> options.entityShadows = Boolean.parseBoolean(strings[1]);
                    case "forceUnicodeFont" -> ((MinecraftClientAccessor) client).callInitFont(options.forceUnicodeFont = Boolean.parseBoolean(strings[1]));
                    case "discrete" -> options.discreteMouseScroll = Boolean.parseBoolean(strings[1]);
                    case "invertYMouse" -> options.invertYMouse = Boolean.parseBoolean(strings[1]);
                    case "reducedDebugInfo" -> options.reducedDebugInfo = Boolean.parseBoolean(strings[1]);
                    case "showSubtitles" -> options.showSubtitles = Boolean.parseBoolean(strings[1]);
                    case "touchscreen" -> options.touchscreen = Boolean.parseBoolean(strings[1]);
                    case "fullscreen" -> {
                        if (window.isFullscreen() != Boolean.parseBoolean(strings[1])) {
                            if (client.isWindowFocused()) {
                                window.toggleFullscreen();
                                options.fullscreen = window.isFullscreen();
                            } else {
                                LOGGER.error("Could not reset fullscreen mode because window wasn't focused!");
                            }
                        }
                    }
                    case "bobView" -> options.bobView = Boolean.parseBoolean(strings[1]);
                    case "toggleCrouch" -> options.sneakToggled = Boolean.parseBoolean(strings[1]);
                    case "toggleSprint" -> options.sprintToggled = Boolean.parseBoolean(strings[1]);
                    case "darkMojangStudiosBackground" -> options.monochromeLogo = Boolean.parseBoolean(strings[1]);
                    case "mouseSensitivity" -> options.mouseSensitivity = Double.parseDouble(strings[1]);
                    case "fov" -> options.fov = Double.parseDouble(strings[1]) < 5 ? Double.parseDouble(strings[1]) * 40.0f + 70.0f : Integer.parseInt(strings[1]);
                    case "screenEffectScale" -> options.distortionEffectScale = Float.parseFloat(strings[1]);
                    case "fovEffectScale" -> options.fovEffectScale = Float.parseFloat(strings[1]);
                    case "gamma" -> options.gamma = Double.parseDouble(strings[1]);
                    case "renderDistance" -> options.viewDistance = Integer.parseInt(strings[1]);
                    case "entityDistanceScaling" -> options.entityDistanceScaling = Float.parseFloat(strings[1]);
                    case "guiScale" -> options.guiScale = Integer.parseInt(strings[1]);
                    case "particles" -> options.particles = ParticlesMode.byId(Integer.parseInt(strings[1]));
                    case "maxFps" -> window.setFramerateLimit(options.maxFps = Integer.parseInt(strings[1]));
                    case "graphicsMode" -> options.graphicsMode = GraphicsMode.byId(Integer.parseInt(strings[1]));
                    case "ao" -> options.ao = AoMode.byId(Integer.parseInt(strings[1]));
                    case "renderClouds" -> options.cloudRenderMode = strings[1].equals("true") ? CloudRenderMode.FANCY : strings[1].equals("false") ? CloudRenderMode.OFF : CloudRenderMode.FAST;
                    case "attackIndicator" -> options.attackIndicator = AttackIndicator.byId(Integer.parseInt(strings[1]));
                    case "lang" -> {
                        client.getLanguageManager().setLanguage(client.getLanguageManager().getLanguage(strings[1]));
                        client.getLanguageManager().reload(client.getResourceManager());
                        options.language = client.getLanguageManager().getLanguage().getCode();
                    }
                    case "chatVisibility" -> options.chatVisibility = ChatVisibility.byId(Integer.parseInt(strings[1]));
                    case "chatOpacity" -> options.chatOpacity = Double.parseDouble(strings[1]);
                    case "chatLineSpacing" -> options.chatLineSpacing = Double.parseDouble(strings[1]);
                    case "textBackgroundOpacity" -> options.textBackgroundOpacity = Double.parseDouble(strings[1]);
                    case "backgroundForChatOnly" -> options.backgroundForChatOnly = Boolean.parseBoolean(strings[1]);
                    case "fullscreenResolution" -> {
                        if (!strings[1].equals(window.getVideoMode().isPresent() ? window.getVideoMode().get().asString() : "")) {
                            window.setVideoMode(VideoMode.fromString(strings[1]));
                            window.applyVideoMode();
                        }
                    }
                    case "advancedItemTooltips" -> options.advancedItemTooltips = Boolean.parseBoolean(strings[1]);
                    case "pauseOnLostFocus" -> options.pauseOnLostFocus = Boolean.parseBoolean(strings[1]);
                    case "chatHeightFocused" -> options.chatHeightFocused = Double.parseDouble(strings[1]);
                    case "chatDelay" -> options.chatDelay = Double.parseDouble(strings[1]);
                    case "chatHeightUnfocused" -> options.chatHeightUnfocused = Double.parseDouble(strings[1]);
                    case "chatScale" -> options.chatScale = Double.parseDouble(strings[1]);
                    case "chatWidth" -> options.chatWidth = Double.parseDouble(strings[1]);
                    case "mipmapLevels" -> {
                        if (options.mipmapLevels != Integer.parseInt(strings[1])) {
                            client.setMipmapLevels(options.mipmapLevels = Integer.parseInt(strings[1]));
                            ((BakedModelManagerAccessor) client.getBakedModelManager()).callApply(((BakedModelManagerAccessor) client.getBakedModelManager()).callPrepare(client.getResourceManager(), client.getProfiler()), client.getResourceManager(), client.getProfiler());
                        }
                    }
                    case "mainHand" -> options.mainArm = "left".equalsIgnoreCase(strings[1]) ? Arm.LEFT : Arm.RIGHT;
                    case "narrator" -> options.narrator = NarratorMode.byId(Integer.parseInt(strings[1]));
                    case "biomeBlendRadius" -> options.biomeBlendRadius = Integer.parseInt(strings[1]);
                    case "mouseWheelSensitivity" -> options.mouseWheelSensitivity = Double.parseDouble(strings[1]);
                    case "rawMouseInput" -> window.setRawMouseMotion(options.rawMouseInput = Boolean.parseBoolean(strings[1]));
                    case "key" -> {
                        for (KeyBinding keyBinding : options.keysAll) {
                            if (string0_split[1].equals(keyBinding.getTranslationKey())) {
                                keyBinding.setBoundKey(InputUtil.fromTranslationKey(strings[1])); break;
                            }
                        }
                    }
                    case "soundCategory" -> {
                        for (SoundCategory soundCategory : SoundCategory.values()) {
                            if (string0_split[1].equals(soundCategory.getName())) {
                                options.setSoundVolume(soundCategory, Float.parseFloat(strings[1])); break;
                            }
                        }
                    }
                    case "modelPart" -> {
                        for (PlayerModelPart playerModelPart : PlayerModelPart.values()) {
                            if (string0_split[1].equals(playerModelPart.getName())) {
                                options.togglePlayerModelPart(playerModelPart, Boolean.parseBoolean(strings[1])); break;
                            }
                        }
                    }
                    case "entityCulling" -> setEntityCulling(Boolean.parseBoolean(strings[1]));
                    case "sneaking" -> options.keySneak.setPressed(options.sneakToggled && (Boolean.parseBoolean(strings[1]) != options.keySneak.isPressed()));
                    case "sprinting" -> options.keySprint.setPressed(options.sprintToggled && (Boolean.parseBoolean(strings[1]) != options.keySprint.isPressed()));
                    case "chunkborders" -> {
                        if (client.debugRenderer.toggleShowChunkBorder() != Boolean.parseBoolean(strings[1])) {
                            client.debugRenderer.toggleShowChunkBorder();
                        }
                    }
                    case "hitboxes" -> client.getEntityRenderDispatcher().setRenderHitboxes(Boolean.parseBoolean(strings[1]));
                    case "perspective" -> options.setPerspective(Perspective.values()[Integer.parseInt(strings[1]) % 3]);
                    case "piedirectory" -> {
                        if (!strings[1].split("\\.")[0].equals("root")) break;
                        ((MinecraftClientAccessor) client).setOpenProfilerSection(strings[1].replace('.', '\u001e'));
                    }
                    case "f1" -> options.hudHidden = Boolean.parseBoolean(strings[1]);
                    case "fovOnWorldJoin" -> fovOnWorldJoin = Optional.of(Double.parseDouble(strings[1]) < 5 ? Double.parseDouble(strings[1]) * 40.0f + 70.0f : Integer.parseInt(strings[1]));
                    case "guiScaleOnWorldJoin" -> guiScaleOnWorldJoin = Optional.of(Integer.parseInt(strings[1]));
                    case "renderDistanceOnWorldJoin" -> renderDistanceOnWorldJoin = Optional.of(Integer.parseInt(strings[1]));
                    case "entityDistanceScalingOnWorldJoin" -> entityDistanceScalingOnWorldJoin = Optional.of(Float.parseFloat(strings[1]));
                    case "changeOnResize" -> changeOnResize = Boolean.parseBoolean(strings[1]);
                    case "f3PauseOnWorldLoad" -> f3PauseOnWorldLoad = Boolean.parseBoolean(strings[1]);
                    case "firstWorldF3PauseDelay" -> firstWorldF3PauseDelay = MathHelper.clamp(Integer.parseInt(strings[1]), 1, 60);
                }
                // Some options.txt settings which aren't accessible in vanilla Minecraft and some unnecessary settings (like Multiplayer stuff) are not included.
                // also has a few extra settings that can be reset that Minecraft doesn't save to options.txt, but are important in speedrunning
            } catch (Exception e) {
                LOGGER.warn("Skipping bad StandardSetting: " + line);
            }
        }
        KeyBinding.updateKeysByCode();
    }

    // load OnWorldJoin options if present
    public static void changeSettingsOnJoin() {
        long start = System.nanoTime();

        renderDistanceOnWorldJoin.ifPresent(viewDistance -> {
            options.viewDistance = viewDistance;
            client.worldRenderer.scheduleTerrainUpdate();
        });
        entityDistanceScalingOnWorldJoin.ifPresent(entityDistanceScaling -> options.entityDistanceScaling = entityDistanceScaling);
        fovOnWorldJoin.ifPresent(fov -> options.fov = fov);
        guiScaleOnWorldJoin.ifPresent(guiScale -> {
            options.guiScale = guiScale;
            client.onResolutionChanged();
        });

        if (fovOnWorldJoin.isPresent() || guiScaleOnWorldJoin.isPresent() || renderDistanceOnWorldJoin.isPresent()) {
            emptyOnWorldJoinOptions();
            options.write();
            LOGGER.info("Changed Settings on World Join ({} ms)", (System.nanoTime() - start) / 1000000.0f);
        }
    }

    // resets OnWorldJoin options to their default (empty) state
    private static void emptyOnWorldJoinOptions() {
        fovOnWorldJoin = Optional.empty();
        entityDistanceScalingOnWorldJoin = Optional.empty();
        guiScaleOnWorldJoin = Optional.empty();
        renderDistanceOnWorldJoin = Optional.empty();
        changeOnResize = false;
        changeOnWindowActivation = false;
        f3PauseOnWorldLoad = false;
    }

    // makes sure the values are within the boundaries of vanilla minecraft / the speedrun.com rule set
    public static void checkSettings() {
        long start = System.nanoTime();

        options.mouseSensitivity = check("Sensitivity", options.mouseSensitivity * 2, 0, 2, true) / 2;
        options.fov = (int) (check("FOV", options.fov, 30, 110, false));
        options.distortionEffectScale = check("Distortion Effects", options.distortionEffectScale, 0, 1, true);
        options.fovEffectScale = check("FOV Effects", options.fovEffectScale, 0, 1, true);
        options.gamma = check("Brightness", options.gamma, 0, 5, true);
        options.viewDistance = check("Render Distance", options.viewDistance, 2, 32);
        options.entityDistanceScaling = check("Entity Distance", options.entityDistanceScaling, 0.5f, 5, true);
        float entityDistanceScalingTemp = options.entityDistanceScaling;
        if (entityDistanceScalingTemp != (options.entityDistanceScaling = (int) (options.entityDistanceScaling * 4) / 4.0f)) {
            LOGGER.warn("Entity Distance was set to a false interval ({})", entityDistanceScalingTemp);
        }
        options.guiScale = check("GUI Scale", options.guiScale, 0, Integer.MAX_VALUE);
        options.maxFps = check("Max Framerate", options.maxFps, 1, 260);
        options.biomeBlendRadius = check("Biome Blend", options.biomeBlendRadius, 0, 7);
        options.chatOpacity = check("Chat Text Opacity", options.chatOpacity, 0, 1, true);
        options.chatLineSpacing = check("Line Spacing", options.chatLineSpacing, 0, 1, true);
        options.textBackgroundOpacity = check("Text Background Opacity", options.textBackgroundOpacity, 0, 1, true);
        options.chatHeightFocused = check("(Chat) Focused Height", options.chatHeightFocused, 0, 1, false);
        options.chatDelay = check("Chat Delay", options.chatDelay, 0, 6, false);
        options.chatHeightUnfocused = check("(Chat) Unfocused Height", options.chatHeightUnfocused, 0, 1, false);
        options.chatScale = check("Chat Text Size", options.chatScale, 0, 1, true);
        options.chatWidth = check("(Chat) Width", options.chatWidth, 0, 1, false);
        if (options.mipmapLevels != (options.mipmapLevels = check("Mipmap Levels", options.mipmapLevels, 0, 4))) {
            client.setMipmapLevels(options.mipmapLevels);
            ((BakedModelManagerAccessor)client.getBakedModelManager()).callApply(((BakedModelManagerAccessor)client.getBakedModelManager()).callPrepare(client.getResourceManager(), client.getProfiler()), client.getResourceManager(), client.getProfiler());
        }
        options.mouseWheelSensitivity = check("Scroll Sensitivity", options.mouseWheelSensitivity, 0.01, 10, false);
        for (SoundCategory soundCategory : SoundCategory.values()) {
            options.setSoundVolume(soundCategory, check("(Music & Sounds) " + SoundCategoryName.valueOf(soundCategory.name()).assignedName, options.getSoundVolume(soundCategory), 0, 1, true));
        }

        if (renderDistanceOnWorldJoin.isPresent()) {
            renderDistanceOnWorldJoin = Optional.of(check("Render Distance (On World Join)", renderDistanceOnWorldJoin.get(), 2, 32));
        }
        if (entityDistanceScalingOnWorldJoin.isPresent()) {
            entityDistanceScalingOnWorldJoin = Optional.of(check("Entity Distance (On World Join)", entityDistanceScalingOnWorldJoin.get(), 0.5f, 5, true));
            entityDistanceScalingTemp = entityDistanceScalingOnWorldJoin.get();
            entityDistanceScalingOnWorldJoin = Optional.of((int) (entityDistanceScalingOnWorldJoin.get() * 4) / 4.0f);
            if (entityDistanceScalingTemp != entityDistanceScalingOnWorldJoin.get()) {
                LOGGER.warn("Entity Distance (On World Join) was set to a false interval ({})", entityDistanceScalingTemp);
            }
        }
        if (fovOnWorldJoin.isPresent()) {
            fovOnWorldJoin = Optional.of((double) (int) check("FOV (On World Join)", fovOnWorldJoin.get(), 30, 110, false));
        }
        if (guiScaleOnWorldJoin.isPresent()) {
            guiScaleOnWorldJoin = Optional.of(check("GUI Scale (On World Join)", guiScaleOnWorldJoin.get(), 0, Integer.MAX_VALUE));
        }

        window.setScaleFactor(window.calculateScaleFactor(options.guiScale, options.forceUnicodeFont));
        options.write();

        LOGGER.info("Finished checking and saving Settings ({} ms)", (System.nanoTime() - start) / 1000000.0f);
    }

    // check methods return the value of the setting, adjusted to be in the given bounds
    // if a setting is outside the bounds, it also gives a log output to signal the value has been corrected
    private static double check(String settingName, double setting, double min, double max, boolean percent) {
        if (setting < min) {
            LOGGER.warn(settingName + " was too low! ({})", percent ? asPercent(setting) : setting);
            return min;
        }
        if (setting > max) {
            LOGGER.warn(settingName + " was too high! ({})", percent ? asPercent(setting) : setting);
            return max;
        }
        return setting;
    }

    private static float check(String settingName, float setting, float min, float max, boolean percent) {
        if (setting < min) {
            LOGGER.warn(settingName + " was too low! ({})", percent ? asPercent(setting) : setting);
            return min;
        }
        if (setting > max) {
            LOGGER.warn(settingName + " was too high! ({})", percent ? asPercent(setting) : setting);
            return max;
        }
        return setting;
    }

    private static int check(String settingName, int setting, int min, int max) {
        if (setting < min) {
            LOGGER.warn(settingName + " was too low! ({})", setting);
            return min;
        }
        if (setting > max) {
            LOGGER.warn(settingName + " was too high! ({})", setting);
            return max;
        }
        return setting;
    }

    private static String asPercent(double value) {
        return value * 100 == (int) (value * 100) ? (int) (value * 100) + "%" : value * 100 + "%";
    }

    private enum SoundCategoryName {
        MASTER("Master Volume"),
        MUSIC("Music"),
        RECORDS("Jukebox/Note Blocks"),
        WEATHER("Weather"),
        BLOCKS("Blocks"),
        HOSTILE("Hostile Creatures"),
        NEUTRAL("Friendly Creatures"),
        PLAYERS("Players"),
        AMBIENT("Ambient/Environment"),
        VOICE("Voice/Speech");

        private final String assignedName;
        SoundCategoryName(String name) {
            this.assignedName = name;
        }
    }

    // returns the contents for a new standardoptions.txt file
    public static String getStandardoptionsTxt() {
        String l = System.lineSeparator();
        StringBuilder string = new StringBuilder("autoJump:" + options.autoJump + l +
                "autoSuggestions:" + options.autoSuggestions + l +
                "chatColors:" + options.chatColors + l +
                "chatLinks:" + options.chatLinks + l +
                "chatLinksPrompt:" + options.chatLinksPrompt + l +
                "enableVsync:" + options.enableVsync + l +
                "entityShadows:" + options.entityShadows + l +
                "forceUnicodeFont:" + options.forceUnicodeFont + l +
                "discrete_mouse_scroll:" + options.discreteMouseScroll + l +
                "invertYMouse:" + options.invertYMouse + l +
                "reducedDebugInfo:" + options.reducedDebugInfo + l +
                "showSubtitles:" + options.showSubtitles + l +
                "touchscreen:" + options.touchscreen + l +
                "fullscreen:" + options.fullscreen + l +
                "bobView:" + options.bobView + l +
                "toggleCrouch:" + options.sneakToggled + l +
                "toggleSprint:" + options.sprintToggled + l +
                "darkMojangStudiosBackground:" + options.monochromeLogo + l +
                "mouseSensitivity:" + options.mouseSensitivity + l +
                "fov:" + (options.fov - 70.0f) / 40.0f + l +
                "screenEffectScale:" + options.distortionEffectScale + l +
                "fovEffectScale:" + options.fovEffectScale + l +
                "gamma:" + options.gamma + l +
                "renderDistance:" + options.viewDistance + l +
                "entityDistanceScaling:" + options.entityDistanceScaling + l +
                "guiScale:" + options.guiScale + l +
                "particles:" + options.particles.getId() + l +
                "maxFps:" + options.maxFps + l +
                "graphicsMode:" + options.graphicsMode.getId() + l +
                "ao:" + options.ao.getId() + l +
                "renderClouds:" + (options.cloudRenderMode == CloudRenderMode.FAST ? "fast" : options.cloudRenderMode == CloudRenderMode.FANCY) + l +
                "attackIndicator:" + options.attackIndicator.getId() + l +
                "lang:" + options.language + l +
                "chatVisibility:" + options.chatVisibility.getId() + l +
                "chatOpacity:" + options.chatOpacity + l +
                "chatLineSpacing:" + options.chatLineSpacing + l +
                "textBackgroundOpacity:" + options.textBackgroundOpacity + l +
                "backgroundForChatOnly:" + options.backgroundForChatOnly + l +
                "fullscreenResolution:" + (options.fullscreenResolution == null ? "" : options.fullscreenResolution) + l +
                "advancedItemTooltips:" + options.advancedItemTooltips + l +
                "pauseOnLostFocus:" + options.pauseOnLostFocus + l +
                "chatHeightFocused:" + options.chatHeightFocused + l +
                "chatDelay:" + options.chatDelay + l +
                "chatHeightUnfocused:" + options.chatHeightUnfocused + l +
                "chatScale:" + options.chatScale + l +
                "chatWidth:" + options.chatWidth + l +
                "mipmapLevels:" + options.mipmapLevels + l +
                "mainHand:" + (options.mainArm == Arm.LEFT ? "left" : "right") + l +
                "narrator:" + options.narrator.getId() + l +
                "biomeBlendRadius:" + options.biomeBlendRadius + l +
                "mouseWheelSensitivity:" + options.mouseWheelSensitivity + l +
                "rawMouseInput:" + options.rawMouseInput + l);
        for (KeyBinding keyBinding : options.keysAll) {
            string.append("key_").append(keyBinding.getTranslationKey()).append(":").append(keyBinding.getBoundKeyTranslationKey()).append(l);
        }
        for (SoundCategory soundCategory : SoundCategory.values()) {
            string.append("soundCategory_").append(soundCategory.getName()).append(":").append(options.getSoundVolume(soundCategory)).append(l);
        }
        for (PlayerModelPart playerModelPart : PlayerModelPart.values()) {
            string.append("modelPart_").append(playerModelPart.getName()).append(":").append(options.isPlayerModelPartEnabled(playerModelPart)).append(l);
        }
        string.append("entityCulling:").append(getEntityCulling().isPresent() ? getEntityCulling().get() : "").append(l).append("sneaking:").append(l).append("sprinting:").append(l).append("chunkborders:").append(l).append("hitboxes:").append(l).append("perspective:").append(l).append("piedirectory:").append(l).append("f1:").append(l).append("fovOnWorldJoin:").append(l).append("guiScaleOnWorldJoin:").append(l).append("renderDistanceOnWorldJoin:").append(l).append("simulationDistanceOnWorldJoin:").append(l).append("entityDistanceScalingOnWorldJoin:").append(l).append("changeOnResize:false").append(l).append("f3PauseOnWorldLoad:false").append(l).append("firstWorldF3PauseDelay:22");

        return string.toString();
    }

    public static void initializeEntityCulling() {
        if (!FabricLoader.getInstance().getModContainer("sodium").isPresent()) return;
        Class entityCullingClass;
        label:
        {
            for (Class clas : SodiumGameOptions.class.getClasses()) {
                for (Field field : clas.getFields()) {
                    if (field.toString().toLowerCase().contains("entityculling")) {
                        entityCulling[0] = field;
                        entityCullingClass = clas;
                        break label;
                    }
                }
            }
            return;
        }
        for (Field field : SodiumGameOptions.class.getFields()) {
            if (field.getType().equals(entityCullingClass)) {
                entityCulling[1] = field; return;
            }
        }
    }

    public static Optional<Boolean> getEntityCulling() {
        if (entityCulling[0] == null || entityCulling[1] == null) return Optional.empty();
        try {
            return Optional.of((boolean) entityCulling[0].get(entityCulling[1].get(SodiumClientMod.options())));
        } catch (IllegalAccessException e) {
            LOGGER.error("Failed to get EntityCulling", e);
        }
        return Optional.empty();
    }

    public static void setEntityCulling(boolean value) {
        if (entityCulling[0] == null || entityCulling[1] == null) return;
        Optional<Boolean> entityCullingTemp = getEntityCulling();
        try {
            entityCulling[0].set(entityCulling[1].get(SodiumClientMod.options()), value);
        } catch (IllegalAccessException e) {
            LOGGER.error("Failed to set EntityCulling to " + value, e);
        }
        entityCullingTemp.ifPresent(entityCullingBefore -> {
            if (entityCullingBefore != getEntityCulling().get()) {
                try {
                    SodiumClientMod.options().writeChanges();
                } catch (IOException e) {
                    LOGGER.error("Failed to save sodium options");
                }
            }
        });
    }

    public static List<String> checkVersion(int[] fileVersion, List<String> existingLines) {
        if (compareVersions(fileVersion, version)) {
            LOGGER.warn("standardoptions.txt was marked with an outdated StandardSettings version ({}), updating now...", String.join(".", Arrays.stream(fileVersion).mapToObj(String::valueOf).toArray(String[]::new)));
        } else {
            return null;
        }

        // remove the values from the lines
        if (existingLines != null) {
            existingLines.replaceAll(line -> line.split(":", 2)[0]);
        }

        List<String> lines = new ArrayList<>();

        checking:
        {
            if (compareVersions(fileVersion, new int[]{1, 2, 3, -996})) {
                if (existingLines != null && (existingLines.contains("firstWorldF3PauseDelay"))) {
                    break checking;
                }
                lines.add("firstWorldF3PauseDelay:22");
            }
            if (compareVersions(fileVersion, new int[]{1, 2, 3, -1000})) {
                if (existingLines != null && (existingLines.contains("f3PauseOnWorldLoad"))) {
                    break checking;
                }
                lines.add("f3PauseOnWorldLoad:false");
            }
            // add lines added in the pre-releases of StandardSettings v1.2.1
            if (compareVersions(fileVersion, new int[]{1, 2, 1, -1000})) {
                if (existingLines != null && (existingLines.contains("entityCulling") || existingLines.contains("f1") || existingLines.contains("guiScaleOnWorldJoin") || existingLines.contains("changeOnResize"))) {
                    break checking;
                }
                lines.add("entityCulling:" + (getEntityCulling().isPresent() ? getEntityCulling().get() : ""));
                lines.add("f1:");
                lines.add("guiScaleOnWorldJoin:");
                lines.add("changeOnResize:false");
            }
        }

        if (lines.size() == 0) {
            LOGGER.info("Didn't find anything to update, good luck on the runs!");
            return null;
        }
        return lines;
    }

    // returns true when versionToCheck is older than versionToCompareTo
    public static boolean compareVersions(int[] versionToCheck, int[] versionToCompareTo) {
        for (int i = 0; i < Math.max(versionToCheck.length, versionToCompareTo.length); i++) {
            int v1 = versionToCheck.length <= i ? 0 : versionToCheck[i];
            int v2 = versionToCompareTo.length <= i ? 0 : versionToCompareTo[i];
            if (v1 == v2) continue;
            return v1 < v2;
        }
        return false;
    }

    public static String getVersion() {
        return String.join(".", Arrays.stream(version).mapToObj(String::valueOf).toArray(String[]::new));
    }
}