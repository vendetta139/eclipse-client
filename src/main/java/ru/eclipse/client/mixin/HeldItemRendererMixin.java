package ru.eclipse.client.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.Hand;
import net.minecraft.util.math.RotationAxis;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.eclipse.client.module.ModuleManager;
import ru.eclipse.client.module.impl.ViewModelModule;
import ru.example.donorprefix.visual.VisualConfig;

@Mixin(HeldItemRenderer.class)
public abstract class HeldItemRendererMixin {

    @Inject(method = "renderFirstPersonItem", at = @At("HEAD"))
    private void onRenderFirstPersonItem(AbstractClientPlayerEntity player, float tickDelta, float pitch, Hand hand, float swingProgress, ItemStack item, float equipProgress, MatrixStack matrices, OrderedRenderCommandQueue commandQueue, int light, CallbackInfo ci) {
        if (matrices == null) return;

        ViewModelModule mod = ModuleManager.VIEW_MODEL;
        boolean modEnabled = mod != null && mod.isEnabled();
        VisualConfig cfg = VisualConfig.getCurrentOrNull();
        boolean hasCfgScale = cfg != null && Math.abs(cfg.handScale - 1.0f) > 0.01f;
        boolean hasCfgShield = cfg != null && cfg.smallShield;
        boolean hasCfgBlockHit = cfg != null && cfg.blockHit;

        if (!modEnabled && !hasCfgScale && !hasCfgShield && !hasCfgBlockHit) {
            return;
        }

        if (modEnabled) {
            float posX = mod.getPosX().getFloatValue();
            float posY = mod.getPosY().getFloatValue();
            float posZ = mod.getPosZ().getFloatValue();

            if (hand == Hand.OFF_HAND) {
                posX = -posX;
            }

            if (posX != 0.0f || posY != 0.0f || posZ != 0.0f) {
                matrices.translate(posX, posY, posZ);
            }
            float sx = mod.getScaleX().getFloatValue();
            float sy = mod.getScaleY().getFloatValue();
            float sz = mod.getScaleZ().getFloatValue();
            if (sx != 1.0f || sy != 1.0f || sz != 1.0f) {
                matrices.scale(sx, sy, sz);
            }
        }

        if (hasCfgScale) {
            float s = cfg.handScale;
            matrices.scale(s, s, s);
        }

        boolean lowShield = (modEnabled && mod.getLowShield().isEnabled()) || hasCfgShield;
        if (lowShield && item != null && !item.isEmpty() && item.isOf(Items.SHIELD)) {
            matrices.translate(0.0f, -0.18f, 0.0f);
        }

        boolean blockHit = (modEnabled && mod.getBlockHit().isEnabled()) || hasCfgBlockHit;
        if (blockHit && hand == Hand.MAIN_HAND && item != null && !item.isEmpty() && isSword(item)) {
            MinecraftClient client = MinecraftClient.getInstance();
            boolean isBlocking = player.isUsingItem()
                    || (client != null && client.options != null && client.options.useKey.isPressed() && client.currentScreen == null);
            if (isBlocking) {
                matrices.translate(-0.12f, 0.12f, -0.08f);
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-20f));
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-32f));
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-18f));
            }
        }
    }

    @Unique
    private static boolean isSword(ItemStack stack) {
        return stack.isIn(ItemTags.SWORDS);
    }
}
