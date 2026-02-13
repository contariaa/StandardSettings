package me.contaria.standardsettings.gui;

import me.contaria.standardsettings.mixin.accessors.OptionSliderWidgetAccessor;
import me.contaria.standardsettings.options.FloatOptionStandardSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.widget.OptionSliderWidget;
import net.minecraft.client.option.GameOption;
import net.minecraft.client.option.GameOptions;

public class StandardOptionSliderWidget extends OptionSliderWidget {
    public final FloatOptionStandardSetting setting;
    public final GameOptions options;

    public StandardOptionSliderWidget(int id, int x, int y, GameOption option, FloatOptionStandardSetting setting, GameOptions options) {
        super(id, x, y, option);
        this.setting = setting;
        this.options = options;

        // update initial value and message, see parent constructor
        ((OptionSliderWidgetAccessor) this).standardsettings$setValue(option.method_6660(options.getFLoatOption(option)));
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
