package ru.eclipse.client.mixin;

import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.eclipse.client.core.EclipseClient;
import ru.eclipse.client.event.EventBus;
import ru.eclipse.client.event.impl.EventTick;

@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin {

    @Inject(method = "getWindowTitle", at = @At("HEAD"), cancellable = true)
    private void onGetWindowTitle(CallbackInfoReturnable<String> cir) {
        cir.setReturnValue(EclipseClient.WINDOW_TITLE);
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void onClientTick(CallbackInfo ci) {
        EventBus.post(new EventTick());
    }
}
