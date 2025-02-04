package me.contaria.standardsettings.options;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import me.contaria.standardsettings.StandardGameOptions;
import me.contaria.standardsettings.mixin.accessors.CyclingButtonWidget$BuilderAccessor;
import me.contaria.standardsettings.mixin.accessors.CyclingOptionAccessor;
import me.contaria.standardsettings.mixin.accessors.OptionAccessor;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.option.CyclingOption;
import net.minecraft.client.option.GameOptions;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class EnumOptionStandardSetting<T extends Enum<T>> extends StandardSetting<T> {
    private final CyclingOption<T> option;
    private final Class<T> enumClass;

    public EnumOptionStandardSetting(String id, @Nullable String category, StandardGameOptions options, CyclingOption<T> option, Class<T> enumClass) {
        super(id, category, options);
        this.option = option;
        this.enumClass = enumClass;

        this.set(this.getOption());
    }

    @Override
    protected T get(GameOptions options) {
        return (T) ((CyclingOptionAccessor) this.option).standardsettings$getGetter().apply(options);
    }

    @Override
    protected void set(GameOptions options, T value) {
        ((CyclingOptionAccessor) this.option).standardsettings$getSetter().accept(options, this.option, value);
    }

    @Override
    protected void valueFromJson(JsonElement jsonElement) {
        String jsonString = jsonElement.getAsString();
        for (T enumConstant : this.enumClass.getEnumConstants()) {
            if (enumConstant.name().equals(jsonString)) {
                this.set(enumConstant);
                break;
            }
        }
    }

    @Override
    protected JsonElement valueToJson() {
        return new JsonPrimitive(this.get().name());
    }

    @Override
    public @NotNull Text getName() {
        return ((OptionAccessor) this.option).standardsettings$getDisplayPrefix();
    }

    @Override
    public @NotNull Text getDisplayText() {
        return ((CyclingButtonWidget$BuilderAccessor) ((CyclingOptionAccessor) this.option).standardsettings$getButtonBuilderFactory().get()).standardsettings$getValueToText().apply(this.get());
    }

    @Override
    public @NotNull ClickableWidget createMainWidget() {
        return ((CyclingOptionAccessor) this.option).standardsettings$getButtonBuilderFactory().get()
                .omitKeyText()
                .initially(this.get())
                .build(0, 0, 120, 20, this.getName(), (button, value) -> this.set((T) value));
    }
}
