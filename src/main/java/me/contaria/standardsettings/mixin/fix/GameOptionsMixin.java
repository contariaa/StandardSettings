package me.contaria.standardsettings.mixin.fix;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import me.contaria.standardsettings.StandardGameOptions;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.sound.SoundCategory;
import net.minecraft.client.sound.SoundManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(GameOptions.class)
public abstract class GameOptionsMixin {

    @WrapOperation(
            method = "<init>*",
            at = @At(
                    value = "NEW",
                    target = "(Ljava/lang/String;ILjava/lang/String;)Lnet/minecraft/client/option/KeyBinding;"
            )
    )
    private KeyBinding doNotCreateKeyBindings(String translationKey, int code, String category, Operation<KeyBinding> original) {
        if (this.isStandardSettings()) {
            return null;
        }
        return original.call(translationKey, code, category);
    }

    @WrapWithCondition(
            method = "getBooleanValue",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/WorldRenderer;reload()V"
            )
    )
    private boolean doNotReloadWorldRenderer(WorldRenderer worldRenderer) {
        return !this.isStandardSettings();
    }

    @WrapWithCondition(
            method = "getBooleanValue",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/font/TextRenderer;setUnicode(Z)V"
            )
    )
    private boolean doNotSetUnicode(TextRenderer textRenderer, boolean unicode) {
        return !this.isStandardSettings();
    }

    @WrapWithCondition(
            method = "getBooleanValue",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/MinecraftClient;reloadResources()V"
            )
    )
    private boolean doNotReloadResources(MinecraftClient client) {
        return !this.isStandardSettings();
    }

    @WrapWithCondition(
            method = "getBooleanValue",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/MinecraftClient;toggleFullscreen()V"
            )
    )
    private boolean doNotToggleFullscreen(MinecraftClient instance) {
        return !this.isStandardSettings();
    }

    @WrapWithCondition(
            method = "getBooleanValue",
            at = @At(
                    value = "INVOKE",
                    target = "Lorg/lwjgl/opengl/Display;setVSyncEnabled(Z)V"
            )
    )
    private boolean doNotSetVsyncEnabled(boolean sync) {
        return !this.isStandardSettings();
    }

    @WrapWithCondition(
            method = "setSoundVolume",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/sound/SoundManager;updateSoundVolume(Lnet/minecraft/client/sound/SoundCategory;F)V"
            )
    )
    private boolean doNotUpdateSoundVolume(SoundManager manager, SoundCategory category, float volume) {
        return !this.isStandardSettings();
    }

    @Unique
    private boolean isStandardSettings() {
        return (Object) this instanceof StandardGameOptions;
    }
}
