package me.contaria.standardsettings.mixin.accessors;

import net.minecraft.client.gui.widget.TextFieldWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(TextFieldWidget.class)
public interface TextFieldWidgetAccessor {
    @Mutable
    @Accessor("x")
    int standardsettings$getX();

    @Mutable
    @Accessor("x")
    void standardsettings$setX(int x);

    @Mutable
    @Accessor("y")
    int standardsettings$getY();

    @Mutable
    @Accessor("y")
    void standardsettings$setY(int y);
}
