package me.contaria.standardsettings.mixin.accessors;

import net.minecraft.client.option.Option;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Option.class)
public interface OptionAccessor {
    @Invoker("getDisplayPrefix")
    Text standardsettings$getDisplayPrefix();
}
