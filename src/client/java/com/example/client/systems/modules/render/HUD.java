package com.example.client.systems.modules.render;

import com.example.client.systems.modules.Category;
import com.example.client.systems.modules.Module;

public class HUD extends Module {
    private boolean watermark = true;

    public HUD() {
        super("HUD", Category.RENDER, true);
        setVisibleInArraylist(false);
    }

    public boolean isWatermark() {
        return watermark;
    }

    public void setWatermark(boolean watermark) {
        this.watermark = watermark;
    }

    public void toggleWatermark() {
        watermark = !watermark;
    }
}