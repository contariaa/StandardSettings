package me.contaria.standardsettings.mixin.accessors;

import net.minecraft.client.render.model.BakedModelManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(BakedModelManager.class)
public interface BakedModelManagerAccessor {
    @Accessor("mipmapLevels")
    int standardsettings$getMipmapLevels();
}
