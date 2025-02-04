package me.contaria.standardsettings.options;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import me.contaria.speedrunapi.util.TextUtil;
import me.contaria.standardsettings.StandardGameOptions;
import me.contaria.standardsettings.mixin.accessors.BooleanOptionAccessor;
import me.contaria.standardsettings.mixin.accessors.OptionAccessor;
import net.minecraft.client.gui.widget.AbstractButtonWidget;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.options.BooleanOption;
import net.minecraft.client.options.GameOptions;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BooleanOptionStandardSetting extends StandardSetting<Boolean> {
    private final BooleanOption option;

    public BooleanOptionStandardSetting(String id, @Nullable String category, StandardGameOptions options, BooleanOption option) {
        super(id, category, options);
        this.option = option;

        this.set(this.getOption());
    }

    @Override
    protected Boolean get(GameOptions options) {
        return this.option.get(options);
    }

    @Override
    protected void set(GameOptions options, Boolean value) {
        ((BooleanOptionAccessor) this.option).standardsettings$set(options, value);
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
    public @NotNull Text getName() {
        return TextUtil.translatable(((OptionAccessor) this.option).standardsettings$getKey());
    }

    @Override
    public @NotNull Text getDisplayText() {
        return StandardSetting.getTextWithoutPrefix(this.option.getDisplayString(this.options), this.option.getDisplayPrefix());
    }

    @Override
    public @NotNull AbstractButtonWidget createMainWidget() {
        return new ButtonWidget(0, 0, 120, 20, this.getText(), button -> {
            this.set(!this.get());
            button.setMessage(this.getText());
        });
    }
}
