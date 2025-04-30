package me.contaria.standardsettings.mixin;

import me.contaria.speedrunapi.util.TextUtil;
import me.contaria.standardsettings.StandardSettings;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.LevelLoadingScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.resource.ResourcePackManager;
import net.minecraft.server.SaveLoader;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.text.Text;
import net.minecraft.world.level.storage.LevelStorage;
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

    @Inject(
            method = "startIntegratedServer",
            at = @At("HEAD")
    )
    private void reset(String levelName, LevelStorage.Session session, ResourcePackManager dataPackManager, SaveLoader saveLoader, boolean newWorld, CallbackInfo ci) {
        if (!MinecraftClient.getInstance().isOnThread()) {
            return;
        }
        if (newWorld) {
            StandardSettings.createCache();
            if (StandardSettings.isEnabled()) {
                StandardSettings.reset();
            }
        } else {
            StandardSettings.loadCache(levelName);
        }

        StandardSettings.resetPendingActions();
    }

    @Inject(
            method = "startIntegratedServer",
            at = @At("TAIL")
    )
    private void onWorldJoin(String levelName, LevelStorage.Session session, ResourcePackManager dataPackManager, SaveLoader saveLoader, boolean newWorld, CallbackInfo ci) {
        if (!MinecraftClient.getInstance().isOnThread()) {
            return;
        }
        if (newWorld) {
            StandardSettings.saveToWorldFile(levelName);
            if (StandardSettings.isEnabled()) {
                if (this.isWindowFocused()) {
                    StandardSettings.onWorldJoin();
                } else {
                    StandardSettings.onWorldJoinPending = true;
                    StandardSettings.autoF3EscPending = StandardSettings.config.autoF3Esc;
                }
            }
        }

        StandardSettings.lastWorld = levelName;
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
            method = "setScreen",
            at = @At("TAIL")
    )
    private void autoF3Esc_onPreview(Screen screen, CallbackInfo ci) {
        if (screen instanceof LevelLoadingScreen && StandardSettings.config.autoF3Esc) {
            Text backToGame = TextUtil.translatable("menu.returnToGame");
            for (Element e : screen.children()) {
                if (!(e instanceof ButtonWidget button)) {
                    continue;
                }
                if (backToGame.equals(button.getMessage())) {
                    button.onPress();
                    break;
                }
            }
        }

        StandardSettings.autoF3EscPending &= !(screen instanceof GameMenuScreen);
    }
}
