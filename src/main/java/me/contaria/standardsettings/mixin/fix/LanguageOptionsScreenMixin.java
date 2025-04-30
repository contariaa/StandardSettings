package me.contaria.standardsettings.mixin.fix;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import me.contaria.standardsettings.gui.StandardSettingsLanguageScreen;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.option.GameOptionsScreen;
import net.minecraft.client.gui.screen.option.LanguageOptionsScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.option.GameOptions;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LanguageOptionsScreen.class)
public abstract class LanguageOptionsScreenMixin extends GameOptionsScreen {

    public LanguageOptionsScreenMixin(Screen parent, GameOptions options, Text title) {
        super(parent, options, title);
    }

    @WrapWithCondition(
            method = "init",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/screen/option/LanguageOptionsScreen;addDrawableChild(Lnet/minecraft/client/gui/Element;)Lnet/minecraft/client/gui/Element;",
                    ordinal = 0
            )
    )
    private boolean doNotAddForceUnicodeFontButton(LanguageOptionsScreen screen, Element element) {
        return !this.isStandardSettings();
    }

    @WrapOperation(
            method = "init",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/widget/ButtonWidget$Builder;dimensions(IIII)Lnet/minecraft/client/gui/widget/ButtonWidget$Builder;"
            )
    )
    private ButtonWidget.Builder moveDoneButtonToCenter(ButtonWidget.Builder builder, int x, int y, int width, int height, Operation<ButtonWidget.Builder> original) {
        if (this.isStandardSettings()) {
            x = this.width / 2 - 100;
            width = 200;
        }
        return original.call(builder, x, y, width, height);
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
