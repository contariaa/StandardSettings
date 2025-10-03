package me.contaria.standardsettings.options;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import me.contaria.speedrunapi.config.api.gui.CallbackButtonWidget;
import me.contaria.standardsettings.StandardGameOptions;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.render.entity.PlayerModelPart;
import net.minecraft.client.resource.language.I18n;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PlayerModelPartStandardSetting extends StandardSetting<Boolean> {
    public final PlayerModelPart playerModelPart;

    public PlayerModelPartStandardSetting(String id, @Nullable String category, StandardGameOptions options, PlayerModelPart playerModelPart) {
        super(id, category, options);
        this.playerModelPart = playerModelPart;

        this.set(this.getOption());
    }

    @Override
    public Boolean get(GameOptions options) {
        return options.getEnabledPlayerModelParts().contains(this.playerModelPart);
    }

    @Override
    public void set(GameOptions options, Boolean value) {
        options.setPlayerModelPart(this.playerModelPart, value);
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
    public @NotNull String getName() {
        return this.playerModelPart.getOptionName().asFormattedString();
    }

    @Override
    public @NotNull String getDisplayText() {
        return I18n.translate(this.get() ? "options.on" : "options.off");
    }

    @Override
    public @NotNull ButtonWidget createMainWidget() {
        return new CallbackButtonWidget(120, 20, this.getText(), button -> {
            this.options.togglePlayerModelPart(this.playerModelPart);
            button.message = this.getText();
        });
    }
}
