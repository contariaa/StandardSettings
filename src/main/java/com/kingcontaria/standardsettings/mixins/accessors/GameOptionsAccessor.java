package com.kingcontaria.standardsettings.mixins.accessors;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.GameOptions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(GameOptions.class)

public interface GameOptionsAccessor {
    @Accessor
    MinecraftClient getClient();
}
