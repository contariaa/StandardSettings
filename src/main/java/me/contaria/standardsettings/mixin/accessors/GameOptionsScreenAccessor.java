package me.contaria.standardsettings.mixin.accessors;

import net.minecraft.client.gui.screen.options.GameOptionsScreen;
import net.minecraft.client.options.GameOptions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(GameOptionsScreen.class)
public interface GameOptionsScreenAccessor {
    @Accessor("gameOptions")
    GameOptions standardsettings$getGameOptions();
}
