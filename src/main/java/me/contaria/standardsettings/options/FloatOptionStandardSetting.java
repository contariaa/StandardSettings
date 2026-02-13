package me.contaria.standardsettings.options;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import me.contaria.standardsettings.StandardGameOptions;
import me.contaria.standardsettings.gui.StandardOptionSliderWidget;
import me.contaria.standardsettings.mixin.accessors.ButtonWidgetAccessor;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.option.GameOption;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.resource.language.I18n;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FloatOptionStandardSetting extends StandardSetting<Float> {
    protected final GameOption option;

    public FloatOptionStandardSetting(String id, @Nullable String category, StandardGameOptions options, GameOption option) {
        super(id, category, options);
        this.option = option;

        this.set(this.getOption());
    }

    @Override
    public Float get(GameOptions options) {
        return options.getFLoatOption(this.option);
    }

    @Override
    public void set(GameOptions options, Float value) {
        options.setOption(this.option, this.option.method_6662(value));
    }

    @Override
    protected void valueFromJson(JsonElement jsonElement) {
        this.set(jsonElement.getAsFloat());
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
        StandardOptionSliderWidget widget = new StandardOptionSliderWidget(this.option.method_882(), 0, 0, this.option, this, this.options);
        ((ButtonWidgetAccessor) widget).standardsettings$setWidth(120);
        return widget;
    }
}
