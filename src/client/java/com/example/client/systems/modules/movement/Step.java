package com.example.client.systems.modules.movement;

import com.example.client.systems.modules.Category;
import com.example.client.systems.modules.Module;
import com.example.client.systems.settings.BooleanSetting;
import com.example.client.systems.settings.ModeSetting;
import com.example.client.systems.settings.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.ai.attributes.Attributes;


public class Step extends Module {
    private final NumberSetting height = new NumberSetting("Height", 1.25, 0.6, 3.0, 0.05);
    private final ModeSetting activeWhen = new ModeSetting("Active When", "Always", "Always", "Sneaking", "NotSneaking");
    private final BooleanSetting safeStep = new BooleanSetting("Safe Step", false);
    private final NumberSetting stepHealth = new NumberSetting("Step Health", 5.0, 1.0, 36.0, 1.0);private double previousStepHeight = 0.6;

    public Step() {
        super("Step", Category.MOVEMENT, false,
                "Steps up blocks automatically");

        addSetting(height);
        addSetting(activeWhen);
        addSetting(safeStep);
        addSetting(stepHealth);
    }

    public void onEnable() {
        Minecraft client = Minecraft.getInstance();

        if (client.player != null && client.player.getAttribute(Attributes.STEP_HEIGHT) != null) {
            previousStepHeight = client.player.getAttribute(Attributes.STEP_HEIGHT).getBaseValue();
        }
    }

    public void onTick() {
        Minecraft client = Minecraft.getInstance();

        if (client.player == null || client.player.getAttribute(Attributes.STEP_HEIGHT) == null) {
            return;
        }

        boolean active =
                activeWhen.get().equalsIgnoreCase("Always")
                        || activeWhen.get().equalsIgnoreCase("Sneaking") && client.player.isShiftKeyDown()
                        || activeWhen.get().equalsIgnoreCase("NotSneaking") && !client.player.isShiftKeyDown();

        double maxHeight = getMaxSafeHeight(client);

        if (isEnabled() && active && maxHeight > 0.0) {
            client.player.getAttribute(Attributes.STEP_HEIGHT).setBaseValue(maxHeight);
        } else {
            client.player.getAttribute(Attributes.STEP_HEIGHT).setBaseValue(previousStepHeight);
        }
    }

    public void onDisable() {
        Minecraft client = Minecraft.getInstance();

        if (client.player != null && client.player.getAttribute(Attributes.STEP_HEIGHT) != null) {
            client.player.getAttribute(Attributes.STEP_HEIGHT).setBaseValue(previousStepHeight);
        }
    }

    private double getMaxSafeHeight(Minecraft client) {
        if (!safeStep.get()) {
            return height.get();
        }

        if (getHealth(client) <= stepHealth.get()) {
            return 0.0;
        }

        return height.get();
    }

    private float getHealth(Minecraft client) {
        return client.player.getHealth() + client.player.getAbsorptionAmount();
    }

    public NumberSetting getHeightSetting() {
        return height;
    }

    public ModeSetting getActiveWhenSetting() {
        return activeWhen;
    }

    public BooleanSetting getSafeStepSetting() {
        return safeStep;
    }

    public NumberSetting getStepHealthSetting() {
        return stepHealth;
    }
}