package me.contaria.standardsettings.gui;

import me.contaria.standardsettings.mixin.accessors.OptionSliderWidgetAccessor;
import me.contaria.standardsettings.options.FloatOptionStandardSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.widget.OptionSliderWidget;
import net.minecraft.client.option.GameOptions;

public class StandardOptionSliderWidget extends OptionSliderWidget {
    public final FloatOptionStandardSetting setting;
    public final GameOptions options;

    public StandardOptionSliderWidget(int i, int j, int k, GameOptions.Option option, FloatOptionStandardSetting setting, GameOptions options) {
        super(i, j, k, option);
        this.setting = setting;
        this.options = options;

        // update initial value and message, see parent constructor
        ((OptionSliderWidgetAccessor) this).standardsettings$setValue(option.getRatio(options.getIntValue(option)));
    }

    @Override
    public void render(MinecraftClient client, int mouseX, int mouseY) {
        this.message = this.setting.getText();
        super.render(client, mouseX, mouseY);
    }

    @Override
    protected void mouseDragged(MinecraftClient client, int mouseX, int mouseY) {
        super.mouseDragged(client, mouseX, mouseY);
        this.message = this.setting.getText();
    }

    @Override
    public boolean isMouseOver(MinecraftClient client, int mouseX, int mouseY) {
        boolean isMouseOver = super.isMouseOver(client, mouseX, mouseY);
        this.message = this.setting.getText();
        return isMouseOver;
    }
}
