package me.contaria.standardsettings.mixin.accessors;

import net.minecraft.client.gui.components.debug.DebugScreenEntry;
import net.minecraft.client.gui.screens.debug.DebugOptionsScreen;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Comparator;
import java.util.Map;

@Mixin(DebugOptionsScreen.OptionList.class)
public interface OptionListAccessor {
    @Accessor("COMPARATOR")
    static Comparator<Map.Entry<Identifier, DebugScreenEntry>> getComparator() {
        throw new UnsupportedOperationException();
    }
}
