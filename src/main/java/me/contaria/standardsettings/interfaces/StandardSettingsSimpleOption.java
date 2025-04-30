package me.contaria.standardsettings.interfaces;

import net.minecraft.client.option.SimpleOption;

public interface StandardSettingsSimpleOption<T> {

    SimpleOption<T> standardsettings$copy();

    SimpleOption<T> standardsettings$copy(SimpleOption.Callbacks<T> callbacks);
}
