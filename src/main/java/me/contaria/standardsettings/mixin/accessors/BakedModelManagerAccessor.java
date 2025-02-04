package me.contaria.standardsettings.mixin.accessors;

import net.minecraft.client.render.model.BakedModelManager;
import net.minecraft.client.render.model.ModelLoader;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.profiler.Profiler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(BakedModelManager.class)
public interface BakedModelManagerAccessor {
    @Accessor("mipmapLevels")
    int standardsettings$getMipmapLevels();

    @Invoker("prepare")
    ModelLoader standardsettings$prepare(ResourceManager resourceManager, Profiler profiler);

    @Invoker("apply")
    void standardsettings$apply(ModelLoader modelLoader, ResourceManager resourceManager, Profiler profiler);
}
