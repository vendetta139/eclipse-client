package ru.eclipse.client.module.impl;

import ru.eclipse.client.module.Category;
import ru.eclipse.client.module.Module;
import ru.eclipse.client.module.setting.BooleanSetting;

public class NoOverlayModule extends Module {
    private final BooleanSetting fire = new BooleanSetting("Без огня", "Полностью убирает огонь с экрана", false);
    private final BooleanSetting suffocation = new BooleanSetting("Без удушья", "Скрывает текстуру блока внутри головы", true);
    private final BooleanSetting underwater = new BooleanSetting("Без воды", "Убирает мутный фильтр под водой", true);
    private final BooleanSetting pumpkin = new BooleanSetting("Без тыквы", "Скрывает маску тыквы на экране", true);

    public NoOverlayModule() {
        super("NoOverlay", "Скрывает оверлеи тыквы, воды, огня и удушья", Category.HUD_OVERLAYS, 0, true);
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
