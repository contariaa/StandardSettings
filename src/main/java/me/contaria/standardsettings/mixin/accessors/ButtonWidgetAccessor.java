package me.contaria.standardsettings.mixin.accessors;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.widget.ButtonWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ButtonWidget.class)
public interface ButtonWidgetAccessor {
    @Invoker("mouseDragged")
    void standardsettings$mouseDragged(MinecraftClient client, int mouseX, int mouseY);

    @Accessor("width")
    void standardsettings$setWidth(int width);
}
