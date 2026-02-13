package me.contaria.standardsettings.gui;

import me.contaria.speedrunapi.config.api.gui.ButtonWidgetCallback;
import me.contaria.speedrunapi.config.api.gui.SpeedrunWidget;
import me.contaria.standardsettings.mixin.accessors.ButtonWidgetAccessor;
import me.contaria.standardsettings.mixin.accessors.TextFieldWidgetAccessor;
import me.contaria.standardsettings.options.StandardSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.resource.language.I18n;

public class StandardOptionWidget implements SpeedrunWidget {
    private final StandardSetting<?> setting;
    private final Object mainWidget;
    private final ButtonWidget toggle;

    private int x;
    private int y;

    public StandardOptionWidget(StandardSetting<?> setting, Object mainWidget) {
        this.setting = setting;
        this.mainWidget = mainWidget;
        this.toggle = new ButtonWidget(-1, 0, 0, 25, 20, I18n.translate(setting.isEnabled() ? "options.on" : "options.off"));
        this.setEnabled(setting.isEnabled());
    }

    private void setEnabled(boolean enabled) {
        if (this.mainWidget instanceof ButtonWidget) {
            ButtonWidget button = (ButtonWidget) this.mainWidget;
            button.active = enabled;
        } else {
            TextFieldWidget widget = (TextFieldWidget) this.mainWidget;
            widget.setEditable(enabled);
            widget.setFocusUnlocked(enabled);
        }
    }

    @Override
    public void render(int mouseX, int mouseY) {
        if (this.mainWidget instanceof ButtonWidget) {
            ButtonWidget button = (ButtonWidget) this.mainWidget;
            button.x = this.x;
            button.y = this.y;
            button.render(MinecraftClient.getInstance(), mouseX, mouseY);
        } else {
            TextFieldWidget widget = (TextFieldWidget) this.mainWidget;
            ((TextFieldWidgetAccessor) widget).standardsettings$setX(this.x);
            ((TextFieldWidgetAccessor) widget).standardsettings$setY(this.y);
            widget.render();
        }
        this.toggle.x = this.x + 120 + 5;
        this.toggle.y = this.y;
        this.toggle.render(MinecraftClient.getInstance(), mouseX, mouseY);
    }

    @Override
    public int getX() {
        return this.x;
    }

    @Override
    public void setX(int x) {
        this.x = x;
    }

    @Override
    public int getY() {
        return this.y;
    }

    @Override
    public void setY(int y) {
        this.y = y;
    }

    @Override
    public int getWidth() {
        return 150;
    }

    @Override
    public int getHeight() {
        return 20;
    }

    @Override
    public boolean mouseClicked(int mouseX, int mouseY, int button) {
        if (this.mainWidget instanceof TextFieldWidget) {
            TextFieldWidget widget = (TextFieldWidget) this.mainWidget;
            int x = ((TextFieldWidgetAccessor) widget).standardsettings$getX();
            int y = ((TextFieldWidgetAccessor) widget).standardsettings$getY();
            if (mouseX >= x && mouseX < x + 120 && mouseY >= y && mouseY < y + 20) {
                widget.mouseClicked(mouseX, mouseY, button);
                return true;
            }
        }
        if (this.mainWidget instanceof ButtonWidget) {
            ButtonWidget widget = (ButtonWidget) this.mainWidget;
            if (button == 0 && widget.isMouseOver(MinecraftClient.getInstance(), mouseX, mouseY)) {
                if (widget instanceof ButtonWidgetCallback) {
                    ((ButtonWidgetCallback) this.mainWidget).onPress();
                }
                widget.playDownSound(MinecraftClient.getInstance().getSoundManager());
                return true;
            }
        }
        if (button == 0 && this.toggle.isMouseOver(MinecraftClient.getInstance(), mouseX, mouseY)) {
            boolean enabled = this.setting.toggleEnabled();
            this.toggle.message = I18n.translate(enabled ? "options.on" : "options.off");
            if (this.mainWidget instanceof ButtonWidget) {
                ButtonWidget widget = ((ButtonWidget) this.mainWidget);
                widget.message = this.setting.getText();
                widget.playDownSound(MinecraftClient.getInstance().getSoundManager());
            }
            this.setEnabled(enabled);
            return true;
        }
        return false;
    }

    @Override
    public boolean keyPressed(char id, int code) {
        if (this.mainWidget instanceof TextFieldWidget) {
            TextFieldWidget widget = (TextFieldWidget) this.mainWidget;
            return widget.keyPressed(id, code);
        }
        return false;
    }

    @Override
    public boolean mouseDragged(int mouseX, int mouseY, int button, long mouseLastClicked) {
        if (button == 0 && this.mainWidget instanceof ButtonWidgetAccessor) {
            ButtonWidgetAccessor widget = (ButtonWidgetAccessor) this.mainWidget;
            widget.standardsettings$mouseDragged(MinecraftClient.getInstance(), mouseX, mouseY);
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(int mouseX, int mouseY, int button) {
        if (button == 0 && this.mainWidget instanceof ButtonWidget) {
            ButtonWidget widget = (ButtonWidget) this.mainWidget;
            widget.mouseReleased(mouseX, mouseY);
            return true;
        }
        return false;
    }
}
