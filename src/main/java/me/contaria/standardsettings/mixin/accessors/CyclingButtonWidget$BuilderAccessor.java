package me.contaria.standardsettings.mixin.accessors;

import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.function.Function;

@Mixin(CyclingButtonWidget.Builder.class)
public interface CyclingButtonWidget$BuilderAccessor {
    @Accessor("valueToText")
    Function<Object, Text> standardsettings$getValueToText();

    @Accessor("values")
    CyclingButtonWidget.Values<Object> standardsettings$getValues();
}
