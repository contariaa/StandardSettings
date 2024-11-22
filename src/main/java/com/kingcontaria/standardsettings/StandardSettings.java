package com.kingcontaria.standardsettings;

import com.google.common.base.Suppliers;
import com.google.common.io.Files;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import com.kingcontaria.standardsettings.mixins.accessors.PieChartAccessor;
import com.mojang.serialization.JsonOps;
import net.caffeinemc.mods.sodium.client.SodiumClientMod;
import net.caffeinemc.mods.sodium.client.gui.SodiumGameOptions;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.*;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.VideoMode;
import net.minecraft.client.util.Window;
import net.minecraft.entity.player.PlayerModelPart;
import net.minecraft.network.message.ChatVisibility;
import net.minecraft.particle.ParticlesMode;
import net.minecraft.resource.SimpleResourceReload;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.Arm;
import net.minecraft.util.Unit;
import net.minecraft.util.math.MathHelper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

@Environment(value= EnvType.CLIENT)
public class StandardSettings {

    public static final int[] version = new int[]{1,2,3, 0};
    public static final Logger LOGGER = LogManager.getLogger();
    public static MinecraftClient client;
    public static GameOptions options;
    private static Window window;
    public static final File standardoptionsFile = new File(FabricLoader.getInstance().getConfigDir().resolve("standardoptions.txt").toUri());
    public static boolean changeOnWindowActivation = false;
    public static boolean changeOnResize = false;
    public static boolean f3PauseSoon = false;
    public static boolean f3PauseOnWorldLoad = false;
    public static int firstWorldF3PauseDelay = 22;
    private static Optional<Integer> renderDistanceOnWorldJoin = Optional.empty();
    private static Optional<Integer> simulationDistanceOnWorldJoin = Optional.empty();
    private static Optional<Double> entityDistanceScalingOnWorldJoin = Optional.empty();
    private static Optional<Integer> fovOnWorldJoin = Optional.empty();
    private static Optional<Integer> guiScaleOnWorldJoin = Optional.empty();
    public static OptionsCache optionsCache;
    public static String lastWorld;
    public static String[] standardoptionsCache;
    public static Map<File, Long> filesLastModifiedMap;
    private static final Field[] entityCulling = new Field[2];

    private static final Supplier<Runnable> saveSodiumOptionsSupplier = Suppliers.memoize(() -> {
        // Sodium 0.5.5 and earlier
        final var writeChangesMethod = Arrays.stream(SodiumGameOptions.class.getMethods())
                .filter(method -> method.getName().equals("writeChanges") && method.getParameterCount() == 0)
                .findAny();
        if (writeChangesMethod.isPresent()) {
            return () -> {
                try {
                    writeChangesMethod.get().invoke(SodiumClientMod.options());
                } catch (IllegalAccessException e) {
                    throw new RuntimeException("Failed to access writeChanges when saving Sodium options", e);
                } catch (InvocationTargetException e) {
                    LOGGER.error("Failed to save Sodium options via writeChanges", e.getCause());
                }
            };
        }

        // Sodium 0.5.6+
        final var writeToDiskMethod = Arrays.stream(SodiumGameOptions.class.getMethods())
                .filter(method -> method.getName().equals("writeToDisk"))
                .findAny();
        if (writeToDiskMethod.isPresent()) {
            return () -> {
                try {
                    writeToDiskMethod.get().invoke(null, SodiumClientMod.options());
                } catch (IllegalAccessException e) {
                    throw new RuntimeException("Failed to access writeToDisk when saving Sodium options", e);
                } catch (InvocationTargetException e) {
                    LOGGER.error("Failed to save Sodium options via writeToDisk", e.getCause());
                }
            };
        }

        throw new RuntimeException("Couldn't determine how to write Sodium options to disk (unsupported Sodium version?)");
    });

    /**
     * True only when loading standard settings during a reset.
     * Used to override Minecraft's stateful clamping of some option values.
     */
    private static boolean resetting;

