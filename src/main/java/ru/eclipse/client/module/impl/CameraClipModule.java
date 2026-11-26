package ru.eclipse.client.module.impl;

import ru.eclipse.client.module.Category;
import ru.eclipse.client.module.Module;

public class CameraClipModule extends Module {
    public CameraClipModule() {
        super("CameraClip", "Камера от третьего лица свободно проходит сквозь стены и блоки", Category.WORLD_FOG, 0, false);
    }
}
