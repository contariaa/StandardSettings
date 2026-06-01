package me.contaria.standardsettings.mixin.accessors;

import net.minecraft.client.gui.components.debugchart.ProfilerPieChart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ProfilerPieChart.class)
public interface PieChartAccessor {
    @Accessor("profilerTreePath")
    String standardsettings$getCurrentPath();

    @Accessor("profilerTreePath")
    void standardsettings$setCurrentPath(String currentPath);
}
