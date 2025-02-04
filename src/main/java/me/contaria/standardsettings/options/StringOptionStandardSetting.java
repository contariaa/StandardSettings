package me.contaria.standardsettings.options;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonPrimitive;
import me.contaria.standardsettings.StandardGameOptions;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.option.GameOptions;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiConsumer;
import java.util.function.Function;

public class StringOptionStandardSetting extends StandardSetting<String> {
    private final Function<GameOptions, String> getter;
    private final BiConsumer<GameOptions, String> setter;
    private final Function<StringOptionStandardSetting, Text> getText;
    private final Function<StringOptionStandardSetting, ClickableWidget> createMainWidget;

    public StringOptionStandardSetting(String id, @Nullable String category, StandardGameOptions options, Function<GameOptions, String> getter, BiConsumer<GameOptions, String> setter, Function<StringOptionStandardSetting, Text> getText, Function<StringOptionStandardSetting, ClickableWidget> createMainWidget) {
        super(id, category, options);
        this.getter = getter;
        this.setter = setter;
        this.getText = getText;
        this.createMainWidget = createMainWidget;

        this.set(this.getOption());
    }

    @Override
    public String get(GameOptions options) {
        return this.getter.apply(options);
    }

    @Override
    public void set(GameOptions options, String value) {
        this.setter.accept(options, value);
    }

    @Override
    protected void valueFromJson(JsonElement jsonElement) {
        this.set(!jsonElement.isJsonNull() ? jsonElement.getAsString() : null);
    }

    @Override
    protected JsonElement valueToJson() {
        String value = this.get();
        return value != null ? new JsonPrimitive(value) : JsonNull.INSTANCE;
    }

    @Override
    protected @NotNull Text getDisplayText() {
        return this.getText.apply(this);
    }

    @Override
    public @NotNull ClickableWidget createMainWidget() {
        return this.createMainWidget.apply(this);
    }
}
