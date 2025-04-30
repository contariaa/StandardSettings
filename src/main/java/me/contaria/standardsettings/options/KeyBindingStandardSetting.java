package me.contaria.standardsettings.options;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import me.contaria.speedrunapi.util.TextUtil;
import me.contaria.standardsettings.StandardSettings;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

public class KeyBindingStandardSetting extends StandardSetting<InputUtil.Key> {
    private final KeyBinding keyBinding;
    private InputUtil.Key value;

    public KeyBindingStandardSetting(String id, @Nullable String category, KeyBinding keyBinding) {
        super(id, category);
        this.keyBinding = keyBinding;

        this.set(this.getVanilla());
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
    public InputUtil.Key getVanilla() {
        return InputUtil.fromTranslationKey(this.keyBinding.getBoundKeyTranslationKey());
    }

    @Override
    public void setVanilla(InputUtil.Key value) {
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
            return TextUtil.literal("> ").append(text.copy().formatted(Formatting.WHITE, Formatting.UNDERLINE)).append(" <").formatted(Formatting.YELLOW);
        }
        if (this.value != InputUtil.UNKNOWN_KEY) {
            for (StandardSetting<?> setting : StandardSettings.config.standardSettings) {
                if (setting != this && setting instanceof KeyBindingStandardSetting && setting.isEnabled() && this.value.equals(((KeyBindingStandardSetting) setting).value)) {
                    return TextUtil.literal("[ ").append(text.copy().formatted(Formatting.WHITE)).append(" ]").formatted(Formatting.RED);
                }
            }
        }
        return text;
    }

    @Override
    public @NotNull ClickableWidget createMainWidget() {
        return new ButtonWidget(0, 0, 120, 20, this.getText(), button -> StandardSettings.config.setFocusedKeyBinding(this), Supplier::get) {
            @Override
            public void render(DrawContext context, int mouseX, int mouseY, float delta) {
                this.setMessage(KeyBindingStandardSetting.this.getText());
                super.render(context, mouseX, mouseY, delta);
            }
        };
    }
}
