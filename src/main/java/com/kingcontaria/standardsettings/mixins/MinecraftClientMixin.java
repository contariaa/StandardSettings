package com.kingcontaria.standardsettings.mixins;

import com.kingcontaria.standardsettings.StandardSettings;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.world.level.LevelInfo;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
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

public class MinecraftClientMixin {

    @Shadow
    @Nullable
    public Screen currentScreen;

    // initialize StandardSettings, doesn't use ClientModInitializer because GameOptions need to be initialized first
    @Inject(method = "initializeGame", at = @At("RETURN"))
    private void initializeStandardSettings(CallbackInfo ci) {
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

    private static boolean shouldResetSettings = true;

    @Inject(method = "startGame", at = @At("HEAD"))
    private void resetOptions(String fileName, String worldName, LevelInfo levelInfo, CallbackInfo ci) {
        if (!new File("saves", fileName).exists()) {
            // reset settings to standardoptions at the start of world creation
            // don't reset settings if the last world was reset on world preview
            if (shouldResetSettings) {
                // StandardSettings.checkSettings() calls window.method_18314() which resizes the screen
                // this also reinitializes the current screen, which is the TitleScreen, causing Atum to trigger another reset
                Screen screen = this.currentScreen;
                try {
                    this.currentScreen = null;

                    StandardSettings.changeOnWindowActivation = false;
                    StandardSettings.LOGGER.info("Reset to StandardSettings...");
                    StandardSettings.load();
                    StandardSettings.LOGGER.info("Checking Settings...");
                    StandardSettings.checkSettings();
                    StandardSettings.options.save();
                    shouldResetSettings = false;
                } finally {
                    this.currentScreen = screen;
                }
            }
        } else {
            StandardSettings.optionsCache.load(fileName);
        }

        // save the world file name of the last world
        StandardSettings.lastWorld = fileName;
    }

    // activate OnWorldJoin options when finishing world creation
    // if instance is unfocused, it will instead wait
    @Inject(method = "startGame", at = @At("RETURN"))
    private void onWorldJoin(CallbackInfo ci) {
        if (StandardSettings.client.isWindowFocused()) {
            StandardSettings.changeSettingsOnJoin();
        } else {
            StandardSettings.changeOnWindowActivation = true;
        }
        shouldResetSettings = true;
    }

    // activate OnWorldJoin options when focusing the instance
    @Inject(method = "method_18228", at = @At("HEAD"))
    private void changeSettingsOnJoin(CallbackInfo ci) {
        if (StandardSettings.changeOnWindowActivation && StandardSettings.client.isWindowFocused()) {
            StandardSettings.changeOnWindowActivation = false;
            StandardSettings.changeSettingsOnJoin();
        }
    }

}
