package me.contaria.standardsettings.interfaces;

import net.minecraft.client.OptionInstance;

public interface StandardSettingsSimpleOption<T> {

    OptionInstance<T> standardsettings$copy();

    OptionInstance<T> standardsettings$copy(OptionInstance.ValueSet<T> callbacks);
}
