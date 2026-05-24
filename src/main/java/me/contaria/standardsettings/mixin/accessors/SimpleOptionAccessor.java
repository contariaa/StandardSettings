package me.contaria.standardsettings.mixin.accessors;

import net.minecraft.client.OptionInstance;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.function.Function;

@Mixin(OptionInstance.class)
public interface SimpleOptionAccessor<T> {
    @Accessor("toString")
    Function<T, Component> standardsettings$getTextGetter();

    @Mutable
    @Accessor("toString")
    void standardsettings$setTextGetter(Function<T, Component> textGetter);

    @Accessor("caption")
    Component standardsettings$getText();

    @Accessor("value")
    void standardsettings$setValue(T value);
}
