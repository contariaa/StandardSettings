package me.contaria.standardsettings.mixin.fix;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import me.contaria.standardsettings.gui.StandardSettingsLanguageScreen;
import net.minecraft.client.gui.screen.option.LanguageOptionsScreen;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(targets = "net/minecraft/client/gui/screen/option/LanguageOptionsScreen$LanguageSelectionListWidget")
public abstract class LanguageSelectionListWidgetMixin {
    @Shadow
    @Final
    LanguageOptionsScreen field_18744;

    @ModifyExpressionValue(
            method = "<init>",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/resource/language/LanguageManager;getLanguage()Ljava/lang/String;"
            )
    )
    private String selectStandardSettingsLanguage(String language) {
        if (this.field_18744 instanceof StandardSettingsLanguageScreen screen) {
            return screen.setting.get();
        }
        return language;
    }
}
