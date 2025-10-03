package me.contaria.standardsettings.options;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import me.contaria.speedrunapi.config.api.gui.CallbackButtonWidget;
import me.contaria.standardsettings.StandardGameOptions;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.resource.language.I18n;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiConsumer;
import java.util.function.Function;

public class CyclingStandardSetting extends StandardSetting<Integer> {
    private final Function<GameOptions, Integer> getter;
    private final BiConsumer<GameOptions, Integer> setter;

    public CyclingStandardSetting(String id, @Nullable String category, StandardGameOptions options, Function<GameOptions, Integer> getter, BiConsumer<GameOptions, Integer> setter) {
        super(id, category, options);
        this.getter = getter;
        this.setter = setter;

        this.set(this.getOption());
    }

    @Override
    public Integer get(GameOptions options) {
        return this.getter.apply(options);
    }

    @Override
    public void set(GameOptions options, Integer value) {
        this.setter.accept(options, value);
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
    protected @NotNull String getDisplayText() {
        return I18n.translate("standardsettings.options." + this.getID() + "." + this.get());
    }

    @Override
    public @NotNull ButtonWidget createMainWidget() {
        return new CallbackButtonWidget(120, 20, this.getText(), button -> {
            this.set(this.get() + 1);
            button.message = this.getText();
        });
    }
}
