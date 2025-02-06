package me.contaria.standardsettings.mixin;

import me.contaria.speedrunapi.util.TextUtil;
import me.contaria.standardsettings.StandardSettings;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.LevelLoadingScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.server.SaveLoader;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.text.Text;
import net.minecraft.util.registry.DynamicRegistryManager;
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
    private void reset(String worldName, LevelInfo levelInfo, DynamicRegistryManager dynamicRegistryManager, GeneratorOptions generatorOptions, CallbackInfo ci) {
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
    private void onWorldJoin(String worldName, LevelInfo levelInfo, DynamicRegistryManager dynamicRegistryManager, GeneratorOptions generatorOptions, CallbackInfo ci) {
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
            method = "startIntegratedServer(Ljava/lang/String;Ljava/util/function/Function;Ljava/util/function/Function;ZLnet/minecraft/client/MinecraftClient$WorldLoadAction;)V",
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
            method = "startIntegratedServer(Ljava/lang/String;Ljava/util/function/Function;Ljava/util/function/Function;ZLnet/minecraft/client/MinecraftClient$WorldLoadAction;)V",
            at = @At("TAIL")
    )
    private void setLastWorld(String worldName, Function<LevelStorage.Session, SaveLoader.DataPackSettingsSupplier> dataPackSettingsSupplierGetter, Function<LevelStorage.Session, SaveLoader.SavePropertiesSupplier> savePropertiesSupplierGetter, boolean safeMode, @Coerce Object worldLoadAction, CallbackInfo ci) {
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
