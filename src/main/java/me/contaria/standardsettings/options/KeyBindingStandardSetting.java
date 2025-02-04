package me.contaria.standardsettings.options;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import me.contaria.speedrunapi.util.TextUtil;
import me.contaria.standardsettings.StandardSettings;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class KeyBindingStandardSetting extends StandardSetting<InputUtil.Key> {
    private final KeyBinding keyBinding;
    private InputUtil.Key value;

    public KeyBindingStandardSetting(String id, @Nullable String category, KeyBinding keyBinding) {
        super(id, category, null);
        this.keyBinding = keyBinding;

        this.set(this.getOption());
    }

    @Override
    public InputUtil.Key get() {
        return this.value;
    }

    @Override
    public void set(InputUtil.Key value) {
        this.value = value;
    }

    @Override
    protected void set(GameOptions options, InputUtil.Key value) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected InputUtil.Key get(GameOptions options) {
        throw new UnsupportedOperationException();
    }

    @Override
    public InputUtil.Key getOption() {
        return InputUtil.fromTranslationKey(this.keyBinding.getBoundKeyTranslationKey());
    }

    @Override
    public void setOption(InputUtil.Key value) {
        this.keyBinding.setBoundKey(value);
    }

    @Override
    protected void valueFromJson(JsonElement jsonElement) {
        this.set(InputUtil.fromTranslationKey(jsonElement.getAsString()));
    }

    @Override
    protected JsonElement valueToJson() {
        return new JsonPrimitive(this.get().getTranslationKey());
    }

    @Override
    public @NotNull Text getName() {
        return TextUtil.translatable(this.keyBinding.getTranslationKey());
    }

    @Override
    public @NotNull Text getDisplayText() {
        Text text = this.value.getLocalizedText();
        if (StandardSettings.config.isFocusedKeyBinding(this)) {
            return TextUtil.literal("> ").append(text).append(" <").formatted(Formatting.YELLOW);
        } else {
            for (StandardSetting<?> setting : StandardSettings.config.standardSettings) {
                if (setting != this && setting instanceof KeyBindingStandardSetting && setting.isEnabled() && this.value.equals(((KeyBindingStandardSetting) setting).value)) {
                    return text.shallowCopy().formatted(Formatting.RED);
                }
            }
        }
        return text;
    }

    @Override
    public @NotNull ClickableWidget createMainWidget() {
        return new ButtonWidget(0, 0, 120, 20, this.getText(), button -> StandardSettings.config.setFocusedKeyBinding(this)) {
            @Override
            public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
                this.setMessage(KeyBindingStandardSetting.this.getText());
                super.render(matrices, mouseX, mouseY, delta);
            }
        };
    }
}
