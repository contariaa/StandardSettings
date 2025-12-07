package me.contaria.standardsettings.mixin.accessors;

import net.minecraft.client.util.Monitor;
import net.minecraft.client.util.Window;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Window.class)
public interface WindowAccessor {
    @Accessor("monitor")
    Monitor standardsettings$getMonitor();
}
