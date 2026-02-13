package me.contaria.standardsettings.gui;

import me.contaria.standardsettings.mixin.accessors.SliderWidgetAccessor;
import me.contaria.standardsettings.options.DoubleOptionStandardSetting;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.DoubleOptionSliderWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;

public class SliderTextFieldStandardOptionWidget extends StandardOptionWidget {
    private final DoubleOptionStandardSetting setting;
    private final DoubleOptionSliderWidget slider;
    private final TextFieldWidget textField;
    private boolean alt;

    public SliderTextFieldStandardOptionWidget(DoubleOptionStandardSetting setting, DoubleOptionSliderWidget slider, TextFieldWidget textField) {
        super(setting, slider);
        this.setting = setting;
        this.slider = slider;
        this.textField = textField;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.slider.isMouseOver(mouseX, mouseY) && Screen.hasControlDown() && setting.isEnabled()) {
            this.alt = !alt;
            this.setMainWidget(this.alt ? textField : slider);
            ((SliderWidgetAccessor) this.slider).callSetValue(setting.getRatio(setting.get()));
            this.textField.setText(setting.get().toString());
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
