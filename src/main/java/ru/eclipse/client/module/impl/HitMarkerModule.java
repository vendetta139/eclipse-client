package ru.eclipse.client.module.impl;

import ru.eclipse.client.module.Category;
import ru.eclipse.client.module.Module;
import ru.eclipse.client.module.setting.BooleanSetting;
import ru.eclipse.client.module.setting.ModeSetting;

public class HitMarkerModule extends Module {
    private final BooleanSetting sound = new BooleanSetting("Звук удара", "Играет звук при попадании", true);
    private final ModeSetting soundType = new ModeSetting("Тип звука", "Выбор звука удара", "Ding", "Ding", "Skeet", "Chime", "Arrow", "Bonk");

    public HitMarkerModule() {
        super("HitMarker", "Рисует крестик и играет звук при ударе", Category.COMBAT_PVP, 0, true);
        addSetting(sound);
        addSetting(soundType);
    }

    public BooleanSetting getSound() {
        return sound;
    }

    public ModeSetting getSoundType() {
        return soundType;
    }
}
