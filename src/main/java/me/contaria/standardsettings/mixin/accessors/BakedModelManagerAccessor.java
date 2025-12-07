package me.contaria.standardsettings.mixin.accessors;

import net.minecraft.client.render.model.BakedModelManager;
import net.minecraft.client.render.model.ModelLoader;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.profiler.Profiler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(BakedModelManager.class)
public interface BakedModelManagerAccessor {
    @Invoker("method_18178")
    ModelLoader standardsettings$prepare(ResourceManager resourceManager, Profiler profiler);

    @Invoker("method_18179")
    void standardsettings$apply(ModelLoader modelLoader, ResourceManager resourceManager, Profiler profiler);
}
