package com.example.client.systems.modules.movement;

import com.example.client.systems.modules.Category;
import com.example.client.systems.modules.Module;
import com.example.client.systems.settings.BooleanSetting;
import com.example.client.systems.settings.NumberSetting;
import com.example.client.systems.settings.ModeSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

import java.util.Random;

public class Strafe extends Module {
    private final BooleanSetting airStrafe = new BooleanSetting("Air Strafe", true);
    private final BooleanSetting groundStrafe = new BooleanSetting("Ground Strafe", true);
    private final NumberSetting speed = new NumberSetting("Speed", 0.12, 0.01, 1.0, 0.01);
    private final BooleanSetting autoJump = new BooleanSetting("Auto Jump", false);
    private final NumberSetting jumpHeight = new NumberSetting("Jump Height", 0.42, 0.1, 1.0, 0.01);
    private final ModeSetting dragBackMode = new ModeSetting("Drag Back Mode", "Off", "Off", "Instant", "Highest", "Random", "Distance", "YPort"); // Added "YPort" mode
    private final NumberSetting yPortHeight = new NumberSetting("YPort Height", 1.0, 0.1, 5.0, 0.1); // New setting for YPort height

    private final Random random = new Random();
    private boolean hasJumped = false; // Track if a jump has been initiated

    public Strafe() {
        super("Strafe", Category.MOVEMENT, false);
        addSetting(airStrafe);
        addSetting(groundStrafe);
        addSetting(speed);
        addSetting(autoJump);
        addSetting(jumpHeight);
        addSetting(dragBackMode);
        addSetting(yPortHeight); // Add the new setting
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        Vec2 move = mc.player.input.getMoveVector(); // x = strafe, y = forward
        boolean isMoving = move.lengthSquared() > 0.000001F;
        boolean onGround = mc.player.onGround();

        // Reset hasJumped if player is on ground
        if (onGround) {
            hasJumped = false;
        }

        // --- YPort Mode Logic ---
        if (dragBackMode.get().equals("YPort")) {
            if (onGround && isMoving) {
                // Teleport player up by yPortHeight
                mc.player.setPos(mc.player.getX(), mc.player.getY() + yPortHeight.get(), mc.player.getZ());
                // Apply horizontal movement after teleporting
                float forwardInput = Math.signum(move.y);
                float sideInput = Math.signum(move.x);
                float playerYaw = mc.player.getYRot();

                float strafeAngle = 90 * sideInput;
                if (forwardInput != 0) {
                    strafeAngle *= forwardInput * 0.5f;
                }

                playerYaw = playerYaw - strafeAngle;
                if (forwardInput < 0) {
                    playerYaw -= 180;
                }
                double yawRadians = Math.toRadians(playerYaw);

                double currentSpeed = speed.get();

                double x = -Math.sin(yawRadians) * currentSpeed;
                double z = Math.cos(yawRadians) * currentSpeed;

                mc.player.setDeltaMovement(x, mc.player.getDeltaMovement().y, z);
            } else if (!isMoving) {
                // If no input, stop horizontal movement
                mc.player.setDeltaMovement(0, mc.player.getDeltaMovement().y, 0);
            }
            return; // YPort mode handles its own movement, so return early
        }
        // --- End YPort Mode Logic ---


        if (!isMoving) {
            // If no input, stop horizontal movement
            mc.player.setDeltaMovement(0, mc.player.getDeltaMovement().y, 0);
            return;
        }

        if (onGround && !groundStrafe.get()) return;
        if (!onGround && !airStrafe.get()) return;

        // Auto Jump (using new jumpHeight setting)
        if (autoJump.get() && onGround) {
            mc.player.setDeltaMovement(mc.player.getDeltaMovement().x, jumpHeight.get(), mc.player.getDeltaMovement().z);
            hasJumped = true; // Mark that a jump has occurred
        }

        float forwardInput = Math.signum(move.y);
        float sideInput = Math.signum(move.x);
        float playerYaw = mc.player.getYRot();

        float strafeAngle = 90 * sideInput;
        if (forwardInput != 0) {
            strafeAngle *= forwardInput * 0.5f;
        }

        playerYaw = playerYaw - strafeAngle;
        if (forwardInput < 0) {
            playerYaw -= 180;
        }
        double yawRadians = Math.toRadians(playerYaw);

        double currentSpeed = speed.get();

        double x = -Math.sin(yawRadians) * currentSpeed;
        double z = Math.cos(yawRadians) * currentSpeed;

        // Apply the horizontal movement
        mc.player.setDeltaMovement(x, mc.player.getDeltaMovement().y, z);

        // Drag Back Mode
        if (!onGround) {
            switch (dragBackMode.get()) {
                case "Instant":
                    mc.player.setDeltaMovement(mc.player.getDeltaMovement().x, -0.2D, mc.player.getDeltaMovement().z);
                    break;
                case "Highest":
                    mc.player.setDeltaMovement(mc.player.getDeltaMovement().x, -0.5D, mc.player.getDeltaMovement().z); // More aggressive
                    break;
                case "Random":
                    double randomDrag = -0.1D - (0.4D * random.nextDouble()); // Random value between -0.1 and -0.5
                    mc.player.setDeltaMovement(mc.player.getDeltaMovement().x, randomDrag, mc.player.getDeltaMovement().z);
                    break;
                case "Distance":
                    // Only apply extreme drag if a jump was initiated and player is at or past jump peak
                    if (hasJumped && mc.player.getDeltaMovement().y <= 0.0D) {
                        mc.player.setDeltaMovement(mc.player.getDeltaMovement().x, -5.0D, mc.player.getDeltaMovement().z); // Extremely quick drag
                        hasJumped = false; // Reset after applying extreme drag
                    }
                    break;
                case "Off":
                default:
                    // Do nothing
                    break;
            }
        }
    }
}