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

public class BooleanStandardSetting extends StandardSetting<Boolean> {
    private final Function<GameOptions, Boolean> getter;
    private final BiConsumer<GameOptions, Boolean> setter;

    public BooleanStandardSetting(String id, @Nullable String category, StandardGameOptions options, Function<GameOptions, Boolean> getter, BiConsumer<GameOptions, Boolean> setter) {
        super(id, category, options);
        this.getter = getter;
        this.setter = setter;

        this.set(this.getOption());
    }

    @Override
    public Boolean get(GameOptions options) {
        return this.getter.apply(options);
    }

    @Override
    public void set(GameOptions options, Boolean value) {
        this.setter.accept(options, value);
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
        return I18n.translate("standardsettings.options." + this.getID());
    }

    @Override
    protected @NotNull String getDisplayText() {
        return this.get() ? I18n.translate("options.on") : I18n.translate("options.off");
    }

    @Override
    public @NotNull ButtonWidget createMainWidget() {
        return new CallbackButtonWidget(120, 20, this.getText(), button -> {
            this.set(!this.get());
            button.message = this.getText();
        });
    }
}
