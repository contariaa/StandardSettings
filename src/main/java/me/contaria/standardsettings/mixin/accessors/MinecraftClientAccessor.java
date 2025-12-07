package me.contaria.standardsettings.mixin.accessors;

import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(MinecraftClient.class)
public interface MinecraftClientAccessor {
    @Accessor("openProfilerSection")
    String standardsettings$getOpenProfilerSection();

    @Accessor("openProfilerSection")
    void standardsettings$setOpenProfilerSection(String openProfilerSection);
}
