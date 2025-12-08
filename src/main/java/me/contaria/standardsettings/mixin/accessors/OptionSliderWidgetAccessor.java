package me.contaria.standardsettings.mixin.accessors;

import net.minecraft.client.gui.widget.OptionSliderWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(OptionSliderWidget.class)
public interface OptionSliderWidgetAccessor {
    @Accessor("value")
    void standardsettings$setValue(float value);
}
