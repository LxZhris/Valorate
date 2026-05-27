package com.example.client.systems.modules.movement;

import com.example.client.systems.modules.Category;
import com.example.client.systems.modules.Module;
import com.example.client.systems.settings.ModeSetting;
import net.minecraft.client.Minecraft;

public class Sprint extends Module {
    private final ModeSetting mode = new ModeSetting(
            "Mode",
            "Safe",
            "Safe",
            "Advanced"
    );

    public Sprint() {
        super("Sprint", Category.MOVEMENT, false,
                "Automatically sprints");

        addSetting(mode);
    }

    public void onTick() {
        Minecraft client = Minecraft.getInstance();

        if (!isEnabled()) return;
        if (client.player == null) return;

        boolean forward = client.options.keyUp.isDown();
        boolean back = client.options.keyDown.isDown();
        boolean left = client.options.keyLeft.isDown();
        boolean right = client.options.keyRight.isDown();

        boolean moving = forward || back || left || right;

        boolean canSprint =
                moving &&
                        !client.player.isShiftKeyDown() &&
                        !client.player.horizontalCollision &&
                        !client.player.isUsingItem() &&
                        client.player.getFoodData().getFoodLevel() > 6;

        if (!canSprint) {
            client.player.setSprinting(false);
            return;
        }

        if (mode.get().equalsIgnoreCase("Safe")) {
            client.player.setSprinting(forward);
            return;
        }

        if (mode.get().equalsIgnoreCase("Advanced")) {
            client.player.setSprinting(moving);
        }
    }

    public void onDisable() {
        Minecraft client = Minecraft.getInstance();

        if (client.player != null) {
            client.player.setSprinting(false);
        }
    }

    public ModeSetting getModeSetting() {
        return mode;
    }
}