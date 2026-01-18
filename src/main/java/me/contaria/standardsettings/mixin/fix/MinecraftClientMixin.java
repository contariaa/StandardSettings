package me.contaria.standardsettings.mixin.fix;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import me.contaria.standardsettings.StandardSettings;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin {

    @ModifyExpressionValue(
            method = "handleKeyInput",
            at = @At(
                    value = "INVOKE",
                    target = "Lorg/lwjgl/input/Keyboard;isRepeatEvent()Z"
            )
    )
    private boolean skipKeyInputOnFocusedKeyBinding(boolean isRepeatEvent) {
        return isRepeatEvent || StandardSettings.config.hasFocusedKeyBinding();
    }
}
