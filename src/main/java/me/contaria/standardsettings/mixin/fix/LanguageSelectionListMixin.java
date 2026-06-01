package me.contaria.standardsettings.mixin.fix;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import me.contaria.standardsettings.gui.StandardSettingsLanguageScreen;
import net.minecraft.client.gui.screens.options.LanguageSelectScreen;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(targets = "net/minecraft/client/gui/screens/options/LanguageSelectScreen$LanguageSelectionList")
public abstract class LanguageSelectionListMixin {
    @Shadow
    @Final
    LanguageSelectScreen this$0;

    @ModifyExpressionValue(
            method = "<init>",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/resources/language/LanguageManager;getSelected()Ljava/lang/String;"
            )
    )
    private String selectStandardSettingsLanguage(String language) {
        if (this.this$0 instanceof StandardSettingsLanguageScreen screen) {
            return screen.setting.get();
        }
        return language;
    }
}
