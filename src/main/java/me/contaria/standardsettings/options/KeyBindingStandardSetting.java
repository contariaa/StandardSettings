package me.contaria.standardsettings.options;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import me.contaria.speedrunapi.config.api.gui.CallbackButtonWidget;
import me.contaria.standardsettings.StandardSettings;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class KeyBindingStandardSetting extends StandardSetting<Integer> {
    private final KeyBinding keyBinding;
    private int value;

    public KeyBindingStandardSetting(String id, @Nullable String category, KeyBinding keyBinding) {
        super(id, category, null);
        this.keyBinding = keyBinding;

        this.set(this.getOption());
    }

    @Override
    public Integer get() {
        return this.value;
    }

    @Override
    public void set(Integer value) {
        this.value = value;
    }

    @Override
    protected void set(GameOptions options, Integer value) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected Integer get(GameOptions options) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Integer getOption() {
        return this.keyBinding.getCode();
    }

    @Override
    public void setOption(Integer value) {
        this.keyBinding.setCode(value);
    }

    @Override
    protected void valueFromJson(JsonElement jsonElement) {
        this.set(jsonElement.getAsInt());
    }

    @Override
    protected JsonElement valueToJson() {
        return new JsonPrimitive(this.get());
    }

    @Override
    public @NotNull String getName() {
        return I18n.translate(this.keyBinding.getTranslationKey());
    }

    @Override
    public @NotNull String getDisplayText() {
        String text = GameOptions.getFormattedNameForKeyCode(this.get());
        if (StandardSettings.config.isFocusedKeyBinding(this)) {
            return Formatting.WHITE + "> " + Formatting.YELLOW + text + Formatting.WHITE + " <";
        } else {
            for (StandardSetting<?> setting : StandardSettings.config.standardSettings) {
                if (setting != this && setting instanceof KeyBindingStandardSetting && setting.isEnabled() && this.get() != 0 && this.get() == ((KeyBindingStandardSetting) setting).value) {
                    return Formatting.RED + text;
                }
            }
        }
        return text;
    }

    @Override
    public @NotNull ButtonWidget createMainWidget() {
        return new CallbackButtonWidget(120, 20, this.getText(), button -> StandardSettings.config.setFocusedKeyBinding(this)) {
            @Override
            public void render(MinecraftClient client, int mouseX, int mouseY) {
                this.message = KeyBindingStandardSetting.this.getText();
                super.render(client, mouseX, mouseY);
            }
        };
    }
}
