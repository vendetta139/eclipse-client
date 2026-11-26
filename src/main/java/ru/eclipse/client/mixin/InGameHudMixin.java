package ru.eclipse.client.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.eclipse.client.module.ModuleManager;
import ru.eclipse.client.module.impl.NoOverlayModule;
import ru.example.donorprefix.hud.HudManager;
import ru.example.donorprefix.visual.VisualConfig;

@Mixin(InGameHud.class)
public abstract class InGameHudMixin {

    @Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true)
    private void onRenderCrosshairHead(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        VisualConfig cfg = VisualConfig.getCurrentOrNull();
        if (cfg != null && cfg.customCrosshair) {
            ci.cancel();
        }
    }

    @Inject(method = "renderOverlay", at = @At("HEAD"), cancellable = true)
    private void onRenderOverlay(DrawContext context, Identifier texture, float opacity, CallbackInfo ci) {
        NoOverlayModule noOverlay = ModuleManager.NO_OVERLAY;
        if (noOverlay == null || !noOverlay.isEnabled() || !noOverlay.getPumpkin().isEnabled()) return;
        if (texture != null && texture.getPath().contains("pumpkin")) {
            ci.cancel();
        }
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void onRenderHudTail(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.options.hudHidden) return;
        HudManager.getInstance().renderInGame(context);
    }
}
