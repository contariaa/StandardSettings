package me.contaria.standardsettings.mixin.fix;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import me.contaria.standardsettings.StandardGameOptions;
import me.contaria.standardsettings.mixin.accessors.LanguageOptionsScreenAccessor;
import net.minecraft.client.gui.screen.options.LanguageOptionsScreen;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.resource.language.LanguageDefinition;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;
import java.util.Map;

@Mixin(targets = "net/minecraft/client/gui/screen/options/LanguageOptionsScreen$LanguageSelectionListWidget")
public abstract class LanguageSelectionListWidgetMixin {
    @Shadow
    @Final
    LanguageOptionsScreen field_1208;
    @Shadow
    @Final
    private Map<String, LanguageDefinition> languageDefinitions;
    @Shadow
    @Final
    private List<String> languageCodes;

    @WrapMethod(
            method = "selectEntry"
    )
    private void doNotReloadResources(int index, boolean doubleClick, int lastMouseX, int lastMouseY, Operation<Void> original) {
        GameOptions options = ((LanguageOptionsScreenAccessor) this.field_1208).standardsettings$getOptions();
        if (options instanceof StandardGameOptions) {
            options.language = this.languageDefinitions.get(this.languageCodes.get(index)).getCode();
            return;
        }
        original.call(index, doubleClick, lastMouseX, lastMouseY);
    }

    @ModifyExpressionValue(
            method = "isEntrySelected",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/resource/language/LanguageDefinition;getCode()Ljava/lang/String;"
            )
    )
    private String highlightStandardSettingsLanguage(String language) {
        GameOptions options = ((LanguageOptionsScreenAccessor) this.field_1208).standardsettings$getOptions();
        if (options instanceof StandardGameOptions) {
            return options.language;
        }
        return language;
    }
}
