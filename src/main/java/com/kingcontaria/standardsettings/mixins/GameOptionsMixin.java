package com.kingcontaria.standardsettings.mixins;

import com.kingcontaria.standardsettings.StandardSettings;
import com.kingcontaria.standardsettings.mixins.accessors.GameOptionsAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.SimpleOption;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

import java.util.function.Consumer;

@Mixin(GameOptions.class)
public class GameOptionsMixin {
    @Unique
    private static final int MAX_ENCODABLE_GUI_SCALE = Integer.MAX_VALUE - 1;

    @ModifyArgs(
            method = "<init>",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/option/SimpleOption;<init>(Ljava/lang/String;Lnet/minecraft/client/option/SimpleOption$TooltipFactory;Lnet/minecraft/client/option/SimpleOption$ValueTextGetter;Lnet/minecraft/client/option/SimpleOption$Callbacks;Ljava/lang/Object;Ljava/util/function/Consumer;)V",
                    ordinal = 0
            ),
            slice = @Slice(
                    from = @At(value = "CONSTANT", args = "stringValue=options.guiScale")
            )
    )
    private void overrideGuiScaleCallbacks(Args args) {
        // Normally when setting the GUI scale option's value, the game caps the value according to the current window size.
        // This is undesirable when loading StandardSettings values, since the window may be small now but be enlarged later in the reset.
        // We override the callbacks to avoid capping the GUI scale value while loading StandardSettings values.
        args.set(3, new SimpleOption.MaxSuppliableIntCallbacks(0, () -> {
            MinecraftClient minecraftClient = MinecraftClient.getInstance();
            return StandardSettings.isResetting() || !minecraftClient.isRunning()
                    ? MAX_ENCODABLE_GUI_SCALE
                    : minecraftClient.getWindow().calculateScaleFactor(0, minecraftClient.forcesUnicodeFont());
        }, MAX_ENCODABLE_GUI_SCALE));

        // This is equivalent to the `changeCallback` that vanilla passes in 1.20.5-1.20.6.
        // We set it explicitly here because vanilla passes a no-op in 1.20-1.20.4,
        // leading to a mismatch between the option's internal value and the rendered GUI scale.
        args.set(5, (Consumer<Integer>)(value -> ((GameOptionsAccessor) this).getClient().onResolutionChanged()));
    }
}
