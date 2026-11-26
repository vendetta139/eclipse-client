package ru.eclipse.client.mixin;

import net.minecraft.client.render.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.eclipse.client.module.ModuleManager;
import ru.eclipse.client.module.impl.CameraClipModule;

@Mixin(Camera.class)
public abstract class CameraMixin {

    @Inject(method = "clipToSpace", at = @At("HEAD"), cancellable = true)
    private void onClipToSpace(float desiredDistance, CallbackInfoReturnable<Float> cir) {
        CameraClipModule mod = ModuleManager.CAMERA_CLIP;
        if (mod == null || !mod.isEnabled()) return;
        cir.setReturnValue(desiredDistance);
    }
}
