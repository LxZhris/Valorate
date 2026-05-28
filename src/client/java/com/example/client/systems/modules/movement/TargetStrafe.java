package com.example.client.systems.modules.movement;

import com.example.client.systems.modules.Category;
import com.example.client.systems.modules.Module;
import com.example.client.systems.settings.BooleanSetting;
import com.example.client.systems.settings.NumberSetting;
import com.example.client.systems.settings.ModeSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.Random;

public class TargetStrafe extends Module {
    private final NumberSetting speed = new NumberSetting("Speed", 0.12, 0.01, 1.0, 0.01);
    private final BooleanSetting autoJump = new BooleanSetting("Auto Jump", false);
    private final NumberSetting jumpHeight = new NumberSetting("Jump Height", 0.42, 0.1, 1.0, 0.01);
    private final ModeSetting dragBackMode = new ModeSetting("Drag Back Mode", "Off", "Off", "Instant", "Highest", "Random", "Distance", "YPort");
    private final NumberSetting yPortHeight = new NumberSetting("YPort Height", 1.0, 0.1, 5.0, 0.1);
    private final NumberSetting distance = new NumberSetting("Distance", 3.0, 0.5, 10.0, 0.1);
    private final ModeSetting direction = new ModeSetting("Direction", "Clockwise", "Clockwise", "Counter-Clockwise", "Adaptive");
    private final NumberSetting targetRange = new NumberSetting("Target Range", 10.0, 1.0, 30.0, 0.5);
    private final BooleanSetting changeDirectionOnStuck = new BooleanSetting("Change Direction on Stuck", true);

    private final Random random = new Random();
    private boolean hasJumped = false;
    private int adaptiveDirectionTimer = 0;
    private boolean isClockwise = true;
    private Vec3 lastPlayerPos = Vec3.ZERO;
    private int stuckTicks = 0;

    public TargetStrafe() {
        super(
                "TargetStrafe",
                Category.MOVEMENT,
                false,
                "Circles around a target entity."
        );
        addSetting(speed);

        // autoJump is always visible, but jumpHeight depends on it
        addSetting(autoJump);

        // jumpHeight is visible only when autoJump is true
        jumpHeight.visibleWhen(autoJump::get);
        addSetting(jumpHeight);

        // dragBackMode is always visible, but yPortHeight depends on it
        addSetting(dragBackMode);

        // yPortHeight is visible only when dragBackMode is YPort
        yPortHeight.visibleWhen(() -> dragBackMode.get().equals("YPort"));
        addSetting(yPortHeight);

        addSetting(distance);
        addSetting(direction);
        addSetting(targetRange);
        addSetting(changeDirectionOnStuck);
    }

    @Override
    public void onTick() {
        if (!isEnabled()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        Entity target = findTarget();
        if (target == null) {
            mc.player.setDeltaMovement(0, mc.player.getDeltaMovement().y, 0);
            return;
        }

        boolean onGround = mc.player.onGround();

        if (onGround) {
            hasJumped = false;
        }

        if (direction.get().equals("Clockwise")) {
            isClockwise = true;
        } else if (direction.get().equals("Counter-Clockwise")) {
            isClockwise = false;
        }

        boolean wasStuck = false;
        if (changeDirectionOnStuck.get()) {
            if (mc.player.position().distanceToSqr(lastPlayerPos) < 0.001 && mc.player.getDeltaMovement().horizontalDistanceSqr() < 0.001) {
                stuckTicks++;
                if (stuckTicks > 10) {
                    isClockwise = !isClockwise;
                    stuckTicks = 0;
                    wasStuck = true;
                }
            } else {
                stuckTicks = 0;
            }
            lastPlayerPos = mc.player.position();
        }

        if (direction.get().equals("Adaptive") && !wasStuck) {
            adaptiveDirectionTimer++;
            if (adaptiveDirectionTimer > 60) {
                isClockwise = !isClockwise;
                adaptiveDirectionTimer = 0;
            }
        }

        if (dragBackMode.get().equals("YPort")) {
            if (onGround) {
                mc.player.setPos(mc.player.getX(), mc.player.getY() + yPortHeight.get(), mc.player.getZ());
                applyTargetStrafeMovement(mc, target, speed.get());
            }
            return;
        }

        if (autoJump.get() && onGround) {
            mc.player.setDeltaMovement(mc.player.getDeltaMovement().x, jumpHeight.get(), mc.player.getDeltaMovement().z);
            hasJumped = true;
        }

        applyTargetStrafeMovement(mc, target, speed.get());

        if (!onGround) {
            switch (dragBackMode.get()) {
                case "Instant":
                    mc.player.setDeltaMovement(mc.player.getDeltaMovement().x, -0.2D, mc.player.getDeltaMovement().z);
                    break;
                case "Highest":
                    mc.player.setDeltaMovement(mc.player.getDeltaMovement().x, -0.5D, mc.player.getDeltaMovement().z);
                    break;
                case "Random":
                    double randomDrag = -0.1D - (0.4D * random.nextDouble());
                    mc.player.setDeltaMovement(mc.player.getDeltaMovement().x, randomDrag, mc.player.getDeltaMovement().z);
                    break;
                case "Distance":
                    if (hasJumped && mc.player.getDeltaMovement().y <= 0.0D) {
                        mc.player.setDeltaMovement(mc.player.getDeltaMovement().x, -5.0D, mc.player.getDeltaMovement().z);
                        hasJumped = false;
                    }
                    break;
                case "Off":
                default:
                    break;
            }
        }
    }

    private void applyTargetStrafeMovement(Minecraft mc, Entity target, double currentSpeed) {
        Vec3 playerPos = mc.player.position();
        Vec3 targetPos = target.position();

        double deltaX = targetPos.x - playerPos.x;
        double deltaZ = targetPos.z - playerPos.z;

        double angleToTarget = Math.toDegrees(Math.atan2(deltaZ, deltaX));

        double strafeAngle = angleToTarget + (isClockwise ? -90 : 90);

        double strafeYawRadians = Math.toRadians(strafeAngle);

        double x = Math.cos(strafeYawRadians);
        double z = Math.sin(strafeYawRadians);

        double currentDistance = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
        double desiredDistance = distance.get();

        double forwardComponent = 0;
        if (currentDistance > desiredDistance + 0.1) {
            forwardComponent = 0.5;
        } else if (currentDistance < desiredDistance - 0.1) {
            forwardComponent = -0.5;
        }

        double moveTowardsAngle = Math.toRadians(angleToTarget);
        double moveX = x + Math.cos(moveTowardsAngle) * forwardComponent;
        double moveZ = z + Math.sin(moveTowardsAngle) * forwardComponent;

        Vec3 finalMovement = new Vec3(moveX, 0, moveZ).normalize().multiply(currentSpeed, 0, currentSpeed);

        mc.player.setDeltaMovement(finalMovement.x, mc.player.getDeltaMovement().y, finalMovement.z);
    }

    private Entity findTarget() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return null;

        Entity closestTarget = null;
        double closestDistanceSq = Double.MAX_VALUE;
        double rangeSq = targetRange.get() * targetRange.get();

        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity instanceof LivingEntity && entity != mc.player) {
                double distSq = mc.player.distanceToSqr(entity);
                if (distSq < rangeSq && distSq < closestDistanceSq) {
                    closestDistanceSq = distSq;
                    closestTarget = entity;
                }
            }
        }
        return closestTarget;
    }
}