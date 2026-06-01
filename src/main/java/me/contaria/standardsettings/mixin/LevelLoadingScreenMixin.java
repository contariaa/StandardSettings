package me.contaria.standardsettings.mixin;

import me.contaria.standardsettings.StandardSettings;
import me.contaria.standardsettings.interfaces.LevelLoadingScreenEx;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelLoadingScreen.class)
public abstract class LevelLoadingScreenMixin extends Screen implements LevelLoadingScreenEx {
    @Unique
    private String levelId;

    @Unique
    private boolean newWorld;

    protected LevelLoadingScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/LevelLoadingScreen;onClose()V"))
    private void onWorldJoin(CallbackInfo ci) {
        if (!Minecraft.getInstance().isSameThread()) {
            return;
        }
        if (this.newWorld) {
            StandardSettings.saveToWorldFile(this.levelId);
            if (StandardSettings.isEnabled()) {
                if (this.minecraft.isWindowActive()) {
                    StandardSettings.onWorldJoin();
                } else {
                    StandardSettings.onWorldJoinPending = true;
                    StandardSettings.autoF3EscPending = StandardSettings.config.autoF3Esc;
                }
            }
        }

        StandardSettings.lastWorld = this.levelId;
    }

    @Override
    public void standardsettings$setLevelId(String levelId) {
        this.levelId = levelId;
    }

    @Override
    public void standardsettings$setNewWorld(boolean newWorld) {
        this.newWorld = newWorld;
    }
}
