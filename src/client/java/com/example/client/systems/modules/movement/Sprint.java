package com.example.client.systems.modules.movement;

import com.example.client.systems.modules.Category;
import com.example.client.systems.modules.Module;
import net.minecraft.client.Minecraft;

public class Sprint extends Module {
    private Mode mode = Mode.SAFE;

    public Sprint() {
        super("Sprint", Category.MOVEMENT, false);
    }

    public void onTick() {
        Minecraft client = Minecraft.getInstance();

        if (!isEnabled()) return;
        if (client.player == null) return;
        if (client.options == null) return;

        boolean movingForward = client.options.keyUp.isDown();
        boolean movingBackward = client.options.keyDown.isDown();
        boolean movingLeft = client.options.keyLeft.isDown();
        boolean movingRight = client.options.keyRight.isDown();

        boolean moving = movingForward || movingBackward || movingLeft || movingRight;

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

        if (mode == Mode.SAFE) {
            client.player.setSprinting(movingForward);
            return;
        }

        if (mode == Mode.ADVANCED) {
            client.player.setSprinting(true);
        }
    }


    public void onDisable() {
        Minecraft client = Minecraft.getInstance();

        if (client.player != null) {
            client.player.setSprinting(false);
        }
    }

    public Mode getMode() {
        return mode;
    }

    public void setMode(Mode mode) {
        this.mode = mode;
    }

    public void toggleMode() {
        mode = mode == Mode.SAFE ? Mode.ADVANCED : Mode.SAFE;
    }

    public enum Mode {
        SAFE,
        ADVANCED
    }
}