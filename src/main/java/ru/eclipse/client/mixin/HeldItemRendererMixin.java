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

        ViewModelModule mod = ModuleManager.getInstance().getModule(ViewModelModule.class);
        if (mod != null && mod.isEnabled()) {
            float posX = mod.getPosX().getFloatValue();
            float posY = mod.getPosY().getFloatValue();
            float posZ = mod.getPosZ().getFloatValue();

            if (hand == Hand.OFF_HAND) {
                posX = -posX;
            }

            matrices.translate(posX, posY, posZ);
            matrices.scale(mod.getScaleX().getFloatValue(), mod.getScaleY().getFloatValue(), mod.getScaleZ().getFloatValue());
        }

        VisualConfig cfg = VisualConfig.getInstance();
        if (Math.abs(cfg.handScale - 1.0f) > 0.01f) {
            float s = cfg.handScale;
            matrices.scale(s, s, s);
        }

        boolean lowShield = (mod != null && mod.isEnabled() && mod.getLowShield().isEnabled()) || cfg.smallShield;
        if (lowShield && item != null && isShield(item)) {
            matrices.translate(0.0f, -0.18f, 0.0f);
        }

        boolean blockHit = (mod != null && mod.isEnabled() && mod.getBlockHit().isEnabled()) || cfg.blockHit;
        if (blockHit && hand == Hand.MAIN_HAND && item != null && isSword(item)) {
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
        if (stack == null || stack.isEmpty()) return false;
        if (stack.isIn(ItemTags.SWORDS)) return true;
        return stack.getItem().toString().toLowerCase().contains("sword");
    }

    @Unique
    private static boolean isShield(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        if (stack.isOf(Items.SHIELD)) return true;
        return stack.getItem().toString().toLowerCase().contains("shield");
    }
}
