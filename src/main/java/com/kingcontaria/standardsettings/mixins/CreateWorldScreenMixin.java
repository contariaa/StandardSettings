package com.kingcontaria.standardsettings.mixins;

import com.kingcontaria.standardsettings.StandardSettings;
import net.minecraft.client.gui.screen.world.CreateWorldScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CreateWorldScreen.class)

public class CreateWorldScreenMixin {

    private static boolean shouldResetSettings = true;

    // reset settings to standardoptions at the start of world creation
    @Inject(method = "createLevel", at = @At("HEAD"))
    private void resetSettings(CallbackInfo info) {
        // don't reset settings if the last world was reset on world preview
        if (shouldResetSettings) {
            StandardSettings.LOGGER.info("Reset to StandardSettings...");
            StandardSettings.load();
            StandardSettings.LOGGER.info("Checking and saving Settings...");
            StandardSettings.checkSettings();
            shouldResetSettings = false;
        }
    }

    // activate OnWorldJoin options when finishing world creation
    // if instance is unfocused, it will instead wait
    @Inject(method = "createLevel", at = @At("RETURN"))
    private void onWorldJoin(CallbackInfo ci) {
        if (StandardSettings.client.isWindowFocused()) {
            StandardSettings.changeSettingsOnJoin();
        } else {
            StandardSettings.changeOnWindowActivation = true;
            if (StandardSettings.f3PauseOnWorldLoad) {
                StandardSettings.f3PauseSoon = true;
            }
        }
        shouldResetSettings = true;
    }

}