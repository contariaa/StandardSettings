package me.contaria.standardsettings.mixin.fix;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import me.contaria.standardsettings.gui.StandardSettingsLanguageScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.option.GameOptionsScreen;
import net.minecraft.client.gui.screen.option.LanguageOptionsScreen;
import net.minecraft.client.gui.widget.DirectionalLayoutWidget;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.client.option.GameOptions;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Slice;

@Mixin(LanguageOptionsScreen.class)
public abstract class LanguageOptionsScreenMixin extends GameOptionsScreen {

    public LanguageOptionsScreenMixin(Screen parent, GameOptions options, Text title) {
        super(parent, options, title);
    }

    @WrapWithCondition(
            method = "initFooter",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/widget/DirectionalLayoutWidget;add(Lnet/minecraft/client/gui/widget/Widget;)Lnet/minecraft/client/gui/widget/Widget;",
                    ordinal = 0
            ),
            slice = @Slice(
                    from = @At(value = "CONSTANT", args = "stringValue=options.font")
            )
    )
    private boolean doNotAddFontOptionsButton(DirectionalLayoutWidget layout, Widget widget) {
        return !this.isStandardSettings();
    }

    @WrapOperation(
            method = "onDone",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/lang/String;equals(Ljava/lang/Object;)Z",
                    remap = false
            )
    )
    private boolean setStandardSettingsLanguage(String newLanguage, Object currentLanguage, Operation<Boolean> original) {
        if ((Object) this instanceof StandardSettingsLanguageScreen screen) {
            // set standardsettings language and skip reloading client language
            screen.setting.set(newLanguage);
            return true;
        }
        return original.call(newLanguage, currentLanguage);
    }

    @Unique
    private boolean isStandardSettings() {
        return (Object) this instanceof StandardSettingsLanguageScreen;
    }
}
