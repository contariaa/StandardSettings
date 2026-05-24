package me.contaria.standardsettings.gui;

import me.contaria.standardsettings.options.CustomStandardSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.options.LanguageSelectScreen;

public class StandardSettingsLanguageScreen extends LanguageSelectScreen {
    public final CustomStandardSetting<String> setting;

    public StandardSettingsLanguageScreen(Minecraft client, CustomStandardSetting<String> setting) {
        super(client.screen, client.options, client.getLanguageManager());
        this.setting = setting;
    }
}
