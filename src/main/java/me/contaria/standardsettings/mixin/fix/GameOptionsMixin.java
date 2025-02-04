package me.contaria.standardsettings.mixin.fix;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import me.contaria.standardsettings.StandardGameOptions;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.option.StickyKeyBinding;
import net.minecraft.client.sound.SoundManager;
import net.minecraft.client.util.InputUtil;
import net.minecraft.sound.SoundCategory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.function.BooleanSupplier;

@Mixin(GameOptions.class)
public abstract class GameOptionsMixin {

    @WrapOperation(
            method = "<init>",
            at = @At(
                    value = "NEW",
                    target = "(Ljava/lang/String;ILjava/lang/String;)Lnet/minecraft/client/option/KeyBinding;"
            )
    )
    private KeyBinding doNotCreateKeyBindings(String translationKey, int code, String category, Operation<KeyBinding> original) {
        if (this.isStandardSettings()) {
            return null;
        }
        return original.call(translationKey, code, category);
    }

    @WrapOperation(
            method = "<init>",
            at = @At(
                    value = "NEW",
                    target = "(Ljava/lang/String;Lnet/minecraft/client/util/InputUtil$Type;ILjava/lang/String;)Lnet/minecraft/client/option/KeyBinding;"
            )
    )
    private KeyBinding doNotCreateKeyBindings(String translationKey, InputUtil.Type type, int code, String category, Operation<KeyBinding> original) {
        if (this.isStandardSettings()) {
            return null;
        }
        return original.call(translationKey, type, code, category);
    }

    @WrapOperation(
            method = "<init>",
            at = @At(
                    value = "NEW",
                    target = "(Ljava/lang/String;ILjava/lang/String;Ljava/util/function/BooleanSupplier;)Lnet/minecraft/client/option/StickyKeyBinding;"
            )
    )
    private StickyKeyBinding doNotCreateKeyBindings(String id, int code, String category, BooleanSupplier toggleGetter, Operation<StickyKeyBinding> original) {
        if (this.isStandardSettings()) {
            return null;
        }
        return original.call(id, code, category, toggleGetter);
    }

    @WrapWithCondition(
            method = "setSoundVolume",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/sound/SoundManager;updateSoundVolume(Lnet/minecraft/sound/SoundCategory;F)V"
            )
    )
    private boolean doNotUpdateSoundVolume(SoundManager manager, SoundCategory category, float volume) {
        return !this.isStandardSettings();
    }

    @Unique
    private boolean isStandardSettings() {
        return (Object) this instanceof StandardGameOptions;
    }
}
