package com.kingcontaria.standardsettings;

import me.voidxwalker.worldpreview.WorldPreview;

public class IfWPExists {
    public static boolean handleLevelLoadScreenRender() {
        if (StandardSettings.f3PauseOnWorldLoad && StandardSettings.hasWP) {
            if (WorldPreview.inPreview) {
                WorldPreview.showMenu = false;
                return true;
            } else {
                return false;
            }
        }
        return true;
    }
}
