package me.contaria.standardsettings.options;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import me.contaria.speedrunapi.config.api.gui.CallbackButtonWidget;
import me.contaria.standardsettings.StandardGameOptions;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.option.GameOption;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.resource.language.I18n;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BooleanOptionStandardSetting extends StandardSetting<Boolean> {
    private final GameOption option;

    public BooleanOptionStandardSetting(String id, @Nullable String category, StandardGameOptions options, GameOption option) {
        super(id, category, options);
        this.option = option;

        this.set(this.getOption());
    }

    @Override
    protected Boolean get(GameOptions options) {
        return options.gteIntOption(this.option);
    }

    @Override
    protected void set(GameOptions options, Boolean value) {
        if (value != this.get(options)) {
            options.setOption(this.option, 1);
        }
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
        return I18n.translate(this.option.getTranslationKey());
    }

    @Override
    public @NotNull String getDisplayText() {
        return StandardSetting.getStringWithoutPrefix(this.options.getStringOption(this.option), this.getName() + ": ");
    }

    @Override
    public @NotNull ButtonWidget createMainWidget() {
        return new CallbackButtonWidget(120, 20, this.getText(), button -> {
            this.options.setOption(this.option, 1);
            button.message = this.getText();
        });
    }
}
