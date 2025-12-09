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
                    target = "Lnet/minecraft/client/option/KeyBinding;matchesKey(II)Z",
                    ordinal = 0
            ),
            slice = @Slice(
                    from = @At(
                            value = "FIELD",
                            target = "Lnet/minecraft/client/option/GameOptions;keyFullscreen:Lnet/minecraft/client/option/KeyBinding;"
                    )
            )
    )
    private boolean dontToggleFullscreenWhenFocusingKeyBinding(boolean matchesKey) {
        return matchesKey && !StandardSettings.config.hasFocusedKeyBinding();
    }


    @ModifyExpressionValue(
            method = "onKey",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/option/KeyBinding;matchesKey(II)Z",
                    ordinal = 0
            ),
            slice = @Slice(
                    from = @At(
                            value = "FIELD",
                            target = "Lnet/minecraft/client/option/GameOptions;keyScreenshot:Lnet/minecraft/client/option/KeyBinding;"
                    )
            )
    )
    private boolean dontScreenshotWhenFocusingKeyBinding(boolean matchesKey) {
        return matchesKey && !StandardSettings.config.hasFocusedKeyBinding();
    }
}
