package ru.eclipse.client.mixin;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import net.minecraft.block.enums.CameraSubmersionType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.fog.FogRenderer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.eclipse.client.module.ModuleManager;
import ru.eclipse.client.module.impl.NoFogModule;

@Mixin(FogRenderer.class)
public abstract class FogRendererMixin {

    @Shadow @Final private GpuBuffer emptyBuffer;

    @Inject(method = "getFogBuffer", at = @At("HEAD"), cancellable = true)
    private void onGetFogBuffer(FogRenderer.FogType type, CallbackInfoReturnable<GpuBufferSlice> cir) {
        NoFogModule mod = ModuleManager.NO_FOG;
        if (mod == null || !mod.isEnabled()) {
            return;
        }

        String mode = mod.getMode().getValue();
        if ("All".equals(mode)) {
            cir.setReturnValue(this.emptyBuffer.slice(0L, FogRenderer.FOG_UBO_SIZE));
            return;
        }

        CameraSubmersionType submersion = CameraSubmersionType.NONE;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc != null && mc.gameRenderer != null && mc.gameRenderer.getCamera() != null) {
            submersion = mc.gameRenderer.getCamera().getSubmersionType();
        }

        boolean shouldDisable = switch (mode) {
            case "Atmosphere" -> submersion == CameraSubmersionType.NONE || submersion == CameraSubmersionType.ATMOSPHERIC;
            case "Water" -> submersion == CameraSubmersionType.WATER;
            case "Lava" -> submersion == CameraSubmersionType.LAVA;
            default -> false;
        };

        if (shouldDisable) {
            cir.setReturnValue(this.emptyBuffer.slice(0L, FogRenderer.FOG_UBO_SIZE));
        }
    }
}
