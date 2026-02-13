package me.contaria.standardsettings.options;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import me.contaria.speedrunapi.config.api.SpeedrunOption;
import me.contaria.speedrunapi.util.TextUtil;
import me.contaria.standardsettings.StandardGameOptions;
import me.contaria.standardsettings.gui.StandardOptionWidget;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.widget.AbstractButtonWidget;
import net.minecraft.client.options.GameOptions;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public abstract class StandardSetting<T> implements SpeedrunOption<T> {
    private final String id;
    private final String category;
    protected final StandardGameOptions options;

    private boolean enabled = true;

    public StandardSetting(String id, String category, StandardGameOptions options) {
        this.id = id;
        this.category = category;
        this.options = options;
    }

    @Override
    public String getID() {
        return this.id;
    }

    @Override
    public String getCategory() {
        return this.category;
    }

    @Override
    public void setCategory(@Nullable String category) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getModID() {
        return "standardsettings";
    }

    @Override
    public T get() {
        return this.get(this.options);
    }

    @Override
    public void set(T value) {
        this.set(this.options, value);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void setUnsafely(Object value) throws ClassCastException {
        this.set((T) value);
    }

    @Override
    public boolean hasWidget() {
        return true;
    }

    @Override
    public @NotNull AbstractButtonWidget createWidget() {
        return new StandardOptionWidget(this, this.createMainWidget());
    }

    public abstract @NotNull AbstractButtonWidget createMainWidget();

    @Override
    public @Nullable Text getDescription() {
        return null;
    }

    @Override
    public final @NotNull Text getText() {
        if (!this.isEnabled()) {
            return TextUtil.literal("-");
        }
        return this.getDisplayText();
    }

    protected abstract @NotNull Text getDisplayText();

    @Override
    public final void fromJson(JsonElement jsonElement) {
        if (jsonElement.isJsonObject()) {
            this.enabled = jsonElement.getAsJsonObject().get("enabled").getAsBoolean();
            this.valueFromJson(jsonElement.getAsJsonObject().get("value"));
        } else {
            this.enabled = true;
            this.valueFromJson(jsonElement);
        }
    }

    protected abstract void valueFromJson(JsonElement jsonElement);

    @Override
    public final JsonElement toJson() {
        if (this.enabled) {
            return this.valueToJson();
        }
        JsonObject jsonObject = new JsonObject();
        jsonObject.add("enabled", new JsonPrimitive(false));
        jsonObject.add("value", this.valueToJson());
        return jsonObject;
    }

    protected abstract JsonElement valueToJson();

    protected abstract void set(GameOptions options, T value);

    protected abstract T get(GameOptions options);

    public T getOption() {
        return this.get(MinecraftClient.getInstance().options);
    }

    public void setOption(T value) {
        this.set(MinecraftClient.getInstance().options, value);
    }

    public void resetOption() {
        if (this.isEnabled()) {
            this.setOption(this.get());
        }
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean toggleEnabled() {
        return this.enabled = !this.enabled;
    }

    public void disable() {
        this.enabled = false;
    }

    protected static Text getTextWithoutPrefix(Text text, Text prefix) {
        if (!text.copy().equals(prefix.copy())) {
            return text;
        }

        List<Text> prefixSiblings = prefix.getSiblings();
        List<Text> textSiblings = text.getSiblings();

        if (prefixSiblings.size() >= textSiblings.size()) {
            return TextUtil.empty();
        }

        List<Text> restText = textSiblings.subList(prefixSiblings.size(), textSiblings.size());

        MutableText newText = restText.remove(0).shallowCopy();
        for (Text t : restText) {
            newText.append(t);
        }
        return newText;
    }
}
