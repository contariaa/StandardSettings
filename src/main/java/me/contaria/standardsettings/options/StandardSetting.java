package me.contaria.standardsettings.options;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import me.contaria.speedrunapi.config.api.SpeedrunOption;
import me.contaria.standardsettings.StandardGameOptions;
import me.contaria.standardsettings.gui.StandardOptionWidget;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.widget.AbstractButtonWidget;
import net.minecraft.client.options.GameOptions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
    public final @NotNull AbstractButtonWidget createWidget() {
        return new StandardOptionWidget(this, this.createMainWidget());
    }

    public abstract @NotNull AbstractButtonWidget createMainWidget();

    @Override
    public @Nullable String getDescription() {
        return null;
    }

    @Override
    public final @NotNull String getText() {
        if (!this.isEnabled()) {
            return "-";
        }
        return this.getDisplayText();
    }

    protected abstract @NotNull String getDisplayText();

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

    protected static String getStringWithoutPrefix(String text, String prefix) {
        if (!text.startsWith(prefix)) {
            return text;
        }
        return text.substring(prefix.length());
    }
}
