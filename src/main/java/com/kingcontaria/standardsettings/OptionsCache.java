package com.kingcontaria.standardsettings;

import com.kingcontaria.standardsettings.mixins.accessors.BakedModelManagerAccessor;
import com.kingcontaria.standardsettings.mixins.accessors.MinecraftClientAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.*;
import net.minecraft.client.render.entity.PlayerModelPart;
import net.minecraft.client.resource.language.LanguageDefinition;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.VideoMode;
import net.minecraft.client.util.Window;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.Arm;

import java.util.Optional;

public class OptionsCache {

    private final MinecraftClient client;
    private final GameOptions options;
    private final Window window;
    private String levelName;
    private boolean autoJump;
    private boolean autoSuggestions;
    private boolean chatColors;
    private boolean chatLinks;
    private boolean chatLinksPrompt;
    private boolean enableVsync;
    private boolean entityShadows;
    private boolean forceUnicodeFont;
    private boolean discreteMouseScroll;
    private boolean invertYMouse;
    private boolean reducedDebugInfo;
    private boolean showSubtitles;
    private boolean directionalAudio;
    private boolean touchscreen;
    private boolean fullscreen;
    private boolean bobView;
    private boolean sneakToggled;
    private boolean sprintToggled;
    private double distortionEffectScale;
    private double fovEffectScale;
    private double darknessEffectScale;
    private double mouseSensitivity;
    private int fov;
    private boolean monochromeLogo;
    private boolean hideLightningFlashes;
    private double gamma;
    private int viewDistance;
    private double entityDistanceScaling;
    private int guiScale;
    private ParticlesMode particles;
    private int maxFps;
    private GraphicsMode graphicsMode;
    private AoMode ao;
    private CloudRenderMode cloudRenderMode;
    private AttackIndicator attackIndicator;
    private LanguageDefinition language;
    private ChatVisibility chatVisibility;
    private double chatOpacity;
    private double chatLineSpacing;
    private double textBackgroundOpacity;
    private boolean backgroundForChatOnly;
    private Optional<VideoMode> fullscreenResolution;
    private boolean advancedItemTooltips;
    private boolean pauseOnLostFocus;
    private double chatHeightFocused;
    private double chatDelay;
    private double chatHeightUnfocused;
    private double chatScale;
    private double chatWidth;
    private int mipmapLevels;
    private Arm mainArm;
    private NarratorMode narrator;
    private int biomeBlendRadius;
    private double mouseWheelSensitivity;
    private boolean rawMouseInput;
    private boolean showAutosaveIndicator;
    private boolean chatPreview;
    private boolean onlyShowSecureChat;
    private Optional<Boolean> entityCulling;
    private boolean sneaking;
    private boolean sprinting;
    private boolean chunkborders;
    private boolean hitboxes;
    private Perspective perspective;
    private String piedirectory;
    private boolean hudHidden;
    private final String[] keysAll;
    private final float[] soundCategories;
    private final boolean[] playerModelParts;

    public OptionsCache(MinecraftClient client) {
        this.client = client;
        this.options = client.options;
        this.window = client.getWindow();
        keysAll = new String[options.allKeys.length];
        soundCategories = new float[SoundCategory.values().length];
        playerModelParts = new boolean[PlayerModelPart.values().length];
    }

