package ru.eclipse.client.mixin;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ContainerWidget;
import net.minecraft.client.gui.widget.EntryListWidget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntryListWidget.class)
public abstract class EntryListWidgetMixin extends ContainerWidget {

    public EntryListWidgetMixin(int x, int y, int width, int height, Text message) {
        super(x, y, width, height, message);
    }

    @Inject(method = "drawMenuListBackground", at = @At("HEAD"), cancellable = true)
    private void onDrawMenuListBackground(DrawContext context, CallbackInfo ci) {
        ci.cancel();
        context.fill(this.getX(), this.getY(), this.getRight(), this.getBottom(), 0x900B0B10);
    }

    @Inject(method = "drawHeaderAndFooterSeparators", at = @At("HEAD"), cancellable = true)
    private void onDrawSeparators(DrawContext context, CallbackInfo ci) {
        ci.cancel();
        context.fill(this.getX(), this.getY() - 1, this.getRight(), this.getY(), 0xFF232533);
        context.fill(this.getX(), this.getBottom(), this.getRight(), this.getBottom() + 1, 0xFF232533);
    }
}
