package me.contaria.standardsettings.gui;

import me.contaria.standardsettings.options.StandardSetting;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.ParentElement;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.screen.ScreenTexts;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class StandardOptionWidget extends ClickableWidget implements ParentElement {
    private final StandardSetting<?> setting;
    private final ClickableWidget mainWidget;
    private final ClickableWidget toggle;
    private Element focused;
    private boolean isDragging;

    public StandardOptionWidget(StandardSetting<?> setting, ClickableWidget mainWidget) {
        super(mainWidget.getX(), mainWidget.getY(), mainWidget.getWidth() + 30, mainWidget.getHeight(), mainWidget.getMessage());

        this.setting = setting;
        this.mainWidget = mainWidget;
        this.toggle = ButtonWidget.builder(ScreenTexts.onOrOff(setting.isEnabled()), button -> {
            boolean enabled = this.setting.toggleEnabled();
            button.setMessage(ScreenTexts.onOrOff(enabled));
            this.setEnabled(enabled);
        }).dimensions(mainWidget.getWidth() + 5, 0, 25, 20).build();
        this.setEnabled(setting.isEnabled());
    }

    private void setEnabled(boolean enabled) {
        this.mainWidget.active = enabled;
        if (this.mainWidget instanceof TextFieldWidget) {
            ((TextFieldWidget) this.mainWidget).setEditable(enabled);
            ((TextFieldWidget) this.mainWidget).setFocusUnlocked(enabled);
        }
        this.mainWidget.setMessage(this.setting.getText());
    }

    @Override
    public void renderButton(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        this.mainWidget.setPosition(this.getX(), this.getY());
        this.mainWidget.render(matrices, mouseX, mouseY, delta);
        this.toggle.setPosition(this.getX() + this.mainWidget.getWidth() + 5, this.getY());
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

    @Override
    public void appendClickableNarrations(NarrationMessageBuilder builder) {
    }
}
