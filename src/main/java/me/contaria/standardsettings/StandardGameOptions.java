package me.contaria.standardsettings;

import me.contaria.standardsettings.mixin.accessors.WindowAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.options.GameOptions;
import net.minecraft.client.util.VideoMode;

import java.io.File;

public class StandardGameOptions extends GameOptions {
    private boolean hitBoxes;
    private boolean chunkBorders;

    public StandardGameOptions(MinecraftClient client, File optionsFile) {
        super(client, optionsFile);
    }

    @Override
    public void load() {
    }

    @Override
    public void write() {
    }

    @Override
    public void onPlayerModelPartChange() {
    }

    public static String getFullscreenResolution(GameOptions options) {
        if (options instanceof StandardGameOptions) {
            return options.fullscreenResolution;
        }
        return MinecraftClient.getInstance().window.getVideoMode().map(VideoMode::asString).orElse(null);
    }

    public static void setFullscreenResolution(GameOptions options, String value) {
        if (options instanceof StandardGameOptions) {
            options.fullscreenResolution = value;
        } else {
            MinecraftClient.getInstance().window.method_4505(VideoMode.fromString(value).map(StandardSettings::findClosestVideoModeIndex).orElse(-1) + 1);
        }
    }

    public static boolean getHitBoxes(GameOptions options) {
        if (options instanceof StandardGameOptions) {
            return ((StandardGameOptions) options).hitBoxes;
        }
        return MinecraftClient.getInstance().getEntityRenderManager().shouldRenderHitboxes();
    }

    public static void setHitBoxes(GameOptions options, boolean value) {
        if (options instanceof StandardGameOptions) {
            ((StandardGameOptions) options).hitBoxes = value;
        } else {
            MinecraftClient.getInstance().getEntityRenderManager().setRenderHitboxes(value);
        }
    }

    public static boolean getChunkBorders(GameOptions options) {
        if (options instanceof StandardGameOptions) {
            return ((StandardGameOptions) options).chunkBorders;
        }
        MinecraftClient.getInstance().debugRenderer.toggleShowChunkBorder();
        return MinecraftClient.getInstance().debugRenderer.toggleShowChunkBorder();
    }

    public static void setChunkBorders(GameOptions options, boolean value) {
        if (options instanceof StandardGameOptions) {
            ((StandardGameOptions) options).chunkBorders = value;
        } else if (MinecraftClient.getInstance().debugRenderer.toggleShowChunkBorder() != value) {
            MinecraftClient.getInstance().debugRenderer.toggleShowChunkBorder();
        }
    }
}
