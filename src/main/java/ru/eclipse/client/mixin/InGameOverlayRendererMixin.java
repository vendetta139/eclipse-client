package ru.eclipse.client.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.InGameOverlayRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.eclipse.client.module.ModuleManager;
import ru.eclipse.client.module.impl.LowFireModule;
import ru.eclipse.client.module.impl.NoOverlayModule;

@Mixin(InGameOverlayRenderer.class)
public abstract class InGameOverlayRendererMixin {

    @Inject(method = "renderFireOverlay", at = @At("HEAD"), cancellable = true)
    private static void onRenderFireOverlay(MatrixStack matrices, VertexConsumerProvider vertexConsumers, Sprite sprite, CallbackInfo ci) {
        NoOverlayModule noOverlay = ModuleManager.NO_OVERLAY;
        if (noOverlay != null && noOverlay.isEnabled() && noOverlay.getFire().isEnabled()) {
            ci.cancel();
            return;
        }

        LowFireModule lowFire = ModuleManager.LOW_FIRE;
        if (lowFire == null || !lowFire.isEnabled() || matrices == null) return;
        matrices.translate(0.0f, lowFire.getHeight().getFloatValue(), 0.0f);
    }

    @Inject(method = "renderInWallOverlay", at = @At("HEAD"), cancellable = true)
    private static void onRenderInWallOverlay(Sprite sprite, MatrixStack matrices, VertexConsumerProvider vertexConsumers, CallbackInfo ci) {
        NoOverlayModule noOverlay = ModuleManager.NO_OVERLAY;
        if (noOverlay == null || !noOverlay.isEnabled() || !noOverlay.getSuffocation().isEnabled()) return;
        ci.cancel();
    }

    @Inject(method = "renderUnderwaterOverlay", at = @At("HEAD"), cancellable = true)
    private static void onRenderUnderwaterOverlay(MinecraftClient client, MatrixStack matrices, VertexConsumerProvider vertexConsumers, CallbackInfo ci) {
        NoOverlayModule noOverlay = ModuleManager.NO_OVERLAY;
        if (noOverlay == null || !noOverlay.isEnabled() || !noOverlay.getUnderwater().isEnabled()) return;
        ci.cancel();
    }
}
