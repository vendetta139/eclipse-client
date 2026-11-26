package ru.eclipse.client.module.impl;

import ru.eclipse.client.module.Category;
import ru.eclipse.client.module.Module;
import ru.eclipse.client.module.setting.BooleanSetting;

public class NoOverlayModule extends Module {
    private final BooleanSetting fire = new BooleanSetting("Без огня", "Полностью убирает огонь на экране", false);
    private final BooleanSetting suffocation = new BooleanSetting("Без удушья", "Убирает текстуру блока внутри головы", true);
    private final BooleanSetting underwater = new BooleanSetting("Без воды", "Убирает мутную текстуру под водой", true);
    private final BooleanSetting pumpkin = new BooleanSetting("Без тыквы", "Убирает черную маску тыквы на голове", true);

    public NoOverlayModule() {
        super("NoOverlay", "Отключение мешающих оверлеев на экране (удушье, тыква, вода, огонь)", Category.HUD_OVERLAYS, 0, true);
        addSetting(fire);
        addSetting(suffocation);
        addSetting(underwater);
        addSetting(pumpkin);
    }

    public BooleanSetting getFire() {
        return fire;
    }

    public BooleanSetting getSuffocation() {
        return suffocation;
    }

    public BooleanSetting getUnderwater() {
        return underwater;
    }

    public BooleanSetting getPumpkin() {
        return pumpkin;
    }
}