    public void save(String levelName) {
        autoJump = options.getAutoJump().getValue();
        autoSuggestions = options.getAutoSuggestions().getValue();
        chatColors = options.getChatColors().getValue();
        chatLinks = options.getChatLinks().getValue();
        chatLinksPrompt = options.getChatLinksPrompt().getValue();
        enableVsync = options.getEnableVsync().getValue();
        entityShadows = options.getEntityShadows().getValue();
        forceUnicodeFont = options.getForceUnicodeFont().getValue();
        discreteMouseScroll = options.getDiscreteMouseScroll().getValue();
        invertYMouse = options.getInvertYMouse().getValue();
        reducedDebugInfo = options.getReducedDebugInfo().getValue();
        showSubtitles = options.getShowSubtitles().getValue();
        directionalAudio = options.getDirectionalAudio().getValue();
        touchscreen = options.getTouchscreen().getValue();
        fullscreen = options.getFullscreen().getValue();
        bobView = options.getBobView().getValue();
        sneakToggled = options.getSneakToggled().getValue();
        sprintToggled = options.getSprintToggled().getValue();
        monochromeLogo = options.getMonochromeLogo().getValue();
        hideLightningFlashes = options.getHideLightningFlashes().getValue();
        mouseSensitivity = options.getMouseSensitivity().getValue();
        fov = options.getFov().getValue();
        distortionEffectScale = options.getDistortionEffectScale().getValue();
        fovEffectScale = options.getFovEffectScale().getValue();
        darknessEffectScale = options.getDarknessEffectScale().getValue();
        gamma = options.getGamma().getValue();
        viewDistance = options.getViewDistance().getValue();
        entityDistanceScaling = options.getEntityDistanceScaling().getValue();
        guiScale = options.getGuiScale().getValue();
        particles = options.getParticles().getValue();
        maxFps = options.getMaxFps().getValue();
        graphicsMode = options.getGraphicsMode().getValue();
        ao = options.getAo().getValue();
        cloudRenderMode = options.getCloudRenderMod().getValue();
        attackIndicator = options.getAttackIndicator().getValue();
        language = client.getLanguageManager().getLanguage();
        chatVisibility = options.getChatVisibility().getValue();
        chatOpacity = options.getChtOpacity().getValue();
        chatLineSpacing = options.getChatLineSpacing().getValue();
        textBackgroundOpacity = options.getTextBackgroundOpacity().getValue();
        backgroundForChatOnly = options.getBackgroundForChatOnly().getValue();
        fullscreenResolution = window.getVideoMode();
        advancedItemTooltips = options.advancedItemTooltips;
        pauseOnLostFocus = options.pauseOnLostFocus;
        chatHeightFocused = options.getChatHeightFocused().getValue();
        chatDelay = options.getChatDelay().getValue();
        chatHeightUnfocused = options.getChatHeightUnfocused().getValue();
        chatScale = options.getChatScale().getValue();
        chatWidth = options.getChatWidth().getValue();
        mipmapLevels = options.getMipmapLevels().getValue();
        mainArm = options.getMainArm().getValue();
        narrator = options.getNarrator().getValue();
        biomeBlendRadius = options.getBiomeBlendRadius().getValue();
        mouseWheelSensitivity = options.getMouseWheelSensitivity().getValue();
        rawMouseInput = options.getRawMouseInput().getValue();
        showAutosaveIndicator = options.getShowAutosaveIndicator().getValue();
//        chatPreview = options.getChatPreview().getValue();
        onlyShowSecureChat = options.getOnlyShowSecureChat().getValue();
        entityCulling = StandardSettings.getEntityCulling();
        sneaking = options.sneakKey.isPressed();
        sprinting = options.sprintKey.isPressed();
        client.debugRenderer.toggleShowChunkBorder();
        chunkborders = client.debugRenderer.toggleShowChunkBorder();
        hitboxes = client.getEntityRenderDispatcher().shouldRenderHitboxes();
        perspective = options.getPerspective();
        piedirectory = ((MinecraftClientAccessor)client).getOpenProfilerSection();
        hudHidden = options.hudHidden;
        int i = 0;
        for (KeyBinding key : options.allKeys) {
            keysAll[i++] = key.getBoundKeyTranslationKey();
        }
        i = 0;
        for (SoundCategory sound : SoundCategory.values()) {
            soundCategories[i++] = options.getSoundVolume(sound);
        }
        i = 0;
        for (PlayerModelPart playerModelPart : PlayerModelPart.values()) {
            playerModelParts[i++] = options.isPlayerModelPartEnabled(playerModelPart);
        }

        StandardSettings.LOGGER.info("Cached options for '{}'" + (this.levelName != null ? " & abandoned old cache" : ""), this.levelName = levelName);
    }

