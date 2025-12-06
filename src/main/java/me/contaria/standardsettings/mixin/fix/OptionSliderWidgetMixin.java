package me.contaria.standardsettings.mixin.fix;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import me.contaria.standardsettings.gui.StandardOptionSliderWidget;
import net.minecraft.client.gui.widget.OptionSliderWidget;
import net.minecraft.client.option.GameOptions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(OptionSliderWidget.class)
public abstract class OptionSliderWidgetMixin {

    @ModifyExpressionValue(
            method = "<init>(IIILnet/minecraft/client/option/GameOptions$Option;FF)V",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/client/MinecraftClient;options:Lnet/minecraft/client/option/GameOptions;"
            )
    )
    private GameOptions useStandardGameOptions(GameOptions options) {
        if ((Object) this instanceof StandardOptionSliderWidget) {
            return ((StandardOptionSliderWidget) (Object) this).options;
        }
        return options;
    }
}
