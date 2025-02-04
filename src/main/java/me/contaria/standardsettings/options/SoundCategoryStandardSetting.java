package me.contaria.standardsettings.options;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import me.contaria.speedrunapi.util.TextUtil;
import me.contaria.standardsettings.StandardGameOptions;
import net.minecraft.client.gui.screen.ScreenTexts;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.OptionSliderWidget;
import net.minecraft.client.option.GameOptions;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
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
    public @NotNull Text getName() {
        return TextUtil.translatable("soundCategory." + this.soundCategory.getName());
    }

    @Override
    public @NotNull Text getDisplayText() {
        // see SoundSliderWidget#updateMessage
        float value = this.get();
        if (value == 0) {
            return ScreenTexts.OFF;
        }
        return TextUtil.literal((int) (value * 100.0) + "%");
    }

    @Override
    public @NotNull ClickableWidget createMainWidget() {
        // see SoundSliderWidget
        return new OptionSliderWidget(this.options, 0, 0, 120, 20, this.get()) {
            {
                this.updateMessage();
            }

            @Override
            protected void updateMessage() {
                this.setMessage(SoundCategoryStandardSetting.this.getText());
            }

            @Override
            protected void applyValue() {
                SoundCategoryStandardSetting.this.set((float) this.value);
            }
        };
    }
}
