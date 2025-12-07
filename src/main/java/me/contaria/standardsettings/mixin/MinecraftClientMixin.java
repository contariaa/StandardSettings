package me.contaria.standardsettings.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import me.contaria.standardsettings.StandardSettings;
import net.minecraft.client.MinecraftClient;
import net.minecraft.world.level.LevelInfo;
import net.minecraft.world.level.LevelProperties;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin {
    @Shadow
    public abstract boolean isWindowFocused();

    @ModifyExpressionValue(
            method = "startIntegratedServer",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/WorldSaveHandler;readProperties()Lnet/minecraft/world/level/LevelProperties;"
            )
    )
    private LevelProperties setIsNewWorld(LevelProperties properties, @Share("isNewWorld") LocalBooleanRef isNewWorld) {
        isNewWorld.set(properties == null);
        return properties;
    }

    @Inject(
            method = "startIntegratedServer",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/WorldSaveHandler;readProperties()Lnet/minecraft/world/level/LevelProperties;",
                    shift = At.Shift.AFTER
            )
    )
    private void reset(String name, String displayName, LevelInfo levelInfo, CallbackInfo ci, @Share("isNewWorld") LocalBooleanRef isNewWorld) {
        if (!MinecraftClient.getInstance().isOnThread() || !isNewWorld.get()) {
            return;
        }
        StandardSettings.createCache();
        if (StandardSettings.isEnabled()) {
            StandardSettings.reset();
        }
    }

    @Inject(
            method = "startIntegratedServer",
            at = @At("TAIL")
    )
    private void onWorldJoin(String name, String displayName, LevelInfo levelInfo, CallbackInfo ci, @Share("isNewWorld") LocalBooleanRef isNewWorld) {
        if (!MinecraftClient.getInstance().isOnThread() || !isNewWorld.get()) {
            return;
        }
        StandardSettings.saveToWorldFile(name);
        if (StandardSettings.isEnabled()) {
            if (this.isWindowFocused()) {
                StandardSettings.onWorldJoin();
            } else {
                StandardSettings.onWorldJoinPending = true;
            }
        }
    }

    @Inject(
            method = "onWindowFocusChanged",
            at = @At("RETURN")
    )
    private void onWorldJoin_onWindowFocus(boolean focused, CallbackInfo ci) {
        if (StandardSettings.onWorldJoinPending && focused) {
            StandardSettings.onWorldJoin();
        }
    }

    @Inject(
            method = "onResolutionChanged",
            at = @At("RETURN")
    )
    private void onWorldJoin_onResize(CallbackInfo ci) {
        if (StandardSettings.onWorldJoinPending && StandardSettings.config.triggerOnResize) {
            StandardSettings.onWorldJoin();
        }
    }

    @Inject(
            method = "startIntegratedServer",
            at = @At("HEAD")
    )
    private void resetPendingActions(CallbackInfo ci) {
        if (MinecraftClient.getInstance().isOnThread()) {
            StandardSettings.resetPendingActions();
        }
    }

    @Inject(
            method = "startIntegratedServer",
            at = @At("HEAD")
    )
    private void loadCache(String name, String displayName, LevelInfo levelInfo, CallbackInfo ci, @Share("isNewWorld") LocalBooleanRef isNewWorld) {
        if (MinecraftClient.getInstance().isOnThread() && !isNewWorld.get()) {
            StandardSettings.loadCache(name);
        }
    }

    @Inject(
            method = "startIntegratedServer",
            at = @At("TAIL")
    )
    private void setLastWorld(String name, String displayName, LevelInfo levelInfo, CallbackInfo ci) {
        if (MinecraftClient.getInstance().isOnThread()) {
            StandardSettings.lastWorld = name;
        }
    }
}
