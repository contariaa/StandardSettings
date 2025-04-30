package me.contaria.standardsettings.options;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import me.contaria.speedrunapi.util.TextUtil;
import me.contaria.standardsettings.interfaces.StandardSettingsSimpleOption;
import me.contaria.standardsettings.mixin.accessors.CyclingButtonWidgetAccessor;
import me.contaria.standardsettings.mixin.accessors.SimpleOptionAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.option.SimpleOption;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

public class SimpleOptionStandardSetting<T> extends StandardSetting<T> {
    protected final SimpleOption<T> option;
    protected final SimpleOption<T> copy;

    public SimpleOptionStandardSetting(String id, @Nullable String category, SimpleOption<T> option) {
        super(id, category);
        this.option = option;
        this.copy = this.copy(option);

        this.set(this.getVanilla());
    }

    protected @NotNull SimpleOption<T> copy(SimpleOption<T> option) {
        SimpleOption<T> copy = this.copySimpleOption(option);
        //noinspection unchecked
        SimpleOptionAccessor<T> copyAccessor = (SimpleOptionAccessor<T>) (Object) copy;

        Function<T, Text> textGetter = copyAccessor.standardsettings$getTextGetter();
        copyAccessor.standardsettings$setTextGetter(value -> {
            Text name = this.getName();
            Text text = textGetter.apply(value);

            String prefix = name.getString() + ": ";
            String string = text.getString();
            if (string.startsWith(prefix)) {
                return TextUtil.literal(string.substring(prefix.length()));
            }
            return text;
        });

        return copy;
    }

    protected @NotNull SimpleOption<T> copySimpleOption(SimpleOption<T> option) {
        //noinspection unchecked
        return ((StandardSettingsSimpleOption<T>) (Object) option).standardsettings$copy();
    }

    @Override
    public T get() {
        return this.get(this.copy);
    }

    @Override
    public void set(T value) {
        this.set(this.copy, value);
    }

    @Override
    public T getVanilla() {
        return this.get(this.option);
    }

    @Override
    public void setVanilla(T value) {
        this.set(this.option, value);
    }

    protected T get(SimpleOption<T> option) {
        return option.getValue();
    }

    protected void set(SimpleOption<T> option, T value) {
        option.setValue(value);
    }

    @Override
    protected void valueFromJson(JsonElement jsonElement) {
        this.copy.getCodec().parse(JsonOps.INSTANCE, jsonElement).result().ifPresent(this.copy::setValue);
    }

    @Override
    protected JsonElement valueToJson() {
        return this.copy.getCodec().encodeStart(JsonOps.INSTANCE, this.copy.getValue()).result().orElseThrow();
    }

    @Override
    public @NotNull Text getName() {
        return ((SimpleOptionAccessor<?>) (Object) this.copy).standardsettings$getText();
    }

    @Override
    public @NotNull Text getDisplayText() {
        return ((SimpleOptionAccessor<T>) (Object) this.copy).standardsettings$getTextGetter().apply(this.get());
    }

    @Override
    public @NotNull ClickableWidget createMainWidget() {
        ClickableWidget widget = this.copy.createWidget(MinecraftClient.getInstance().options, 0, 0, 120, this::set);
        if (widget instanceof CyclingButtonWidgetAccessor cyclingWidget) {
            cyclingWidget.standardsettings$setOptionTextOmitted(true);
        }
        return widget;
    }
}
