package me.contaria.standardsettings.options;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import me.contaria.speedrunapi.config.api.SpeedrunOption;
import me.contaria.speedrunapi.util.TextUtil;
import me.contaria.standardsettings.gui.StandardOptionWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.BooleanSupplier;

public abstract class StandardSetting<T> implements SpeedrunOption<T> {
    private final String id;
    private final String category;

    private BooleanSupplier visible = () -> true;

    private boolean enabled = true;

    protected StandardSetting(String id, String category) {
        this.id = id;
        this.category = category;
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

    @SuppressWarnings("unchecked")
    @Override
    public void setUnsafely(Object value) throws ClassCastException {
        this.set((T) value);
    }

    @Override
    public boolean hasWidget() {
        return this.visible.getAsBoolean();
    }

    public void setVisible(boolean visible) {
        this.visible = () -> visible;
    }

    public void setVisible(BooleanSupplier visible) {
        this.visible = visible;
    }

    @Override
    public final @NotNull ClickableWidget createWidget() {
        return new StandardOptionWidget(this, this.createMainWidget());
    }

    public abstract @NotNull ClickableWidget createMainWidget();

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

    public abstract T getVanilla();

    public abstract void setVanilla(T value);

    public void resetOption() {
        if (this.isEnabled()) {
            this.setVanilla(this.get());
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
}
