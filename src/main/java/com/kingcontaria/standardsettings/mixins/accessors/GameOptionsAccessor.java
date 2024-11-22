package com.kingcontaria.standardsettings.mixins.accessors;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.GameOptions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(GameOptions.class)

public interface GameOptionsAccessor {
    @Invoker
    static void callOnFontOptionsChanged() {
        throw new UnsupportedOperationException();
    }

    @Accessor
    MinecraftClient getClient();
}
