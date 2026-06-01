package me.contaria.standardsettings.mixin.fix;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.resources.model.ModelManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;

@Mixin(ModelManager.class)
public abstract class ModelManagerMixin {
    @WrapOperation(method = "reload", at = @At(value = "INVOKE", target = "Ljava/util/concurrent/CompletableFuture;thenApplyAsync(Ljava/util/function/Function;)Ljava/util/concurrent/CompletableFuture;"))
    private CompletableFuture<?> useExecutor(CompletableFuture<?> instance, Function<Object, ?> fn, Operation<CompletableFuture<?>> original, @Local(argsOnly = true, ordinal = 0) Executor taskExecutor) {
        return instance.thenApplyAsync(fn, taskExecutor);
    }
}
