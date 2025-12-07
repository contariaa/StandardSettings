package me.contaria.standardsettings.gui;

import me.contaria.standardsettings.options.StandardSetting;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.ParentElement;
import net.minecraft.client.gui.widget.AbstractButtonWidget;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.resource.language.I18n;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class StandardOptionWidget extends AbstractButtonWidget implements ParentElement {
    private final AbstractButtonWidget mainWidget;
    private final AbstractButtonWidget toggle;
    private Element focused;
    private boolean isDragging;

    public StandardOptionWidget(StandardSetting<?> setting, AbstractButtonWidget mainWidget) {
        super(mainWidget.x, mainWidget.y, mainWidget.getWidth() + 30, 20, mainWidget.getMessage());

        this.mainWidget = mainWidget;
        this.toggle = new ButtonWidget(mainWidget.getWidth() + 5, 0, 25, 20, I18n.translate(setting.isEnabled() ? "options.on" : "options.off"), button -> {
            boolean enabled = setting.toggleEnabled();
            button.setMessage(I18n.translate(enabled ? "options.on" : "options.off"));
            this.mainWidget.setMessage(setting.getText());
            this.setEnabled(enabled);
        });
        this.setEnabled(setting.isEnabled());
    }

    private void setEnabled(boolean enabled) {
        this.mainWidget.active = enabled;
        if (this.mainWidget instanceof TextFieldWidget) {
            ((TextFieldWidget) this.mainWidget).setIsEditable(enabled);
            ((TextFieldWidget) this.mainWidget).method_1856(enabled);
        }
    }

    @Override
    public void renderButton(int mouseX, int mouseY, float delta) {
        this.mainWidget.x = this.x;
        this.mainWidget.y = this.y;
        this.mainWidget.render(mouseX, mouseY, delta);
        this.toggle.x = this.x + this.mainWidget.getWidth() + 5;
        this.toggle.y = this.y;
        this.toggle.render(mouseX, mouseY, delta);
    }

    @Override
    public List<? extends Element> children() {
        List<Element> children = new ArrayList<>();
        children.add(this.mainWidget);
        children.add(this.toggle);
        return children;
    }

    @Override
    public final boolean isDragging() {
        return this.isDragging;
    }

    @Override
    public final void setDragging(boolean dragging) {
        this.isDragging = dragging;
    }

    @Nullable
    @Override
    public Element getFocused() {
        return this.focused;
    }

    @Override
    public void setFocused(@Nullable Element focused) {
        this.focused = focused;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return ParentElement.super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return ParentElement.super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        return ParentElement.super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }
}
