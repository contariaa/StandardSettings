package me.contaria.standardsettings.gui;

import me.contaria.standardsettings.options.StandardSetting;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.ParentElement;
import net.minecraft.client.gui.screen.ScreenTexts;
import net.minecraft.client.gui.widget.AbstractButtonWidget;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.math.MatrixStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class StandardOptionWidget extends AbstractButtonWidget implements ParentElement {
    private AbstractButtonWidget mainWidget;
    private final AbstractButtonWidget toggle;
    private Element focused;
    private boolean isDragging;

    public StandardOptionWidget(StandardSetting<?> setting, AbstractButtonWidget mainWidget) {
        super(mainWidget.x, mainWidget.y, mainWidget.getWidth() + 30, mainWidget.getHeight(), mainWidget.getMessage());

        this.mainWidget = mainWidget;
        this.toggle = new ButtonWidget(mainWidget.getWidth() + 5, 0, 25, 20, ScreenTexts.getToggleText(setting.isEnabled()), button -> {
            boolean enabled = setting.toggleEnabled();
            button.setMessage(ScreenTexts.getToggleText(enabled));
            this.mainWidget.setMessage(setting.getText());
            this.setEnabled(enabled);
        });
        this.setEnabled(setting.isEnabled());
    }

    private void setEnabled(boolean enabled) {
        this.mainWidget.active = enabled;
        if (this.mainWidget instanceof TextFieldWidget) {
            ((TextFieldWidget) this.mainWidget).setEditable(enabled);
            ((TextFieldWidget) this.mainWidget).setFocusUnlocked(enabled);
        }
    }

    protected void setMainWidget(AbstractButtonWidget widget) {
        this.mainWidget = widget;
    }

    @Override
    public void renderButton(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        this.mainWidget.x = this.x;
        this.mainWidget.y = this.y;
        this.mainWidget.render(matrices, mouseX, mouseY, delta);
        this.toggle.x = this.x + this.mainWidget.getWidth() + 5;
        this.toggle.y = this.y;
        this.toggle.render(matrices, mouseX, mouseY, delta);
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
