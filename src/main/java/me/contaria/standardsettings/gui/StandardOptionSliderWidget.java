package me.contaria.standardsettings.gui;

import me.contaria.standardsettings.options.FloatOptionStandardSetting;
import net.minecraft.client.gui.widget.OptionSliderWidget;
import net.minecraft.client.option.GameOptions;

public class StandardOptionSliderWidget extends OptionSliderWidget {
    public final FloatOptionStandardSetting setting;
    public final GameOptions options;

    public StandardOptionSliderWidget(int i, int j, int k, GameOptions.Option option, FloatOptionStandardSetting setting, GameOptions options) {
        super(i, j, k, option);
        this.setting = setting;
        this.options = options;
    }
}
