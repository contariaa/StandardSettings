package me.contaria.standardsettings.mixin.accessors;

import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.client.option.CyclingOption;
import net.minecraft.client.option.GameOptions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.function.Function;
import java.util.function.Supplier;

@Mixin(CyclingOption.class)
public interface CyclingOptionAccessor {
    @Accessor("setter")
    CyclingOption.Setter<Object> standardsettings$getSetter();

    @Accessor("getter")
    Function<GameOptions, Object> standardsettings$getGetter();

    @Accessor("buttonBuilderFactory")
    Supplier<CyclingButtonWidget.Builder<Object>> standardsettings$getButtonBuilderFactory();
}
