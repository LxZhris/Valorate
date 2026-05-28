package com.example.client.systems.modules.render;

import com.example.client.systems.modules.Category;
import com.example.client.systems.modules.Module;
import com.example.client.systems.settings.ModeSetting;
import com.example.client.systems.theme.ThemeManager;

public class HUD extends Module {
    private boolean watermark = true;

    private final ModeSetting theme =
            new ModeSetting("Theme", "Valorate", "Valorate", "SS");

    public HUD() {
        super("HUD", Category.RENDER, false,
                "Displays client HUD elements");

        setVisibleInArraylist(false);

        addSetting(theme);
    }

    @Override
    public void onTick() {
        ThemeManager.setTheme(theme.get());
    }

    public ModeSetting getThemeSetting() {
        return theme;
    }

    public boolean isSSTheme() {
        return theme.get().equalsIgnoreCase("SS");
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