    /**
     * Initializes static fields that require the client and its options to be initialized already.
     */
    public static void initializeClientRefs() {
        client = MinecraftClient.getInstance();
        window = client.getWindow();
        options = client.options;
        optionsCache = new OptionsCache(client);
    }

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
                    case "autoJump" -> options.getAutoJump().setValue(Boolean.parseBoolean(strings[1]));
                    case "autoSuggestions" -> options.getAutoSuggestions().setValue(Boolean.parseBoolean(strings[1]));
                    case "chatColors" -> options.getChatColors().setValue(Boolean.parseBoolean(strings[1]));
                    case "chatLinks" -> options.getChatLinks().setValue(Boolean.parseBoolean(strings[1]));
                    case "chatLinksPrompt" -> options.getChatLinksPrompt().setValue(Boolean.parseBoolean(strings[1]));
                    case "enableVsync" -> options.getEnableVsync().setValue(Boolean.parseBoolean(strings[1]));
                    case "entityShadows" -> options.getEntityShadows().setValue(Boolean.parseBoolean(strings[1]));
                    case "forceUnicodeFont" -> options.getForceUnicodeFont().setValue(Boolean.parseBoolean(strings[1]));
                    case "japaneseGlyphVariants" -> options.getJapaneseGlyphVariants().setValue(Boolean.parseBoolean(strings[1]));
                    case "discrete" -> options.getDiscreteMouseScroll().setValue(Boolean.parseBoolean(strings[1]));
                    case "invertYMouse" -> options.getInvertYMouse().setValue(Boolean.parseBoolean(strings[1]));
                    case "reducedDebugInfo" -> options.getReducedDebugInfo().setValue(Boolean.parseBoolean(strings[1]));
                    case "showSubtitles" -> options.getShowSubtitles().setValue(Boolean.parseBoolean(strings[1]));
                    case "directionalAudio" -> options.getDirectionalAudio().setValue(Boolean.parseBoolean(strings[1]));
                    case "touchscreen" -> options.getTouchscreen().setValue(Boolean.parseBoolean(strings[1]));
                    case "fullscreen" -> {
                        if (window.isFullscreen() != Boolean.parseBoolean(strings[1])) {
                            if (client.isWindowFocused()) {
                                window.toggleFullscreen();
                                options.getFullscreen().setValue(window.isFullscreen());
                            } else {
                                LOGGER.error("Could not reset fullscreen mode because window wasn't focused!");
                            }
                        }
                    }
                    case "bobView" -> options.getBobView().setValue(Boolean.parseBoolean(strings[1]));
                    case "toggleCrouch" -> options.getSneakToggled().setValue(Boolean.parseBoolean(strings[1]));
                    case "toggleSprint" -> options.getSprintToggled().setValue(Boolean.parseBoolean(strings[1]));
                    case "darkMojangStudiosBackground" -> options.getMonochromeLogo().setValue(Boolean.parseBoolean(strings[1]));
                    case "hideLightningFlashes" -> options.getHideLightningFlashes().setValue(Boolean.parseBoolean(strings[1]));
                    case "mouseSensitivity" -> options.getMouseSensitivity().setValue(Double.parseDouble(strings[1]));
                    case "fov" -> options.getFov().setValue((int) (Double.parseDouble(strings[1]) * 40.0f + 70.0f));
                    case "screenEffectScale" -> options.getDistortionEffectScale().setValue(Double.parseDouble(strings[1]));
                    case "fovEffectScale" -> options.getFovEffectScale().setValue(Double.parseDouble(strings[1]));
                    case "darknessEffectScale" -> options.getDarknessEffectScale().setValue(Double.parseDouble(strings[1]));
                    case "gamma" -> options.getGamma().setValue(Double.parseDouble(strings[1]));
                    case "renderDistance" -> options.getViewDistance().setValue(Integer.parseInt(strings[1]));
                    case "simulationDistance" -> options.getSimulationDistance().setValue(Integer.parseInt(strings[1]));
                    case "entityDistanceScaling" -> options.getEntityDistanceScaling().setValue(Double.parseDouble(strings[1]));
                    case "guiScale" -> options.getGuiScale().setValue(Integer.parseInt(strings[1]));
                    case "particles" -> options.getParticles().setValue(ParticlesMode.byId(Integer.parseInt(strings[1])));
                    case "maxFps" -> options.getMaxFps().setValue(Integer.parseInt(strings[1]));
                    case "graphicsMode" -> options.getGraphicsMode().setValue(GraphicsMode.byId(Integer.parseInt(strings[1])));
                    // same conversion logic as net.minecraft.datafixer.fix.OptionsAmbientOcclusionFix.fixValue
                    case "ao" -> options.getAo().setValue(switch (strings[1]) {
                        case "false", "0" -> false;
                        default -> true;
                    });
                    case "renderClouds" -> options.getCloudRenderMode().setValue(strings[1].equals("\"true\"") ? CloudRenderMode.FANCY : strings[1].equals("\"false\"") ? CloudRenderMode.OFF : CloudRenderMode.FAST);
                    case "attackIndicator" -> options.getAttackIndicator().setValue(AttackIndicator.byId(Integer.parseInt(strings[1])));
                    case "lang" -> {
                        final var languages = client.getLanguageManager().getAllLanguages();
                        final var languageCode = strings[1];
                        // reloading languages is slow in 1.20+, so skip it if unnecessary
                        if (!Objects.equals(client.getLanguageManager().getLanguage(), languageCode)) {
                            if (languages.containsKey(languageCode)) {
                                client.getLanguageManager().setLanguage(languageCode);
                                client.getLanguageManager().reload(client.getResourceManager());
                                options.language = languageCode;
                            } else {
                                LOGGER.warn("No language found for language code '{}', ignoring", languageCode);
                            }
                        }
                    }
                    case "chatVisibility" -> options.getChatVisibility().setValue(ChatVisibility.byId(Integer.parseInt(strings[1])));
                    case "chatOpacity" -> options.getChatOpacity().setValue(Double.parseDouble(strings[1]));
                    case "chatLineSpacing" -> options.getChatLineSpacing().setValue(Double.parseDouble(strings[1]));
                    case "textBackgroundOpacity" -> options.getTextBackgroundOpacity().setValue(Double.parseDouble(strings[1]));
                    case "backgroundForChatOnly" -> options.getBackgroundForChatOnly().setValue(Boolean.parseBoolean(strings[1]));
                    case "highContrastBlockOutline" -> options.getHighContrastBlockOutline().setValue(Boolean.parseBoolean(strings[1]));
                    case "fullscreenResolution" -> {
                        if (!strings[1].equals(window.getFullscreenVideoMode().isPresent() ? window.getFullscreenVideoMode().get().asString() : "")) {
                            window.setFullscreenVideoMode(VideoMode.fromString(strings[1]));
                            window.applyFullscreenVideoMode();
                        }
                    }
                    case "advancedItemTooltips" -> options.advancedItemTooltips = Boolean.parseBoolean(strings[1]);
                    case "pauseOnLostFocus" -> options.pauseOnLostFocus = Boolean.parseBoolean(strings[1]);
                    case "chatHeightFocused" -> options.getChatHeightFocused().setValue(Double.parseDouble(strings[1]));
                    case "chatDelay" -> options.getChatDelay().setValue(Double.parseDouble(strings[1]));
                    case "chatHeightUnfocused" -> options.getChatHeightUnfocused().setValue(Double.parseDouble(strings[1]));
                    case "chatScale" -> options.getChatScale().setValue(Double.parseDouble(strings[1]));
                    case "chatWidth" -> options.getChatWidth().setValue(Double.parseDouble(strings[1]));
                    case "mipmapLevels" -> {
                        if (options.getMipmapLevels().getValue() != Integer.parseInt(strings[1])) {
                            options.getMipmapLevels().setValue(Integer.parseInt(strings[1]));
                            client.setMipmapLevels(options.getMipmapLevels().getValue());
                            reloadBakedModelManager();
                        }
                    }
                    case "inactivityFpsLimit" -> {
                        // GameOptions#read
                        JsonReader jsonReader = new JsonReader(new StringReader(strings[1].isEmpty() ? "\"\"" : strings[1]));
                        JsonElement jsonElement = JsonParser.parseReader(jsonReader);
                        options.getInactivityFpsLimit().setValue(InactivityFpsLimit.Codec.parse(JsonOps.INSTANCE, jsonElement).getOrThrow());
                    }
                    case "mainHand" -> options.getMainArm().setValue("\"left\"".equalsIgnoreCase(strings[1]) ? Arm.LEFT : Arm.RIGHT);
                    case "narrator" -> options.getNarrator().setValue(NarratorMode.byId(Integer.parseInt(strings[1])));
                    case "biomeBlendRadius" -> options.getBiomeBlendRadius().setValue(Integer.parseInt(strings[1]));
                    case "mouseWheelSensitivity" -> options.getMouseWheelSensitivity().setValue(Double.parseDouble(strings[1]));
                    case "rawMouseInput" -> options.getRawMouseInput().setValue(Boolean.parseBoolean(strings[1]));
                    case "showAutosaveIndicator" -> options.getShowAutosaveIndicator().setValue(Boolean.parseBoolean(strings[1]));
                    case "onlyShowSecureChat" -> options.getOnlyShowSecureChat().setValue(Boolean.parseBoolean(strings[1]));
                    case "menuBackgroundBlurriness" -> options.getMenuBackgroundBlurriness().setValue(Integer.parseInt(strings[1]));
                    case "key" -> {
                        for (KeyBinding keyBinding : options.allKeys) {
                            if (string0_split[1].equals(keyBinding.getTranslationKey())) {
                                keyBinding.setBoundKey(InputUtil.fromTranslationKey(strings[1])); break;
                            }
                        }
                    }
                    case "soundCategory" -> {
                        for (SoundCategory soundCategory : SoundCategory.values()) {
                            if (string0_split[1].equals(soundCategory.getName())) {
                                options.getSoundVolumeOption(soundCategory).setValue(Double.parseDouble(strings[1]));
                                break;
                            }
                        }
                    }
                    case "modelPart" -> {
                        for (PlayerModelPart playerModelPart : PlayerModelPart.values()) {
                            if (string0_split[1].equals(playerModelPart.getName())) {
                                options.setPlayerModelPart(playerModelPart, Boolean.parseBoolean(strings[1])); break;
                            }
                        }
                    }
                    case "entityCulling" -> setEntityCulling(Boolean.parseBoolean(strings[1]));
                    case "sneaking" -> {
                        if (options.getSneakToggled().getValue() && (Boolean.parseBoolean(strings[1]) != options.sneakKey.isPressed())) {
                            options.sneakKey.setPressed(true);
                        }
                    }
                    case "sprinting" -> {
                        if (options.getSprintToggled().getValue() && (Boolean.parseBoolean(strings[1]) != options.sprintKey.isPressed())) {
                            options.sprintKey.setPressed(true);
                        }
                    }
                    case "chunkborders" -> {
                        if (client.debugRenderer.toggleShowChunkBorder() != Boolean.parseBoolean(strings[1])) {
                            client.debugRenderer.toggleShowChunkBorder();
                        }
                    }
                    case "hitboxes" -> client.getEntityRenderDispatcher().setRenderHitboxes(Boolean.parseBoolean(strings[1]));
                    case "perspective" -> options.setPerspective(Perspective.values()[Integer.parseInt(strings[1]) % 3]);
                    case "piedirectory" -> {
                        if (!strings[1].split("\\.")[0].equals("root")) break;
                        ((PieChartAccessor) client.getDebugHud().getPieChart()).setCurrentPath(strings[1].replace('.','\u001e'));
                    }
                    case "f1" -> options.hudHidden = Boolean.parseBoolean(strings[1]);
                    case "fovOnWorldJoin" -> fovOnWorldJoin = Optional.of(Double.parseDouble(strings[1]) < 5 ? (int) (Double.parseDouble(strings[1]) * 40.0f + 70.0f) : Integer.parseInt(strings[1]));
                    case "guiScaleOnWorldJoin" -> guiScaleOnWorldJoin = Optional.of(Integer.parseInt(strings[1]));
                    case "renderDistanceOnWorldJoin" -> renderDistanceOnWorldJoin = Optional.of(Integer.parseInt(strings[1]));
                    case "simulationDistanceOnWorldJoin" -> simulationDistanceOnWorldJoin = Optional.of(Integer.parseInt(strings[1]));
                    case "entityDistanceScalingOnWorldJoin" -> entityDistanceScalingOnWorldJoin = Optional.of(Double.parseDouble(strings[1]));
                    case "changeOnResize" -> changeOnResize = Boolean.parseBoolean(strings[1]);
                    case "f3PauseOnWorldLoad" -> f3PauseOnWorldLoad = Boolean.parseBoolean(strings[1]);
                    case "firstWorldF3PauseDelay" -> firstWorldF3PauseDelay = MathHelper.clamp(Integer.parseInt(strings[1]), 1, 60);
                }
                // Some options.txt settings which aren't accessible in vanilla Minecraft and some unnecessary settings (like Multiplayer stuff) are not included.
                // also has a few extra settings that can be reset that Minecraft doesn't save to options.txt, but are important in speedrunning
            } catch (Exception exception) {
                LOGGER.warn("Skipping bad StandardSetting: " + line, exception);
            }
        }
        KeyBinding.updateKeysByCode();
    }

    // load OnWorldJoin options if present
    public static void changeSettingsOnJoin() {
        long start = System.nanoTime();

        renderDistanceOnWorldJoin.ifPresent(viewDistance -> options.getViewDistance().setValue(viewDistance));
        simulationDistanceOnWorldJoin.ifPresent(simulationDistance -> options.getSimulationDistance().setValue(simulationDistance));
        entityDistanceScalingOnWorldJoin.ifPresent(entityDistanceScaling -> options.getEntityDistanceScaling().setValue(entityDistanceScaling));
        fovOnWorldJoin.ifPresent(fov -> options.getFov().setValue(fov));
        guiScaleOnWorldJoin.ifPresent(integer -> {
            options.getGuiScale().setValue(integer);
            client.onResolutionChanged();
        });

        if (fovOnWorldJoin.isPresent()
                || guiScaleOnWorldJoin.isPresent()
                || renderDistanceOnWorldJoin.isPresent()
                || simulationDistanceOnWorldJoin.isPresent()
                || entityDistanceScalingOnWorldJoin.isPresent()
        ) {
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
        simulationDistanceOnWorldJoin = Optional.empty();
        changeOnResize = false;
        changeOnWindowActivation = false;
        f3PauseOnWorldLoad = false;
    }

    // makes sure the values are within the boundaries of vanilla minecraft / the speedrun.com rule set
    public static void checkSettings() {
        long start = System.nanoTime();

        options.getMouseSensitivity().setValue(check("Sensitivity", options.getMouseSensitivity().getValue() * 2, 0, 2, true) / 2);
        options.getFov().setValue(check("FOV", options.getFov().getValue(), 30, 110));
        options.getDistortionEffectScale().setValue(check("Distortion Effects", options.getDistortionEffectScale().getValue(), 0, 1, true));
        options.getFovEffectScale().setValue(check("FOV Effects", options.getFovEffectScale().getValue(),0,1, true));
        options.getGamma().setValue(check("Brightness", options.getGamma().getValue(), 0, 5, true));
        options.getViewDistance().setValue(check("Render Distance", options.getViewDistance().getValue(), 2, 32));
        options.getSimulationDistance().setValue(check("Simulation Distance", options.getSimulationDistance().getValue(), 5, 32));
        options.getEntityDistanceScaling().setValue(check("Entity Distance", options.getEntityDistanceScaling().getValue(), 0.5f, 5, true));
        double entityDistanceScalingTemp = options.getEntityDistanceScaling().getValue();
        options.getEntityDistanceScaling().setValue((int) (options.getEntityDistanceScaling().getValue() * 4) / 4.0D);
        if (entityDistanceScalingTemp != options.getEntityDistanceScaling().getValue()) {
            LOGGER.warn("Entity Distance was set to a false interval ({})", entityDistanceScalingTemp);
        }
        options.getGuiScale().setValue(check("GUI Scale", options.getGuiScale().getValue(), 0, Integer.MAX_VALUE));
        options.getMaxFps().setValue(check("Max Framerate", options.getMaxFps().getValue(), 1, 260));
        options.getBiomeBlendRadius().setValue(check("Biome Blend", options.getBiomeBlendRadius().getValue(), 0, 7));
        options.getChatOpacity().setValue(check("Chat Text Opacity", options.getChatOpacity().getValue(), 0, 1, true));
        options.getChatLineSpacing().setValue(check("Line Spacing", options.getChatLineSpacing().getValue(), 0, 1, true));
        options.getTextBackgroundOpacity().setValue(check("Text Background Opacity", options.getTextBackgroundOpacity().getValue(), 0, 1, true));
        options.getChatHeightFocused().setValue(check("(Chat) Focused Height", options.getChatHeightFocused().getValue(), 0, 1, false));
        options.getChatDelay().setValue(check("Chat Delay", options.getChatDelay().getValue(), 0, 6, false));
        options.getChatHeightUnfocused().setValue(check("(Chat) Unfocused Height", options.getChatHeightUnfocused().getValue(), 0, 1, false));
        options.getChatScale().setValue(check("Chat Text Size", options.getChatScale().getValue(), 0, 1, true));
        options.getChatWidth().setValue(check("(Chat) Width", options.getChatWidth().getValue(), 0, 1, false));
        if (options.getMipmapLevels().getValue() < 0 || options.getMipmapLevels().getValue() > 4) {
            options.getMipmapLevels().setValue(check("Mipmap Levels", options.getMipmapLevels().getValue(), 0, 4));
            client.setMipmapLevels(options.getMipmapLevels().getValue());
            reloadBakedModelManager();
        }
        options.getMouseWheelSensitivity().setValue(check("Scroll Sensitivity", options.getMouseWheelSensitivity().getValue(), 0.01, 10, false));
        for (SoundCategory soundCategory : SoundCategory.values()) {
            final var settingName = "(Music & Sounds) " + SoundCategoryName.valueOf(soundCategory.name()).assignedName;
            options.getSoundVolumeOption(soundCategory).setValue((double) check(settingName, options.getSoundVolume(soundCategory), 0, 1, true));
        }

        if (renderDistanceOnWorldJoin.isPresent()) {
            renderDistanceOnWorldJoin = Optional.of(check("Render Distance (On World Join)", renderDistanceOnWorldJoin.get(), 2, 32));
        }
        if (simulationDistanceOnWorldJoin.isPresent()) {
            simulationDistanceOnWorldJoin = Optional.of(check("Simulation Distance (On World Join)", simulationDistanceOnWorldJoin.get(), 5, 32));
        }
        if (entityDistanceScalingOnWorldJoin.isPresent()) {
            entityDistanceScalingOnWorldJoin = Optional.of(check("Entity Distance (On World Join)", entityDistanceScalingOnWorldJoin.get(), 0.5f, 5, true));
            entityDistanceScalingTemp = entityDistanceScalingOnWorldJoin.get();
            entityDistanceScalingOnWorldJoin = Optional.of((int) (entityDistanceScalingOnWorldJoin.get() * 4) / 4.0D);
            if (entityDistanceScalingTemp != entityDistanceScalingOnWorldJoin.get()) {
                LOGGER.warn("Entity Distance (On World Join) was set to a false interval ({})", entityDistanceScalingTemp);
            }
        }
        if (fovOnWorldJoin.isPresent()) {
            fovOnWorldJoin = Optional.of(check("FOV (On World Join)", fovOnWorldJoin.get(), 30, 110));
        }
        if (guiScaleOnWorldJoin.isPresent()) {
            guiScaleOnWorldJoin = Optional.of(check("GUI Scale (On World Join)", guiScaleOnWorldJoin.get(), 0, Integer.MAX_VALUE));
        }

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

    public static void setResetting(boolean resetting) {
        StandardSettings.resetting = resetting;
    }

    public static boolean isResetting() {
        return resetting;
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
        StringBuilder string = new StringBuilder("autoJump:" + options.getAutoJump().getValue() + l +
                "autoSuggestions:" + options.getAutoSuggestions().getValue() + l +
                "chatColors:" + options.getChatColors().getValue() + l +
                "chatLinks:" + options.getChatLinks().getValue() + l +
                "chatLinksPrompt:" + options.getChatLinksPrompt().getValue() + l +
                "enableVsync:" + options.getEnableVsync().getValue() + l +
                "entityShadows:" + options.getEntityShadows().getValue() + l +
                "forceUnicodeFont:" + options.getForceUnicodeFont().getValue() + l +
                "japaneseGlyphVariants:" + options.getJapaneseGlyphVariants().getValue() + l +
                "discrete_mouse_scroll:" + options.getDiscreteMouseScroll().getValue() + l +
                "invertYMouse:" + options.getInvertYMouse().getValue() + l +
                "reducedDebugInfo:" + options.getReducedDebugInfo().getValue() + l +
                "showSubtitles:" + options.getShowSubtitles().getValue() + l +
                "directionalAudio:" + options.getDirectionalAudio().getValue() + l +
                "touchscreen:" + options.getTouchscreen().getValue() + l +
                "fullscreen:" + options.getFullscreen().getValue() + l +
                "bobView:" + options.getBobView().getValue() + l +
                "toggleCrouch:" + options.getSneakToggled().getValue() + l +
                "toggleSprint:" + options.getSprintToggled().getValue() + l +
                "darkMojangStudiosBackground:" + options.getMonochromeLogo().getValue() + l +
                "hideLightningFlashes:" + options.getHideLightningFlashes().getValue() + l +
                "mouseSensitivity:" + options.getMouseSensitivity().getValue() + l +
                "fov:" + (options.getFov().getValue() - 70.0f) / 40.0f + l +
                "screenEffectScale:" + options.getDistortionEffectScale().getValue() + l +
                "fovEffectScale:" + options.getFovEffectScale().getValue() + l +
                "darknessEffectScale:" + options.getDarknessEffectScale().getValue() + l +
                "gamma:" + options.getGamma().getValue() + l +
                "renderDistance:" + options.getViewDistance().getValue() + l +
                "simulationDistance:" + options.getSimulationDistance().getValue() + l +
                "entityDistanceScaling:" + options.getEntityDistanceScaling().getValue() + l +
                "guiScale:" + options.getGuiScale().getValue() + l +
                "particles:" + options.getParticles().getValue().getId() + l +
                "maxFps:" + options.getMaxFps().getValue() + l +
                "graphicsMode:" + options.getGraphicsMode().getValue().getId() + l +
                "ao:" + options.getAo().getValue() + l +
                "renderClouds:\"" + (options.getCloudRenderMode().getValue() == CloudRenderMode.FAST ? "fast" : options.getCloudRenderMode().getValue() == CloudRenderMode.FANCY) + "\"" + l +
                "attackIndicator:" + options.getAttackIndicator().getValue().getId() + l +
                "lang:" + options.language + l +
                "chatVisibility:" + options.getChatVisibility().getValue().getId() + l +
                "chatOpacity:" + options.getChatOpacity().getValue() + l +
                "chatLineSpacing:" + options.getChatLineSpacing().getValue() + l +
                "textBackgroundOpacity:" + options.getTextBackgroundOpacity().getValue() + l +
                "backgroundForChatOnly:" + options.getBackgroundForChatOnly().getValue() + l +
                "highContrastBlockOutline:" + options.getHighContrastBlockOutline().getValue() + l +
                "fullscreenResolution:" + (options.fullscreenResolution == null ? "" : options.fullscreenResolution) + l +
                "advancedItemTooltips:" + options.advancedItemTooltips + l +
                "pauseOnLostFocus:" + options.pauseOnLostFocus + l +
                "chatHeightFocused:" + options.getChatHeightFocused().getValue() + l +
                "chatDelay:" + options.getChatDelay().getValue() + l +
                "chatHeightUnfocused:" + options.getChatHeightUnfocused().getValue() + l +
                "chatScale:" + options.getChatScale().getValue() + l +
                "chatWidth:" + options.getChatWidth().getValue() + l +
                "mipmapLevels:" + options.getMipmapLevels().getValue() + l +
                "inactivityFpsLimit:" + InactivityFpsLimit.Codec.encodeStart(JsonOps.INSTANCE, options.getInactivityFpsLimit().getValue()).getOrThrow() + l +
                "mainHand:" + (options.getMainArm().getValue() == Arm.LEFT ? "\"left\"" : "\"right\"") + l +
                "narrator:" + options.getNarrator().getValue().getId() + l +
                "biomeBlendRadius:" + options.getBiomeBlendRadius().getValue() + l +
                "mouseWheelSensitivity:" + options.getMouseWheelSensitivity().getValue() + l +
                "rawMouseInput:" + options.getRawMouseInput().getValue() + l +
                "showAutosaveIndicator:" + options.getShowAutosaveIndicator().getValue() + l +
               // "chatPreview:" + options.getChatPreview().getValue() + l +
                "onlyShowSecureChat:" + options.getOnlyShowSecureChat().getValue() + l +
                "menuBackgroundBlurriness:" + options.getMenuBackgroundBlurriness().getValue() + l);
        for (KeyBinding keyBinding : options.allKeys) {
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
        Class<?> entityCullingClass;
        label:
        {
            for (Class<?> clas : SodiumGameOptions.class.getClasses()) {
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
                saveSodiumOptionsSupplier.get().run();
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

    static void reloadBakedModelManager() {
        SimpleResourceReload.start(
                client.getResourceManager(),
                List.of(client.getBakedModelManager()),
                Runnable::run,
                Runnable::run,
                CompletableFuture.completedFuture(Unit.INSTANCE),
                false
        ).whenComplete().join();
    }
}