package ru.eclipse.client.mixin;

import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.world.dimension.DimensionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.eclipse.client.module.ModuleManager;
import ru.eclipse.client.module.impl.FullbrightModule;

@Mixin(LightmapTextureManager.class)
public abstract class LightmapTextureManagerMixin {

    @Inject(method = "getBrightness(Lnet/minecraft/world/dimension/DimensionType;I)F", at = @At("HEAD"), cancellable = true)
    private static void onGetBrightness(DimensionType type, int lightLevel, CallbackInfoReturnable<Float> cir) {
        FullbrightModule mod = ModuleManager.getInstance().getModule(FullbrightModule.class);
        if (mod != null && mod.isEnabled() && (mod.getMode().is("Gamma") || mod.getMode().is("Both"))) {
            cir.setReturnValue(1.0f);
        }
    }
}
