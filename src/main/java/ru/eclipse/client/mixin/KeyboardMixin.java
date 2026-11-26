package ru.eclipse.client.mixin;

import net.minecraft.client.Keyboard;
import net.minecraft.client.input.KeyInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.eclipse.client.event.EventBus;
import ru.eclipse.client.event.impl.EventKey;

@Mixin(Keyboard.class)
public abstract class KeyboardMixin {

    @Inject(method = "onKey", at = @At("HEAD"))
    private void onKeyboardKey(long window, int action, KeyInput keyInput, CallbackInfo ci) {
        if (keyInput != null) {
            EventBus.post(new EventKey(keyInput.key(), action, keyInput.modifiers()));
        }
    }
}
