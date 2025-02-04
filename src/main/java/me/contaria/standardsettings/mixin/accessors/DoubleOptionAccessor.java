package me.contaria.standardsettings.mixin.accessors;

import net.minecraft.client.option.DoubleOption;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(DoubleOption.class)
public interface DoubleOptionAccessor extends OptionAccessor {
    @Invoker("adjust")
    double standardsettings$adjust(double value);
}
