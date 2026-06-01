package me.contaria.standardsettings.mixin.fix;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import me.contaria.standardsettings.gui.StandardSettingsLanguageScreen;
import net.minecraft.client.Options;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.LanguageSelectScreen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Slice;

@Mixin(LanguageSelectScreen.class)
public abstract class LanguageSelectScreenMixin extends OptionsSubScreen {

    public LanguageSelectScreenMixin(Screen parent, Options options, Component title) {
        super(parent, options, title);
    }

    @WrapWithCondition(
            method = "addFooter",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/layouts/LinearLayout;addChild(Lnet/minecraft/client/gui/layouts/LayoutElement;)Lnet/minecraft/client/gui/layouts/LayoutElement;",
                    ordinal = 0
            ),
            slice = @Slice(
                    from = @At(value = "CONSTANT", args = "stringValue=options.font")
            )
    )
    private boolean doNotAddFontOptionsButton(LinearLayout layout, LayoutElement widget) {
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
