package me.contaria.standardsettings.gui;

import me.contaria.standardsettings.options.SoundCategoryStandardSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.widget.ButtonWidget;
import org.lwjgl.opengl.GL11;

public class StandardSoundSliderWidget extends ButtonWidget {
    private final SoundCategoryStandardSetting setting;

    private float progress;
    private boolean focused;

    public StandardSoundSliderWidget(SoundCategoryStandardSetting setting) {
        super(-1, 0, 0, 120, 20, setting.getText());
        this.setting = setting;
        this.progress = setting.get();
    }

    @Override
    public int getYImage(boolean isHovered) {
        return 0;
    }

    @Override
    protected void mouseDragged(MinecraftClient client, int mouseX, int mouseY) {
        if (this.visible) {
            if (this.focused) {
                this.progress = Math.max(0.0f, Math.min(1.0f, (mouseX - (this.x + 4.0f)) / (this.width - 8.0f)));
                this.updateValue();
            }

            GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
            this.drawTexture(this.x + (int) (this.progress * (this.width - 8)), this.y, 0, 66, 4, 20);
            this.drawTexture(this.x + (int) (this.progress * (this.width - 8)) + 4, this.y, 196, 66, 4, 20);
        }
    }

    @Override
    public boolean isMouseOver(MinecraftClient client, int mouseX, int mouseY) {
        if (super.isMouseOver(client, mouseX, mouseY)) {
            this.progress = Math.max(0.0f, Math.min(1.0f, (mouseX - (this.x + 4.0f)) / (this.width - 8.0f)));
            this.updateValue();
            this.focused = true;
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY) {
        this.focused = false;
    }

    private void updateValue() {
        this.setting.set(this.progress);
        this.message = this.setting.getText();
    }
}
