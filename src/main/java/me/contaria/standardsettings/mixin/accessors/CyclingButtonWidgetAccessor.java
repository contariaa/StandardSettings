package me.contaria.standardsettings.mixin.accessors;

import net.minecraft.client.gui.widget.CyclingButtonWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(CyclingButtonWidget.class)
public interface CyclingButtonWidgetAccessor {
    @Mutable
    @Accessor("optionTextOmitted")
    void standardsettings$setOptionTextOmitted(boolean optionTextOmitted);
}
