package me.contaria.standardsettings;

import me.contaria.standardsettings.compat.SodiumCompat;
import me.contaria.standardsettings.mixin.accessors.MinecraftClientAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.options.GameOptions;
import net.minecraft.client.util.VideoMode;
import net.minecraft.util.profiler.ProfileResult;

import java.io.File;

public class StandardGameOptions extends GameOptions {
    private boolean hitBoxes;
    private boolean chunkBorders;
    private String pieDirectory;
    private boolean sneaking;
    private boolean sprinting;
    private boolean entityCulling;

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
        return MinecraftClient.getInstance().getWindow().getVideoMode().map(VideoMode::asString).orElse(null);
    }

    public static void setFullscreenResolution(GameOptions options, String value) {
        if (options instanceof StandardGameOptions) {
            options.fullscreenResolution = value;
        } else {
            MinecraftClient.getInstance().getWindow().setVideoMode(VideoMode.fromString(value));
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

    public static String getPieDirectory(GameOptions options) {
        String pieDirectory;
        if (options instanceof StandardGameOptions) {
            pieDirectory = ((StandardGameOptions) options).pieDirectory;
        } else {
            pieDirectory = ((MinecraftClientAccessor) MinecraftClient.getInstance()).standardsettings$getOpenProfilerSection();
        }
        return ProfileResult.getHumanReadableName(pieDirectory);
    }

    public static void setPieDirectory(GameOptions options, String value) {
        if (!value.startsWith("root")) {
            value = "root";
        }
        value = value.replace('.', '\u001e').trim();
        if (options instanceof StandardGameOptions) {
            ((StandardGameOptions) options).pieDirectory = value;
        } else {
            ((MinecraftClientAccessor) MinecraftClient.getInstance()).standardsettings$setOpenProfilerSection(value);
        }
    }

    public static boolean getSneaking(GameOptions options) {
        if (options instanceof StandardGameOptions) {
            return ((StandardGameOptions) options).sneaking;
        }
        return options.keySneak.isPressed();
    }

    public static void setSneaking(GameOptions options, boolean value) {
        if (options instanceof StandardGameOptions) {
            ((StandardGameOptions) options).sneaking = value;
        } else if (options.sneakToggled && options.keySneak.isPressed() != value) {
            options.keySneak.setPressed(true);
        }
    }

    public static boolean getSprinting(GameOptions options) {
        if (options instanceof StandardGameOptions) {
            return ((StandardGameOptions) options).sprinting;
        }
        return options.keySprint.isPressed();
    }

    public static void setSprinting(GameOptions options, boolean value) {
        if (options instanceof StandardGameOptions) {
            ((StandardGameOptions) options).sprinting = value;
        } else if (options.sprintToggled && options.keySprint.isPressed() != value) {
            options.keySprint.setPressed(true);
        }
    }

    public static boolean getEntityCulling(GameOptions options) {
        if (options instanceof StandardGameOptions) {
            return ((StandardGameOptions) options).entityCulling;
        } else if (StandardSettings.HAS_SODIUM) {
            return SodiumCompat.getEntityCulling();
        }
        return false;
    }

    public static void setEntityCulling(GameOptions options, boolean value) {
        if (options instanceof StandardGameOptions) {
            ((StandardGameOptions) options).entityCulling = value;
        } else if (StandardSettings.HAS_SODIUM) {
            SodiumCompat.setEntityCulling(value);
        }
    }
}
