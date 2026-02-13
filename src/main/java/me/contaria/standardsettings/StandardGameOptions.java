package me.contaria.standardsettings;

import me.contaria.standardsettings.mixin.accessors.MinecraftClientAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.render.entity.EntityRenderDispatcher;

import java.io.File;

public class StandardGameOptions extends GameOptions {
    private boolean hitBoxes;
    private String pieDirectory;

    public StandardGameOptions(MinecraftClient client, File optionsFile) {
        super(client, optionsFile);
    }

    @Override
    public void load() {
    }

    @Override
    public void save() {
    }

    @Override
    public void onPlayerModelPartChange() {
    }

    public static boolean getHitBoxes(GameOptions options) {
        if (options instanceof StandardGameOptions) {
            return ((StandardGameOptions) options).hitBoxes;
        }
        return EntityRenderDispatcher.renderHitboxes;
    }

    public static void setHitBoxes(GameOptions options, boolean value) {
        if (options instanceof StandardGameOptions) {
            ((StandardGameOptions) options).hitBoxes = value;
        } else {
            EntityRenderDispatcher.renderHitboxes = value;
        }
    }

    public static String getPieDirectory(GameOptions options) {
        if (options instanceof StandardGameOptions) {
            return ((StandardGameOptions) options).pieDirectory;
        }
        return ((MinecraftClientAccessor) MinecraftClient.getInstance()).standardsettings$getOpenProfilerSection();
    }

    public static void setPieDirectory(GameOptions options, String value) {
        if (!value.startsWith("root")) {
            value = "root";
        }
        value = value.trim();
        if (options instanceof StandardGameOptions) {
            ((StandardGameOptions) options).pieDirectory = value;
        } else {
            ((MinecraftClientAccessor) MinecraftClient.getInstance()).standardsettings$setOpenProfilerSection(value);
        }
    }
}
