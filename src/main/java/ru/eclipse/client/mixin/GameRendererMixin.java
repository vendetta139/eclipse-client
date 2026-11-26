package ru.eclipse.client.mixin;

import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.eclipse.client.module.ModuleManager;
import ru.eclipse.client.module.impl.FullbrightModule;
import ru.eclipse.client.module.impl.NoHurtCamModule;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {

    @Inject(method = "tiltViewWhenHurt", at = @At("HEAD"), cancellable = true)
    private void onTiltViewWhenHurt(MatrixStack matrices, float tickDelta, CallbackInfo ci) {
        NoHurtCamModule mod = ModuleManager.NO_HURT_CAM;
        if (mod == null || !mod.isEnabled()) return;
        ci.cancel();
    }

    @Inject(method = "getNightVisionStrength", at = @At("HEAD"), cancellable = true)
    private static void onGetNightVisionStrength(LivingEntity entity, float tickDelta, CallbackInfoReturnable<Float> cir) {
        FullbrightModule mod = ModuleManager.FULLBRIGHT;
        if (mod == null || !mod.isEnabled()) return;
        if (mod.getMode().is("NightVision") || mod.getMode().is("Both")) {
            cir.setReturnValue(1.0f);
        }
    }
}
