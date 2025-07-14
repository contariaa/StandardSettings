package com.kingcontaria.standardsettings.mixins;

import com.kingcontaria.standardsettings.IfWPExists;
import com.kingcontaria.standardsettings.StandardSettings;
import net.minecraft.client.gui.screen.LevelLoadingScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelLoadingScreen.class)
public abstract class LevelLoadingScreenMixin extends Screen {
    // For the WorldPreview to get paused
    @Unique
    private boolean previewPauseDone = false;

    protected LevelLoadingScreenMixin(Text title) {
        super(title);
    }

    @Inject(method = "render", at = @At("HEAD"))
    public void standardSettings_render(MatrixStack matrices, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (StandardSettings.hasWP && !this.previewPauseDone) {
            this.previewPauseDone = IfWPExists.handleLevelLoadScreenRender();
        }
    }

}
