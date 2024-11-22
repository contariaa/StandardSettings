package com.kingcontaria.standardsettings.mixins;

import com.kingcontaria.standardsettings.StandardSettings;
import com.mojang.datafixers.DataFixer;
import net.minecraft.world.PlayerSaveHandler;
import net.minecraft.world.level.storage.LevelStorage;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

@Mixin(PlayerSaveHandler.class)

public class PlayerSaveHandlerMixin {

    @Shadow @Final private File playerDataDir;
    private boolean isNewWorld;

    @Inject(method = "<init>", at = @At(value = "INVOKE", target = "Ljava/io/File;mkdirs()Z", ordinal = 0))
    private void isNewWorld(LevelStorage.Session session, DataFixer dataFixer, CallbackInfo ci) {
        isNewWorld = !playerDataDir.exists();
    }

    // saves the standardoptions to world file for verification purposes
    @Inject(method = "<init>", at = @At("TAIL"))
    private void saveStandardoptionsTxt(LevelStorage.Session session, DataFixer dataFixer, CallbackInfo ci) {
        if (isNewWorld && StandardSettings.standardoptionsCache != null) {
            try {
                Files.write(playerDataDir.getParentFile().toPath().resolve("standardoptions.txt"), String.join(System.lineSeparator(), StandardSettings.standardoptionsCache).getBytes());
                StandardSettings.LOGGER.info("Saved standardoptions.txt to world file");
            } catch (IOException e) {
                StandardSettings.LOGGER.error("Failed to save standardoptions.txt to world file", e);
            }
        }
    }

}
