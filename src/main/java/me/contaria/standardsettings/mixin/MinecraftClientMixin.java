package me.contaria.standardsettings.mixin;

import com.mojang.datafixers.util.Function4;
import me.contaria.standardsettings.StandardSettings;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.LevelLoadingScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.resource.DataPackSettings;
import net.minecraft.resource.ResourceManager;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.registry.RegistryTracker;
import net.minecraft.world.SaveProperties;
import net.minecraft.world.gen.GeneratorOptions;
import net.minecraft.world.level.LevelInfo;
import net.minecraft.world.level.storage.LevelStorage;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Function;

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
            method = "createWorld",
            at = @At("HEAD")
    )
    private void reset(String worldName, LevelInfo levelInfo, RegistryTracker.Modifiable registryTracker, GeneratorOptions generatorOptions, CallbackInfo ci) {
        if (!MinecraftClient.getInstance().isOnThread()) {
            return;
        }
        StandardSettings.createCache();
        if (StandardSettings.isEnabled()) {
            StandardSettings.reset();
        }
    }

    @Inject(
            method = "createWorld",
            at = @At("TAIL")
    )
    private void onWorldJoin(String worldName, LevelInfo levelInfo, RegistryTracker.Modifiable registryTracker, GeneratorOptions generatorOptions, CallbackInfo ci) {
        if (!MinecraftClient.getInstance().isOnThread()) {
            return;
        }
        StandardSettings.saveToWorldFile(worldName);
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
            method = "startIntegratedServer(Ljava/lang/String;Lnet/minecraft/util/registry/RegistryTracker$Modifiable;Ljava/util/function/Function;Lcom/mojang/datafixers/util/Function4;ZLnet/minecraft/client/MinecraftClient$WorldLoadAction;)V",
            at = @At("HEAD")
    )
    private void resetPendingActions(CallbackInfo ci) {
        if (MinecraftClient.getInstance().isOnThread()) {
            StandardSettings.resetPendingActions();
        }
    }

    @Inject(
            method = "startIntegratedServer(Ljava/lang/String;)V",
            at = @At("HEAD")
    )
    private void loadCache(String worldName, CallbackInfo ci) {
        if (MinecraftClient.getInstance().isOnThread()) {
            StandardSettings.loadCache(worldName);
        }
    }

    @Inject(
            method = "startIntegratedServer(Ljava/lang/String;Lnet/minecraft/util/registry/RegistryTracker$Modifiable;Ljava/util/function/Function;Lcom/mojang/datafixers/util/Function4;ZLnet/minecraft/client/MinecraftClient$WorldLoadAction;)V",
            at = @At("TAIL")
    )
    private void setLastWorld(String worldName, RegistryTracker.Modifiable registryTracker, Function<LevelStorage.Session, DataPackSettings> function, Function4<LevelStorage.Session, RegistryTracker.Modifiable, ResourceManager, DataPackSettings, SaveProperties> function4, boolean safeMode, @Coerce Object worldLoadAction, CallbackInfo ci) {
        if (MinecraftClient.getInstance().isOnThread()) {
            StandardSettings.lastWorld = worldName;
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
            Text backToGame = new TranslatableText("menu.returnToGame");
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
