package me.contaria.standardsettings.mixin.fix;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import me.contaria.standardsettings.StandardGameOptions;
import me.contaria.standardsettings.mixin.accessors.GameOptionsScreenAccessor;
import net.minecraft.client.gui.screen.options.LanguageOptionsScreen;
import net.minecraft.client.options.GameOptions;
import net.minecraft.client.resource.language.LanguageDefinition;
import net.minecraft.client.resource.language.LanguageManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(targets = "net/minecraft/client/gui/screen/options/LanguageOptionsScreen$LanguageSelectionListWidget")
public abstract class LanguageSelectionListWidgetMixin {
    @Shadow
    @Final
    LanguageOptionsScreen field_18744;

    @WrapOperation(
            method = "<init>",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/resource/language/LanguageManager;getLanguage()Lnet/minecraft/client/resource/language/LanguageDefinition;"
            )
    )
    private LanguageDefinition selectStandardSettingsLanguage(LanguageManager manager, Operation<LanguageDefinition> original) {
        GameOptions options = ((GameOptionsScreenAccessor) this.field_18744).standardsettings$getGameOptions();
        if (options instanceof StandardGameOptions) {
            return manager.getLanguage(options.language);
        }
        return original.call(manager);
    }
}
