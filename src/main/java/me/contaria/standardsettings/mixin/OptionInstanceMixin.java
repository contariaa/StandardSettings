package me.contaria.standardsettings.mixin;

import com.mojang.serialization.Codec;
import me.contaria.standardsettings.interfaces.StandardSettingsSimpleOption;
import net.minecraft.client.OptionInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Consumer;

@Mixin(OptionInstance.class)
public abstract class OptionInstanceMixin<T> implements StandardSettingsSimpleOption<T> {
    @Unique
    private String key;
    @Unique
    private OptionInstance.CaptionBasedToString<T> valueTextGetter;
    @Unique
    private OptionInstance.ValueSet<T> callbacks;
    @Unique
    private T defaultValue;

    @Inject(
            method = "<init>(Ljava/lang/String;Lnet/minecraft/client/OptionInstance$TooltipSupplier;Lnet/minecraft/client/OptionInstance$CaptionBasedToString;Lnet/minecraft/client/OptionInstance$ValueSet;Lcom/mojang/serialization/Codec;Ljava/lang/Object;Ljava/util/function/Consumer;)V",
            at = @At("TAIL")
    )
    private void setCopySupplier(String key, OptionInstance.TooltipSupplier<T> tooltipFactory, OptionInstance.CaptionBasedToString<T> valueTextGetter, OptionInstance.ValueSet<T> callbacks, Codec<T> codec, T defaultValue, Consumer<T> changeCallback, CallbackInfo ci) {
        this.key = key;
        this.valueTextGetter = valueTextGetter;
        this.callbacks = callbacks;
        this.defaultValue = defaultValue;
    }

    @Override
    public OptionInstance<T> standardsettings$copy() {
        return new OptionInstance<>(this.key, OptionInstance.noTooltip(), this.valueTextGetter, this.callbacks, this.defaultValue, value -> {});
    }

    @Override
    public OptionInstance<T> standardsettings$copy(OptionInstance.ValueSet<T> callbacks) {
        return new OptionInstance<>(this.key, OptionInstance.noTooltip(), this.valueTextGetter, callbacks, this.defaultValue, value -> {});
    }
}
