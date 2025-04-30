package me.contaria.standardsettings.mixin.accessors;

import net.minecraft.client.option.SimpleOption;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.function.Function;

@Mixin(SimpleOption.class)
public interface SimpleOptionAccessor<T> {
    @Accessor("textGetter")
    Function<T, Text> standardsettings$getTextGetter();

    @Mutable
    @Accessor("textGetter")
    void standardsettings$setTextGetter(Function<T, Text> textGetter);

    @Accessor("text")
    Text standardsettings$getText();

    @Accessor("value")
    void standardsettings$setValue(T value);
}
