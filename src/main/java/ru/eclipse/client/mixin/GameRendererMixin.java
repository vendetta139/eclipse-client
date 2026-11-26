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
        NoHurtCamModule mod = ModuleManager.getInstance().getModule(NoHurtCamModule.class);
        if (mod != null && mod.isEnabled()) {
            ci.cancel();
        }
    }

    @Inject(method = "getNightVisionStrength", at = @At("HEAD"), cancellable = true)
    private static void onGetNightVisionStrength(LivingEntity entity, float tickDelta, CallbackInfoReturnable<Float> cir) {
        FullbrightModule mod = ModuleManager.getInstance().getModule(FullbrightModule.class);
        if (mod != null && mod.isEnabled() && (mod.getMode().is("NightVision") || mod.getMode().is("Both"))) {
            cir.setReturnValue(1.0f);
        }
    }
}
