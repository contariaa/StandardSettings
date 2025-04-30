package me.contaria.standardsettings.mixin;

import com.mojang.serialization.Codec;
import me.contaria.standardsettings.interfaces.StandardSettingsSimpleOption;
import net.minecraft.client.option.SimpleOption;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Consumer;

@Mixin(SimpleOption.class)
public abstract class SimpleOptionMixin<T> implements StandardSettingsSimpleOption<T> {
    @Unique
    private String key;
    @Unique
    private SimpleOption.ValueTextGetter<T> valueTextGetter;
    @Unique
    private SimpleOption.Callbacks<T> callbacks;
    @Unique
    private T defaultValue;

    @Inject(
            method = "<init>(Ljava/lang/String;Lnet/minecraft/client/option/SimpleOption$TooltipFactory;Lnet/minecraft/client/option/SimpleOption$ValueTextGetter;Lnet/minecraft/client/option/SimpleOption$Callbacks;Lcom/mojang/serialization/Codec;Ljava/lang/Object;Ljava/util/function/Consumer;)V",
            at = @At("TAIL")
    )
    private void setCopySupplier(String key, SimpleOption.TooltipFactory<T> tooltipFactory, SimpleOption.ValueTextGetter<T> valueTextGetter, SimpleOption.Callbacks<T> callbacks, Codec<T> codec, T defaultValue, Consumer<T> changeCallback, CallbackInfo ci) {
        this.key = key;
        this.valueTextGetter = valueTextGetter;
        this.callbacks = callbacks;
        this.defaultValue = defaultValue;
    }

    @Override
    public SimpleOption<T> standardsettings$copy() {
        return new SimpleOption<>(this.key, SimpleOption.emptyTooltip(), this.valueTextGetter, this.callbacks, this.defaultValue, value -> {
        });
    }

    @Override
    public SimpleOption<T> standardsettings$copy(SimpleOption.Callbacks<T> callbacks) {
        return new SimpleOption<>(this.key, SimpleOption.emptyTooltip(), this.valueTextGetter, callbacks, this.defaultValue, value -> {
        });
    }
}
