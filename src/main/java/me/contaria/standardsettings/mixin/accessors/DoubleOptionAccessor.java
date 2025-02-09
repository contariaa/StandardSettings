package me.contaria.standardsettings.mixin.accessors;

import net.minecraft.client.options.DoubleOption;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(DoubleOption.class)
public interface DoubleOptionAccessor extends OptionAccessor {
    @Invoker("method_18618")
    double standardsettings$adjust(double value);
}
