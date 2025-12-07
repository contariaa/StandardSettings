package me.contaria.standardsettings.mixin.accessors;

import net.minecraft.client.gui.menu.options.LanguageOptionsScreen;
import net.minecraft.client.options.GameOptions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LanguageOptionsScreen.class)
public interface LanguageOptionsScreenAccessor {
    @Accessor("options")
    GameOptions standardsettings$getOptions();
}
