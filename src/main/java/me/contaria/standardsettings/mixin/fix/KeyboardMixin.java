package me.contaria.standardsettings.mixin.fix;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import me.contaria.standardsettings.StandardSettings;
import net.minecraft.client.Keyboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Slice;

@Mixin(Keyboard.class)
public abstract class KeyboardMixin {

    @ModifyExpressionValue(
            method = "onKey",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/options/KeyBinding;matchesKey(II)Z"
            ),
            slice = @Slice(
                    from = @At(
                            value = "FIELD",
                            target = "Lnet/minecraft/client/options/GameOptions;keyFullscreen:Lnet/minecraft/client/options/KeyBinding;"
                    ),
                    to = @At(
                            value = "INVOKE",
                            target = "Lnet/minecraft/client/util/ScreenshotUtils;saveScreenshot(Ljava/io/File;IILnet/minecraft/client/gl/Framebuffer;Ljava/util/function/Consumer;)V"
                    )
            )
    )
    private boolean dontToggleFullscreenOrScreenshotWhenFocusingKeyBinding(boolean matchesKey) {
        return matchesKey && !StandardSettings.config.hasFocusedKeyBinding();
    }
}
