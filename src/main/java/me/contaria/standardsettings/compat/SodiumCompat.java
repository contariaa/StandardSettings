package me.contaria.standardsettings.compat;

import net.caffeinemc.mods.sodium.client.SodiumClientMod;

public class SodiumCompat {

    public static void setEntityCulling(boolean value) {
        SodiumClientMod.options().performance.useEntityCulling = value;
    }

    public static boolean getEntityCulling() {
        return SodiumClientMod.options().performance.useEntityCulling;
    }
}
