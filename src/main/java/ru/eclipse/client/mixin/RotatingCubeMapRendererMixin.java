package ru.eclipse.client.mixin;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.RotatingCubeMapRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RotatingCubeMapRenderer.class)
public abstract class RotatingCubeMapRendererMixin {

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void onRenderCubeMap(DrawContext context, int width, int height, boolean bl, CallbackInfo ci) {
        ci.cancel();
    }
}
