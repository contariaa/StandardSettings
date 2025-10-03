package me.contaria.standardsettings.options;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import me.contaria.standardsettings.StandardGameOptions;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.PagedEntryListWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.client.sound.SoundCategory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SoundCategoryStandardSetting extends StandardSetting<Float> {
    private final SoundCategory soundCategory;

    public SoundCategoryStandardSetting(String id, @Nullable String category, StandardGameOptions options, SoundCategory soundCategory) {
        super(id, category, options);
        this.soundCategory = soundCategory;

        this.set(this.getOption());
    }

    @Override
    public Float get(GameOptions options) {
        return options.getSoundVolume(this.soundCategory);
    }

    @Override
    public void set(GameOptions options, Float value) {
        options.setSoundVolume(this.soundCategory, value);
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
        return I18n.translate("soundCategory." + this.soundCategory.getName());
    }

    @Override
    public @NotNull String getDisplayText() {
        // see SoundSliderWidget#updateMessage
        float value = this.get();
        if (value == 0) {
            return I18n.translate("options.off");
        }
        return (int) (value * 100.0) + "%";
    }

    @Override
    public @NotNull ButtonWidget createMainWidget() {
        // see SoundSliderWidget
        SliderWidget widget = new SliderWidget(new PagedEntryListWidget.Listener() {
            @Override
            public void setBooleanValue(int id, boolean value) {
            }

            @Override
            public void setFloatValue(int id, float value) {
                SoundCategoryStandardSetting.this.set(value);
            }

            @Override
            public void setStringValue(int id, String text) {
            }
        }, -1, 0, 0, "soundCategory." + this.soundCategory.getName(), 0.0f, 1.0f, this.get(), (id, label, value) -> {
            if (value == 0.0f) {
                return I18n.translate("options.off");
            }
            return (int) (value * 100.0f) + "%";
        });
        widget.setWidth(120);
        return widget;
    }
}
