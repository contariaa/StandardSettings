package me.contaria.standardsettings.mixin.fix;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import me.contaria.standardsettings.StandardGameOptions;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.option.GameOptionsScreen;
import net.minecraft.client.gui.screen.option.LanguageOptionsScreen;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.resource.language.LanguageDefinition;
import net.minecraft.client.resource.language.LanguageManager;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LanguageOptionsScreen.class)
public abstract class LanguageOptionsScreenMixin extends GameOptionsScreen {

    public LanguageOptionsScreenMixin(Screen parent, GameOptions gameOptions, Text title) {
        super(parent, gameOptions, title);
    }

    @WrapWithCondition(
            method = "method_19820",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/resource/language/LanguageManager;setLanguage(Lnet/minecraft/client/resource/language/LanguageDefinition;)V"
            )
    )
    private boolean doNotSetLanguage(LanguageManager manager, LanguageDefinition language) {
        return !this.isStandardSettings();
    }

    @WrapWithCondition(
            method = "method_19820",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/MinecraftClient;reloadResources()Ljava/util/concurrent/CompletableFuture;"
            )
    )
    private boolean doNotReloadResources(MinecraftClient client) {
        return !this.isStandardSettings();
    }

    @Unique
    private boolean isStandardSettings() {
        return this.gameOptions instanceof StandardGameOptions;
    }
}
