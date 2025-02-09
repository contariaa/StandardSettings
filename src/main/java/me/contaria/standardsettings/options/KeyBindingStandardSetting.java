package me.contaria.standardsettings.options;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import me.contaria.standardsettings.StandardSettings;
import net.minecraft.client.gui.widget.AbstractButtonWidget;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.options.GameOptions;
import net.minecraft.client.options.KeyBinding;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class KeyBindingStandardSetting extends StandardSetting<InputUtil.KeyCode> {
    private final KeyBinding keyBinding;
    private InputUtil.KeyCode value;

    public KeyBindingStandardSetting(String id, @Nullable String category, KeyBinding keyBinding) {
        super(id, category, null);
        this.keyBinding = keyBinding;

        this.set(this.getOption());
    }

    @Override
    public InputUtil.KeyCode get() {
        return this.value;
    }

    @Override
    public void set(InputUtil.KeyCode value) {
        this.value = value;
    }

    @Override
    protected void set(GameOptions options, InputUtil.KeyCode value) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected InputUtil.KeyCode get(GameOptions options) {
        throw new UnsupportedOperationException();
    }

    @Override
    public InputUtil.KeyCode getOption() {
        return InputUtil.fromName(this.keyBinding.getName());
    }

    @Override
    public void setOption(InputUtil.KeyCode value) {
        this.keyBinding.setKeyCode(value);
    }

    @Override
    protected void valueFromJson(JsonElement jsonElement) {
        this.set(InputUtil.fromName(jsonElement.getAsString()));
    }

    @Override
    protected JsonElement valueToJson() {
        return new JsonPrimitive(this.get().getName());
    }

    @Override
    public @NotNull String getName() {
        return I18n.translate(this.keyBinding.getId());
    }

    @Override
    public @NotNull String getDisplayText() {
        String text = this.getLocalizedName(this.value);
        if (StandardSettings.config.isFocusedKeyBinding(this)) {
            return Formatting.WHITE + "> " + Formatting.YELLOW + text + Formatting.WHITE + " <";
        } else {
            for (StandardSetting<?> setting : StandardSettings.config.standardSettings) {
                if (setting != this && setting instanceof KeyBindingStandardSetting && setting.isEnabled() && this.value.equals(((KeyBindingStandardSetting) setting).value)) {
                    return Formatting.RED + text;
                }
            }
        }
        return text;
    }

    // copy of KeyBinding#getLocalizedName
    private String getLocalizedName(InputUtil.KeyCode keyCode) {
        String key = keyCode.getName();
        int i = keyCode.getKeyCode();
        String name = null;
        switch (keyCode.getCategory()) {
            case KEYSYM:
                name = InputUtil.getKeycodeName(i);
                break;
            case SCANCODE:
                name = InputUtil.getScancodeName(i);
                break;
            case MOUSE:
                String string3 = I18n.translate(key);
                name = Objects.equals(string3, key) ? I18n.translate(InputUtil.Type.MOUSE.getName(), i + 1) : string3;
        }
        return name == null ? I18n.translate(key) : name;
    }

    @Override
    public @NotNull AbstractButtonWidget createMainWidget() {
        return new ButtonWidget(0, 0, 120, 20, this.getText(), button -> StandardSettings.config.setFocusedKeyBinding(this)) {
            @Override
            public void render(int mouseX, int mouseY, float delta) {
                this.setMessage(KeyBindingStandardSetting.this.getText());
                super.render(mouseX, mouseY, delta);
            }
        };
    }
}
