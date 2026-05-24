package me.contaria.standardsettings.mixin.accessors;

import net.minecraft.client.gui.components.CycleButton;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(CycleButton.class)
public interface CyclingButtonWidgetAccessor {
    @Mutable
    @Accessor("displayState")
    void standardsettings$setOptionTextOmitted(CycleButton.DisplayState displayState);
}
