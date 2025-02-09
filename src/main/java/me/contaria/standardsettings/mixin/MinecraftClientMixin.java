package me.contaria.standardsettings.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import me.contaria.standardsettings.StandardSettings;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.LevelLoadingScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.world.level.LevelInfo;
import net.minecraft.world.level.LevelProperties;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin {
    @Shadow
    @Nullable
    private IntegratedServer server;

    @Shadow
    public abstract boolean isWindowFocused();

    @Shadow
    public abstract void openPauseMenu(boolean pause);

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
                StandardSettings.autoF3EscPending = StandardSettings.config.autoF3Esc;
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

    @Inject(
            method = "tick",
            at = @At("HEAD")
    )
    private void autoF3Esc(CallbackInfo ci) {
        StandardSettings.autoF3EscPending &= this.server != null && !this.isWindowFocused();
        if (StandardSettings.autoF3EscPending) {
            if (StandardSettings.config.autoF3EscDelay > 0) {
                StandardSettings.config.autoF3EscDelay--;
            } else {
                this.openPauseMenu(true);
            }
        }
    }

    @Inject(
            method = "openScreen",
            at = @At("TAIL")
    )
    private void autoF3Esc_onPreview(Screen screen, CallbackInfo ci) {
        if (screen instanceof LevelLoadingScreen && StandardSettings.config.autoF3Esc) {
            String backToGame = I18n.translate("menu.returnToGame");
            for (Element e : screen.children()) {
                if (!(e instanceof ButtonWidget)) {
                    continue;
                }
                ButtonWidget button = (ButtonWidget) e;
                if (backToGame.equals(button.getMessage())) {
                    button.onPress();
                    break;
                }
            }
        }

        StandardSettings.autoF3EscPending &= !(screen instanceof GameMenuScreen);
    }
}
