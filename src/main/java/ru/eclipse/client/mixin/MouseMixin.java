package ru.eclipse.client.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import net.minecraft.client.input.MouseInput;
import net.minecraft.entity.Entity;
import net.minecraft.util.hit.EntityHitResult;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.example.donorprefix.visual.CPSCounter;
import ru.example.donorprefix.visual.HitMarkerManager;
import ru.example.donorprefix.visual.TargetHUDManager;

@Mixin(Mouse.class)
public abstract class MouseMixin {

    @Shadow @Final private MinecraftClient client;

    @Inject(method = "onMouseButton", at = @At("HEAD"))
    private void onMouseButton(long window, MouseInput mouseInput, int action, CallbackInfo ci) {
        if (mouseInput == null) return;
        int button = mouseInput.button();
        if (action == GLFW.GLFW_PRESS && (client == null || client.currentScreen == null)) {
            if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                CPSCounter.onLeftClick();
                if (client != null && client.player != null) {
                    Entity target = client.targetedEntity;
                    if (target == null && client.crosshairTarget instanceof EntityHitResult ehr) {
                        target = ehr.getEntity();
                    }
                    if (target != null) {
                        HitMarkerManager.triggerHit();
                        TargetHUDManager.setTarget(target);
                    }
                }
            } else if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
                CPSCounter.onRightClick();
            }
        }
    }
}
