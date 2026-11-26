package ru.eclipse.client.mixin;

import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.eclipse.client.module.ModuleManager;
import ru.eclipse.client.module.impl.TimeChangerModule;
import ru.eclipse.client.module.impl.WeatherChangerModule;

@Mixin(World.class)
public abstract class WorldMixin {

    @Shadow public abstract boolean isClient();

    @Inject(method = "getTimeOfDay", at = @At("HEAD"), cancellable = true)
    private void onGetTimeOfDay(CallbackInfoReturnable<Long> cir) {
        if (this.isClient()) {
            TimeChangerModule mod = ModuleManager.getInstance().getModule(TimeChangerModule.class);
            if (mod != null && mod.isEnabled()) {
                long fixed = mod.getFixedTime();
                if (fixed >= 0) {
                    cir.setReturnValue(fixed);
                }
            }
        }
    }

    @Inject(method = "getRainGradient", at = @At("HEAD"), cancellable = true)
    private void onGetRainGradient(float delta, CallbackInfoReturnable<Float> cir) {
        if (this.isClient()) {
            WeatherChangerModule mod = ModuleManager.getInstance().getModule(WeatherChangerModule.class);
            if (mod != null && mod.isEnabled()) {
                if (mod.getMode().is("Clear")) {
                    cir.setReturnValue(0.0f);
                } else if (mod.getMode().is("Rain") || mod.getMode().is("Thunder")) {
                    cir.setReturnValue(1.0f);
                }
            }
        }
    }

    @Inject(method = "getThunderGradient", at = @At("HEAD"), cancellable = true)
    private void onGetThunderGradient(float delta, CallbackInfoReturnable<Float> cir) {
        if (this.isClient()) {
            WeatherChangerModule mod = ModuleManager.getInstance().getModule(WeatherChangerModule.class);
            if (mod != null && mod.isEnabled()) {
                if (mod.getMode().is("Clear") || mod.getMode().is("Rain")) {
                    cir.setReturnValue(0.0f);
                } else if (mod.getMode().is("Thunder")) {
                    cir.setReturnValue(1.0f);
                }
            }
        }
    }
}
