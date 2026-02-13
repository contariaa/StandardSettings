package me.contaria.standardsettings.options;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import me.contaria.speedrunapi.config.api.gui.CallbackButtonWidget;
import me.contaria.standardsettings.StandardGameOptions;
import me.contaria.standardsettings.StandardSettings;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.option.GameOption;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.resource.language.I18n;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.ToIntFunction;

public class CyclingOptionStandardSetting extends StandardSetting<Integer> {
    private final GameOption option;
    private final ToIntFunction<GameOptions> optionGetter;

    public CyclingOptionStandardSetting(String id, @Nullable String category, StandardGameOptions options, GameOption option, ToIntFunction<GameOptions> optionGetter) {
        super(id, category, options);
        this.option = option;
        this.optionGetter = optionGetter;

        this.set(this.getOption());
    }

    @Override
    public Integer get(GameOptions options) {
        return this.optionGetter.applyAsInt(options);
    }

    @Override
    public void set(GameOptions options, Integer value) {
        int original = this.get(options);
        int current = original;
        while (current != value) {
            options.setOption(this.option, 1);
            current = this.get(options);

            if (current == original) {
                StandardSettings.LOGGER.warn("Failed to set {} to {}.", this.getID(), value);
                break;
            }
        }
    }

    @Override
    protected void valueFromJson(JsonElement jsonElement) {
        this.set(jsonElement.getAsInt());
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
