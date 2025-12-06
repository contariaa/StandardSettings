package me.contaria.standardsettings.options;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import me.contaria.standardsettings.StandardGameOptions;
import me.contaria.standardsettings.gui.StandardOptionSliderWidget;
import me.contaria.standardsettings.mixin.accessors.ButtonWidgetAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.OptionSliderWidget;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.resource.language.I18n;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FloatOptionStandardSetting extends StandardSetting<Float> {
    private final GameOptions.Option option;

    public FloatOptionStandardSetting(String id, @Nullable String category, StandardGameOptions options, GameOptions.Option option) {
        super(id, category, options);
        this.option = option;

        this.set(this.getOption());
    }

    @Override
    public Float get(GameOptions options) {
        return options.getIntValue(this.option);
    }

    @Override
    public void set(GameOptions options, Float value) {
        options.setValue(this.option, this.option.adjust(value));
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
        return I18n.translate(this.option.getName());
    }

    @Override
    public @NotNull String getDisplayText() {
        return StandardSetting.getStringWithoutPrefix(this.options.getValueMessage(this.option), this.getName() + ": ");
    }

    @Override
    public @NotNull ButtonWidget createMainWidget() {
        StandardOptionSliderWidget widget = new StandardOptionSliderWidget(this.option.getOrdinal(), 0, 0, this.option, this, this.options) {
            @Override
            public void render(MinecraftClient client, int mouseX, int mouseY) {
                this.message = FloatOptionStandardSetting.this.getText();
                super.render(client, mouseX, mouseY);
            }
        };
        ((ButtonWidgetAccessor) widget).standardsettings$setWidth(120);
        return widget;
    }
}
