package ru.example.donorprefix.visual;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;

/**
 * Отслеживание текущей цели в бою для Target HUD и светящейся ауры (Target ESP Circle).
 */
public class TargetHUDManager {
    private static LivingEntity currentTarget = null;
    private static long lastTargetTime = 0;
    private static final long TARGET_TIMEOUT_MS = 2500;

    public static void setTarget(Entity target) {
        if (target instanceof LivingEntity living && living.isAlive() && !living.isRemoved()) {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client != null && living == client.player) return;
            currentTarget = living;
            lastTargetTime = System.currentTimeMillis();
        }
    }

    public static LivingEntity getCurrentTarget() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (currentTarget != null) {
            boolean valid = currentTarget.isAlive() 
                    && !currentTarget.isRemoved() 
                    && currentTarget.getHealth() > 0.0f
                    && (System.currentTimeMillis() - lastTargetTime <= TARGET_TIMEOUT_MS);
            if (valid && client != null && client.player != null) {
                if (client.player.distanceTo(currentTarget) > 24.0f) {
                    valid = false;
                }
            }
            if (!valid) {
                currentTarget = null;
            } else {
                return currentTarget;
            }
        }
        try {
            if (client != null && client.player != null) {
                if (client.targetedEntity instanceof LivingEntity living && living.isAlive() && !living.isRemoved() && living != client.player && living.getHealth() > 0.0f) {
                    return living;
                }
                if (client.crosshairTarget instanceof net.minecraft.util.hit.EntityHitResult ehr && ehr.getEntity() instanceof LivingEntity living && living.isAlive() && !living.isRemoved() && living != client.player && living.getHealth() > 0.0f) {
                    return living;
                }
            }
        } catch (Throwable ignored) {}
        return null;
    }
}
