package com.example.client.systems.modules.render;

import com.example.client.systems.modules.Category;
import com.example.client.systems.modules.Module;
import com.example.client.systems.settings.BooleanSetting;
import com.example.client.systems.settings.NumberSetting;

public class Notifications extends Module {
    private final NumberSetting duration =
            new NumberSetting("Duration", 2.0, 1.0, 8.0, 0.5);

    private final NumberSetting maxAmount =
            new NumberSetting("Max Amount", 3.0, 1.0, 8.0, 1.0);

    private final BooleanSetting sound =
            new BooleanSetting("Sound", true);

    public Notifications() {
        super(
                "Notifications",
                Category.RENDER,
                true,
                "Shows animated module toggle notifications."
        );

        addSetting(duration);
        addSetting(maxAmount);
        addSetting(sound);
    }

    public double getDurationSeconds() {
        return duration.get();
    }

    public int getMaxAmount() {
        return maxAmount.get().intValue();
    }

    public boolean shouldPlaySound() {
        return sound.get();
    }
}