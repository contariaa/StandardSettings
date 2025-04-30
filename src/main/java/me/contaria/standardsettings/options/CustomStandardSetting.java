package me.contaria.standardsettings.options;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonPrimitive;
import me.contaria.speedrunapi.util.TextUtil;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class CustomStandardSetting<T> extends StandardSetting<T> {
    private final Supplier<T> getter;
    private final Consumer<T> setter;
    private final Function<T, T> validator;
    private final Function<JsonElement, T> fromJson;
    private final Function<T, JsonElement> toJson;
    private final Function<T, Text> textGetter;
    private final Function<CustomStandardSetting<T>, ClickableWidget> mainWidgetCreator;
    private T value;

    public CustomStandardSetting(String id, @Nullable String category, Supplier<T> getter, Consumer<T> setter, Function<T, T> validator, Function<JsonElement, T> fromJson, Function<T, JsonElement> toJson, Function<T, Text> textGetter, Function<CustomStandardSetting<T>, ClickableWidget> mainWidgetCreator) {
        super(id, category);
        this.getter = getter;
        this.setter = setter;
        this.validator = validator;
        this.toJson = toJson;
        this.fromJson = fromJson;
        this.textGetter = textGetter;
        this.mainWidgetCreator = mainWidgetCreator;

        this.set(this.getVanilla());
    }

    @Override
    public T get() {
        return this.value;
    }

    @Override
    public void set(T value) {
        this.value = this.validator.apply(value);
    }

    @Override
    public T getVanilla() {
        return this.getter.get();
    }

    @Override
    public void setVanilla(T value) {
        this.setter.accept(value);
    }

    @Override
    protected void valueFromJson(JsonElement jsonElement) {
        this.set(this.fromJson.apply(jsonElement));
    }

    @Override
    protected JsonElement valueToJson() {
        return this.toJson.apply(this.get());
    }

    @Override
    @NotNull
    public Text getDisplayText() {
        return this.textGetter.apply(this.get());
    }

    @Override
    public @NotNull ClickableWidget createMainWidget() {
        return this.mainWidgetCreator.apply(this);
    }

    public static CustomStandardSetting<Boolean> ofBoolean(String id, String category, Supplier<Boolean> getter, Consumer<Boolean> setter) {
        return new CustomStandardSetting<>(
                id, category, getter, setter, value -> value,
                JsonElement::getAsBoolean, JsonPrimitive::new,
                ScreenTexts::onOrOff,
                setting -> CyclingButtonWidget.onOffBuilder(setting.get())
                        .omitKeyText()
                        .build(0, 0, 120, 20, setting.getName(), (button, value) -> setting.set(value))
        );
    }

    public static CustomStandardSetting<String> ofString(String id, String category, Supplier<String> getter, Consumer<String> setter, Function<String, Text> textGetter, Function<CustomStandardSetting<String>, ClickableWidget> mainWidgetCreator) {
        return ofString(id, category, getter, setter, value -> value, textGetter, mainWidgetCreator);
    }

    public static CustomStandardSetting<String> ofString(String id, String category, Supplier<String> getter, Consumer<String> setter, Function<String, String> validator, Function<String, Text> textGetter, Function<CustomStandardSetting<String>, ClickableWidget> mainWidgetCreator) {
        return new CustomStandardSetting<>(
                id, category, getter, setter, validator,
                json -> json.isJsonNull() ? null : json.getAsString(), string -> string == null ? JsonNull.INSTANCE : new JsonPrimitive(string),
                textGetter, mainWidgetCreator
        );
    }

    public static <T extends Enum<T>> CustomStandardSetting<T> ofEnum(String id, String category, Supplier<T> getter, Consumer<T> setter, Class<T> type) {
        Function<T, Text> textGetter = value -> TextUtil.translatable("speedrunapi.config.standardsettings.option.perspective." + value.name());
        return new CustomStandardSetting<>(
                id, category, getter, setter, value -> value,
                json -> {
                    String jsonString = json.getAsString();
                    for (T enumConstant : type.getEnumConstants()) {
                        if (enumConstant.name().equals(jsonString)) {
                            return enumConstant;
                        }
                    }
                    throw new IllegalArgumentException("Failed to parse enum value!");
                }, value -> new JsonPrimitive(value.name()),
                textGetter,
                setting -> CyclingButtonWidget.builder(textGetter)
                        .values(type.getEnumConstants())
                        .initially(setting.get())
                        .omitKeyText()
                        .build(0, 0, 120, 20, setting.getName(), (button, value) -> setting.set(value))
        );
    }
}
