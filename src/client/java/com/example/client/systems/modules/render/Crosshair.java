package com.example.client.systems.modules.render;

import com.example.client.systems.modules.Category;
import com.example.client.systems.modules.Module;
import com.example.client.systems.settings.BooleanSetting;
import com.example.client.systems.settings.ModeSetting;

public class Crosshair extends Module {
    private final ModeSetting symbol =
            new ModeSetting("Symbol", "卐");

    private final BooleanSetting shadow =
            new BooleanSetting("Shadow", true);

    public Crosshair() {
        super("Crosshair", Category.RENDER, false, "Replaces the default crosshair with a custom symbol.");

        addSetting(symbol);
        addSetting(shadow);
    }

    public String getSymbol() {
        return symbol.get();
    }

    public boolean hasShadow() {
        return shadow.get();
    }
}