package com.kingcontaria.standardsettings.mixins;

import com.kingcontaria.standardsettings.StandardSettings;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.RunArgs;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.LevelLoadingScreen;
import net.minecraft.client.gui.screen.Screen;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.attribute.UserDefinedFileAttributeView;
import java.util.*;
import java.util.stream.Stream;

@Mixin(MinecraftClient.class)

public abstract class MinecraftClientMixin {

    @Shadow public abstract void openPauseMenu(boolean pause);

    @Shadow @Nullable public Screen currentScreen;
    @Shadow private boolean windowFocused;
    @Unique
    private int tickCount = -3;

    // initialize StandardSettings, doesn't use ClientModInitializer because GameOptions need to be initialized first
    @Inject(method = "<init>", at = @At("RETURN"))
    private void initializeStandardSettings(RunArgs args, CallbackInfo ci) {
        StandardSettings.initializeEntityCulling();

        // create standardoptions.txt
        if (!StandardSettings.standardoptionsFile.exists()) {
            StandardSettings.LOGGER.info("Creating StandardSettings File...");

            long start = System.nanoTime();

            // create config file if necessary
            if (!StandardSettings.standardoptionsFile.getParentFile().exists()) {
                if (!StandardSettings.standardoptionsFile.getParentFile().mkdir()) {
                    StandardSettings.LOGGER.error("Failed to create config file");
                    return;
                }
            }

            // create file and mark with current StandardSettings version
            try {
                UserDefinedFileAttributeView view = Files.getFileAttributeView(StandardSettings.standardoptionsFile.toPath(), UserDefinedFileAttributeView.class);
                Files.write(StandardSettings.standardoptionsFile.toPath(), StandardSettings.getStandardoptionsTxt().getBytes());
                view.write("standardsettings", Charset.defaultCharset().encode(StandardSettings.getVersion()));
                StandardSettings.LOGGER.info("Finished creating StandardSettings File ({} ms)", (System.nanoTime() - start) / 1000000.0f);
            } catch (IOException e) {
                StandardSettings.LOGGER.error("Failed to create StandardSettings File", e);
            }
            return;
        }

        // check the marked StandardSettings versions along the standardoptions file chain
        Map<UserDefinedFileAttributeView, int[]> fileVersionsMap = new HashMap<>();
        List<String> lines = new ArrayList<>();
        List<File> fileChain = new ArrayList<>();
        try {
            // resolve standardoptions file chain
            File file = StandardSettings.standardoptionsFile;
            do {
                fileChain.add(file);
                try {
                    lines = com.google.common.io.Files.readLines(file, StandardCharsets.UTF_8);
                } catch (IOException e) {
                    break;
                }
            } while (lines != null && lines.size() > 0 && (file = new File(lines.get(0))).exists() && !fileChain.contains(file));

            // get the StandardSettings versions marked to the files
            for (File file2 : fileChain) {
                UserDefinedFileAttributeView view = Files.getFileAttributeView(file2.toPath(), UserDefinedFileAttributeView.class);
                fileVersionsMap.put(view, readVersion(view));
            }
        } catch (Exception e) {
            StandardSettings.LOGGER.error("Failed to check for file versions", e);
        }

        // Finds the highest StandardSettings version of the file chain
        int[] highestVersion = new int[]{1,2,0,0};
        for (int[] fileVersion : fileVersionsMap.values()) {
            if (StandardSettings.compareVersions(highestVersion, fileVersion)) {
                highestVersion = fileVersion;
            }
        }

        // Update standardoptions file if necessary and update the StandardSettings versions marked to the file
        try {
            List<String> linesToAdd = StandardSettings.checkVersion(highestVersion, lines);
            if (linesToAdd != null) {
                com.google.common.io.Files.append(System.lineSeparator() + String.join(System.lineSeparator(), linesToAdd), fileChain.get(fileChain.size() - 1), Charset.defaultCharset());
                StandardSettings.LOGGER.info("Finished updating standardoptions.txt");
            }
            for (Map.Entry<UserDefinedFileAttributeView, int[]> entry : fileVersionsMap.entrySet()) {
                if (StandardSettings.compareVersions(entry.getValue(), StandardSettings.version)) {
                    try {
                        entry.getKey().write("standardsettings", Charset.defaultCharset().encode(StandardSettings.getVersion()));
                    } catch (IOException e) {
                        StandardSettings.LOGGER.error("Failed to sign version number to file", e);
                    }
                }
            }
        } catch (IOException e) {
            StandardSettings.LOGGER.error("Failed to update standardoptions.txt", e);
        }
    }

    // reads the last marked StandardSettings version from the file
    private int[] readVersion(UserDefinedFileAttributeView view) {
        try {
            String name = "standardsettings";
            ByteBuffer buf = ByteBuffer.allocate(view.size(name));
            view.read(name, buf);
            buf.flip();
            String value = Charset.defaultCharset().decode(buf).toString();
            return Stream.of(value.split("\\.")).mapToInt(Integer::parseInt).toArray();
        } catch (Exception e) {
            return new int[]{1,2,0,0};
        }
    }

    // activate OnWorldJoin options when focusing the instance
    @Inject(method = "onWindowFocusChanged", at = @At("RETURN"))
    private void changeSettingsOnJoin(boolean focused, CallbackInfo ci) {
        if (focused && StandardSettings.changeOnWindowActivation) {
            StandardSettings.changeOnWindowActivation = false;
            StandardSettings.changeSettingsOnJoin();
        }
    }

    // activate OnWorldJoin Options when resizing the instance
    @Inject(method = "onResolutionChanged", at = @At("HEAD"))
    private void changeSettingsOnResize(CallbackInfo ci) {
        if (StandardSettings.changeOnWindowActivation && StandardSettings.changeOnResize) {
            StandardSettings.changeOnWindowActivation = false;
            StandardSettings.changeSettingsOnJoin();
        }
    }

    // save the world file name of the last world
    @Inject(method = "disconnect(Lnet/minecraft/client/gui/screen/Screen;)V", at = @At("HEAD"))
    private void cacheOptions(CallbackInfo ci) {
        try {
            StandardSettings.lastWorld = StandardSettings.client.getServer().getIconFile().get().getParent().getFileName().toString();
        } catch (Exception e) {
            // empty catch block
        }
    }
    @Inject(method = "tick", at = @At("HEAD"))
    private void standardSettings_OnPauseNextTick(CallbackInfo ci) {
        if (StandardSettings.f3PauseSoon && !(currentScreen instanceof LevelLoadingScreen)) {
            // System.out.println("WHATt: " + tickCount); // useful debug line
            if (windowFocused) {
                StandardSettings.f3PauseSoon = false;
                tickCount = 1;
                return;
            }
            if (tickCount == -3) { tickCount = 1 + StandardSettings.firstWorldF3PauseDelay; }
            if (tickCount > 0) {
                tickCount--;
                return;
            }
            tickCount = 1;
            openPauseMenu(true);
            StandardSettings.f3PauseSoon = !(currentScreen instanceof GameMenuScreen);
        }
    }

}