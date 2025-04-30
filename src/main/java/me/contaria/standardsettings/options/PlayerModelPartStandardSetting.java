package me.contaria.standardsettings.options;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.render.entity.PlayerModelPart;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PlayerModelPartStandardSetting extends StandardSetting<Boolean> {
    public final PlayerModelPart playerModelPart;
    private boolean value;

    public PlayerModelPartStandardSetting(String id, @Nullable String category, PlayerModelPart playerModelPart) {
        super(id, category);
        this.playerModelPart = playerModelPart;

        this.set(this.getVanilla());
    }

    @Override
    public Boolean get() {
        return this.value;
    }

    @Override
    public void set(Boolean value) {
        this.value = value;
    }

    @Override
    public Boolean getVanilla() {
        return MinecraftClient.getInstance().options.isPlayerModelPartEnabled(this.playerModelPart);
    }

    @Override
    public void setVanilla(Boolean value) {
        MinecraftClient.getInstance().options.togglePlayerModelPart(this.playerModelPart, value);
    }

    @Override
    protected void valueFromJson(JsonElement jsonElement) {
        this.set(jsonElement.getAsBoolean());
    }

    @Override
    protected JsonElement valueToJson() {
        return new JsonPrimitive(this.get());
    }

    @Override
    public @NotNull Text getName() {
        return this.playerModelPart.getOptionName();
    }

    @Override
    public @NotNull Text getDisplayText() {
        return ScreenTexts.onOrOff(this.get());
    }

    @Override
    public @NotNull ClickableWidget createMainWidget() {
        return ButtonWidget.builder(this.getText(), button -> {
            this.set(!this.get());
            button.setMessage(this.getText());
        }).dimensions(0, 0, 120, 20).build();
    }
}
