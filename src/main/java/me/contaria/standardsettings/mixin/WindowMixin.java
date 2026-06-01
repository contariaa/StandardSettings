package me.contaria.standardsettings.mixin;

import com.mojang.blaze3d.platform.Window;
import me.contaria.standardsettings.StandardSettings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Window.class)
public class WindowMixin {
    @Inject(
            method = "onFocus",
            at = @At("RETURN")
    )
    private void onWorldJoin_onWindowFocus(long handle, boolean focused, CallbackInfo ci) {
        if (StandardSettings.onWorldJoinPending && focused) {
            StandardSettings.onWorldJoin();
        }
    }
}
