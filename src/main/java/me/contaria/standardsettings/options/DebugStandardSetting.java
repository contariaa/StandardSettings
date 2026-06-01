package me.contaria.standardsettings.options;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.debug.DebugScreenEntryStatus;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DebugStandardSetting extends StandardSetting<DebugScreenEntryStatus> {
    private final Identifier id;
    private DebugScreenEntryStatus value;

    public DebugStandardSetting(String id, @Nullable String category, Identifier id2) {
        super(id, category);
        this.id = id2;

        this.set(this.getVanilla());
    }

    @Override
    public DebugScreenEntryStatus get() {
        return this.value;
    }

    @Override
    public void set(DebugScreenEntryStatus value) {
        this.value = value;
    }

    @Override
    public DebugScreenEntryStatus getVanilla() {
        return Minecraft.getInstance().debugEntries.getStatus(id);
    }

    @Override
    public void setVanilla(DebugScreenEntryStatus value) {
        Minecraft.getInstance().debugEntries.setStatus(id, value);
    }

    @Override
    protected void valueFromJson(JsonElement jsonElement) {
        DebugScreenEntryStatus.CODEC.parse(JsonOps.INSTANCE, jsonElement).result().ifPresent(this::set);
    }

    @Override
    protected JsonElement valueToJson() {
        return DebugScreenEntryStatus.CODEC.encodeStart(JsonOps.INSTANCE, this.get()).result().orElseThrow();
    }

    @Override
    public @NotNull Component getName() {
        return Component.literal(this.id.getPath());
    }

    @Override
    public @NotNull Component getDisplayText() {
        return readableStatus(this.value);
    }

    private static Component readableStatus(DebugScreenEntryStatus status) {
        return switch (status) {
            case ALWAYS_ON -> Component.translatable("debug.entry.always");
            case IN_OVERLAY -> Component.translatable("debug.entry.overlay");
            case NEVER -> CommonComponents.OPTION_OFF;
        };
    }

    @Override
    public @NotNull AbstractWidget createMainWidget() {
        return new CycleButton.Builder<>(DebugStandardSetting::readableStatus, this::get)
                .withValues(DebugScreenEntryStatus.values())
                .displayOnlyValue()
                .create(0, 0, 120, 20, Component.empty(), (button, value) -> this.set(value));
    }
}
