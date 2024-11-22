package com.kingcontaria.standardsettings.mixins.accessors;

import net.minecraft.client.gui.hud.debug.PieChart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(PieChart.class)
public interface PieChartAccessor {
    @Accessor
    String getCurrentPath();

    @Accessor
    void setCurrentPath(String currentPath);
}
