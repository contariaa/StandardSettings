package me.contaria.standardsettings.gui;

import me.contaria.standardsettings.options.CustomStandardSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.option.LanguageOptionsScreen;

public class StandardSettingsLanguageScreen extends LanguageOptionsScreen {
    public final CustomStandardSetting<String> setting;

    public StandardSettingsLanguageScreen(MinecraftClient client, CustomStandardSetting<String> setting) {
        super(client.currentScreen, client.options, client.getLanguageManager());
        this.setting = setting;
    }
}