    public void load(String levelName) {
        if (!levelName.equals(this.levelName)) {
            return;
        }
        options.getAutoJump().setValue(autoJump);
        options.getAutoSuggestions().setValue(autoSuggestions);
        options.getChatColors().setValue(chatColors);
        options.getChatLinks().setValue(chatLinks);
        options.getChatLinksPrompt().setValue(chatLinksPrompt);
        options.getEnableVsync().setValue(enableVsync);
        options.getEntityShadows().setValue(entityShadows);
        options.getForceUnicodeFont().setValue(forceUnicodeFont);
        options.getDiscreteMouseScroll().setValue(discreteMouseScroll);
        options.getInvertYMouse().setValue(invertYMouse);
        options.getReducedDebugInfo().setValue(reducedDebugInfo);
        options.getShowSubtitles().setValue(showSubtitles);
        options.getDirectionalAudio().setValue(directionalAudio);
        options.getTouchscreen().setValue(touchscreen);
        if (window.isFullscreen() != fullscreen) {
            if (client.isWindowFocused()) {
                window.toggleFullscreen();
            } else {
                StandardSettings.LOGGER.error("Could not reset fullscreen mode because window wasn't focused!");
            }
            options.getFullscreen().setValue(window.isFullscreen());
        }
        options.getBobView().setValue(bobView);
        options.getSneakToggled().setValue(sneakToggled);
        options.getSprintToggled().setValue(sprintToggled);
        options.getMonochromeLogo().setValue(monochromeLogo);
        options.getHideLightningFlashes().setValue(hideLightningFlashes);
        options.getMouseSensitivity().setValue(mouseSensitivity);
        options.getFov().setValue(fov);
        options.getDistortionEffectScale().setValue(distortionEffectScale);
        options.getFovEffectScale().setValue(fovEffectScale);
        options.getDarknessEffectScale().setValue(darknessEffectScale);
        options.getGamma().setValue(gamma);
        options.getViewDistance().setValue(viewDistance);
        options.getEntityDistanceScaling().setValue(entityDistanceScaling);
        options.getGuiScale().setValue(guiScale);
        options.getParticles().setValue(particles);
        options.getMaxFps().setValue(maxFps);
        options.getGraphicsMode().setValue(graphicsMode);
        options.getAo().setValue(ao);
        options.getCloudRenderMod().setValue(cloudRenderMode);
        options.getAttackIndicator().setValue(attackIndicator);
        if (!language.getCode().equals(options.language)) {
            client.getLanguageManager().setLanguage(language);
            client.getLanguageManager().reload(client.getResourceManager());
            options.language = client.getLanguageManager().getLanguage().getCode();
        }
        options.getChatVisibility().setValue(chatVisibility);
        options.getChtOpacity().setValue(chatOpacity);
        options.getChatLineSpacing().setValue(chatLineSpacing);
        options.getTextBackgroundOpacity().setValue(textBackgroundOpacity);
        options.getBackgroundForChatOnly().setValue(backgroundForChatOnly);
        if (fullscreenResolution != window.getVideoMode()) {
            window.setVideoMode(fullscreenResolution);
            window.applyVideoMode();
            options.fullscreenResolution = window.getVideoMode().toString();
        }
        options.advancedItemTooltips = advancedItemTooltips;
        options.pauseOnLostFocus = pauseOnLostFocus;
        options.getChatHeightFocused().setValue(chatHeightFocused);
        options.getChatDelay().setValue(chatDelay);
        options.getChatHeightUnfocused().setValue(chatHeightUnfocused);
        options.getChatScale().setValue(chatScale);
        options.getChatWidth().setValue(chatWidth);
        if (options.getMipmapLevels().getValue() != mipmapLevels) {
            options.getMipmapLevels().setValue(mipmapLevels);
            client.setMipmapLevels(options.getMipmapLevels().getValue());
            ((BakedModelManagerAccessor)client.getBakedModelManager()).callApply(((BakedModelManagerAccessor)client.getBakedModelManager()).callPrepare(client.getResourceManager(), client.getProfiler()), client.getResourceManager(), client.getProfiler());
        }
        options.getMainArm().setValue(mainArm);
        options.getNarrator().setValue(narrator);
        options.getBiomeBlendRadius().setValue(biomeBlendRadius);
        options.getMouseWheelSensitivity().setValue(mouseWheelSensitivity);
        options.getRawMouseInput().setValue(rawMouseInput);
        options.getShowAutosaveIndicator().setValue(showAutosaveIndicator);
        // options.getChatPreview().setValue(chatPreview);
        options.getOnlyShowSecureChat().setValue(onlyShowSecureChat);
        entityCulling.ifPresent(StandardSettings::setEntityCulling);
        if (options.getSneakToggled().getValue() && (sneaking != options.sneakKey.isPressed())) {
            options.sneakKey.setPressed(true);
        }
        if (options.getSprintToggled().getValue() && (sprinting != options.sprintKey.isPressed())) {
            options.sprintKey.setPressed(true);
        }
        if (client.debugRenderer.toggleShowChunkBorder() != chunkborders) {
            client.debugRenderer.toggleShowChunkBorder();
        }
        client.getEntityRenderDispatcher().setRenderHitboxes(hitboxes);
        options.setPerspective(perspective);
        ((MinecraftClientAccessor)client).setOpenProfilerSection(piedirectory);
        options.hudHidden = hudHidden;
        int i = 0;
        for (KeyBinding keyBinding : options.allKeys) {
            keyBinding.setBoundKey(InputUtil.fromTranslationKey(keysAll[i++]));
        }
        KeyBinding.updateKeysByCode();
        i = 0;
        for (SoundCategory soundCategory : SoundCategory.values()) {
            options.setSoundVolume(soundCategory, soundCategories[i++]);
        }
        i = 0;
        for (PlayerModelPart playerModelPart : PlayerModelPart.values()) {
            options.togglePlayerModelPart(playerModelPart, playerModelParts[i++]);
        }

        StandardSettings.LOGGER.info("Restored cached options for '{}'", this.levelName);
        this.levelName = null;
    }

}