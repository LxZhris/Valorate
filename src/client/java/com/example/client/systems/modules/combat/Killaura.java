package com.example.client.systems.modules.combat;

import com.example.client.systems.modules.Category;
import com.example.client.systems.modules.Module;
import com.example.client.systems.settings.ButtonSetting;
import com.example.client.systems.settings.BooleanSetting;
import com.example.client.systems.settings.ModeSetting;
import com.example.client.systems.settings.NumberSetting;
import com.example.client.systems.targets.TargetManager;
import com.example.client.systems.ui.TargetSelectionScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.Packet;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.lang.reflect.Constructor;
import java.util.Comparator;
import java.util.List;
import java.util.Objects; // Import Objects for requireNonNull

public class Killaura extends Module {
    public static Killaura INSTANCE;

    private final ButtonSetting targets =
            new ButtonSetting("Targets", "Select", () ->
                    Minecraft.getInstance().setScreen(new TargetSelectionScreen())
            );

    private final NumberSetting range =
            new NumberSetting("Range", 4.0, 1.0, 6.0, 0.1);

    private final BooleanSetting attackThroughWalls =
            new BooleanSetting("Through Walls", false);

    private final BooleanSetting rotate =
            new BooleanSetting("Rotate", true); // New Rotate setting

    private final ModeSetting f5RotationMode =
            new ModeSetting("F5 Rotation", "Off", "Off", "Client", "Server", "Silent");

    private final ModeSetting targetMode =
            new ModeSetting("Target Mode", "Single", "Single", "Switch");

    private final NumberSetting switchDelay =
            (NumberSetting) new NumberSetting("Switch Delay", 3, 1, 10, 1)
                    .visibleWhen(() -> targetMode.get().equals("Switch"));

    private LivingEntity target;
    private long lastAttackTick = -1L;

    public Killaura() {
        super("Killaura", Category.COMBAT, false, "Automatically attacks selected nearby targets.");

        INSTANCE = this;

        addSetting(targets);

        addSetting(range);
        addSetting(attackThroughWalls);
        addSetting(rotate); // Add new setting
        addSetting(f5RotationMode);
        addSetting(targetMode);
        addSetting(switchDelay);
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();

        if (!isEnabled()) {
            target = null;
            return;
        }

        if (mc.player == null || mc.level == null) {
            target = null;
            return;
        }

        Objects.requireNonNull(mc.player); // Assert non-null after check
        Objects.requireNonNull(mc.level); // Assert non-null after check

        target = findTarget(mc);
        if (target == null) return;

        float yaw = 0;
        float pitch = 0;

        if (rotate.get()) { // Only calculate and apply rotation if 'Rotate' is enabled
            Vec3 playerEyePos = mc.player.getEyePosition(1.0F);
            Vec3 targetEyePos = target.getEyePosition(1.0F);

            double x = targetEyePos.x - playerEyePos.x;
            double y = targetEyePos.y - playerEyePos.y;
            double z = targetEyePos.z - playerEyePos.z;

            double horizontalDistance = Math.sqrt(x * x + z * z);

            yaw = (float) Mth.wrapDegrees(Math.toDegrees(Math.atan2(z, x)) - 90.0F);
            pitch = (float) Mth.wrapDegrees(Math.toDegrees(-Math.atan2(y, horizontalDistance)));

            String mode = f5RotationMode.get();

            if ("Client".equals(mode)) {
                mc.player.setYRot(yaw);
                mc.player.setXRot(pitch);
            } else if ("Server".equals(mode)) {
                if (!sendRotationPacket(mc, yaw, pitch)) {
                    mc.player.setYRot(yaw);
                    mc.player.setXRot(pitch);
                }
            } else if ("Silent".equals(mode)) {
                sendRotationPacket(mc, yaw, pitch);
            }
        }

        double attackSpeed = mc.player.getAttributeValue(Attributes.ATTACK_SPEED);
        if (attackSpeed <= 0.0) attackSpeed = 1.0;

        int cooldownTicks = Math.max(1, (int) Math.ceil(20.0 / attackSpeed));
        long currentTick = mc.level.getGameTime();

        if (lastAttackTick < 0 || currentTick - lastAttackTick >= cooldownTicks) {
            if (rotate.get() && ("Server".equals(f5RotationMode.get()) || "Silent".equals(f5RotationMode.get()))) {
                // Re-send rotation packet right before attack for server-side/silent modes if rotate is enabled
                sendRotationPacket(mc, yaw, pitch);
            }

            mc.gameMode.attack(mc.player, target);
            mc.player.swing(mc.player.getUsedItemHand());
            lastAttackTick = currentTick;
        }
    }

    @Override
    public void onDisable() {
        target = null;
        lastAttackTick = -1L;
    }

    private LivingEntity findTarget(Minecraft mc) {
        Objects.requireNonNull(mc.player); // Assert non-null after check
        Objects.requireNonNull(mc.level); // Assert non-null after check

        List<LivingEntity> potentialTargets = mc.level.getEntitiesOfClass(
                LivingEntity.class,
                mc.player.getBoundingBox().inflate(range.get()),
                entity -> {
                    if (entity == mc.player || entity.isDeadOrDying()) return false;

                    if (!TargetManager.isTarget(entity)) return false;

                    if (!attackThroughWalls.get() && !canSeeEntity(mc, entity)) return false;

                    return mc.player.distanceTo(entity) <= range.get();
                }
        );

        return potentialTargets.stream()
                .min(Comparator.comparingDouble(mc.player::distanceTo))
                .orElse(null);
    }

    private boolean canSeeEntity(Minecraft mc, LivingEntity entity) {
        Objects.requireNonNull(mc.player); // Assert non-null after check
        Objects.requireNonNull(mc.level); // Assert non-null after check

        Vec3 start = mc.player.getEyePosition(1.0F);
        Vec3 end = entity.getEyePosition(1.0F);

        ClipContext context = new ClipContext(
                start,
                end,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                mc.player
        );

        HitResult result = mc.level.clip(context);

        return result.getType() == HitResult.Type.MISS;
    }

    private boolean sendRotationPacket(Minecraft mc, float yaw, float pitch) {
        Objects.requireNonNull(mc.player); // Assert non-null after check
        Objects.requireNonNull(mc.getConnection()); // Assert non-null after check

        String[] candidates = new String[]{
                "net.minecraft.network.protocol.game.ServerboundMovePlayerPacket$Rot",
                "net.minecraft.network.protocol.game.ServerboundMovePlayerPacket$Rotation",
                "net.minecraft.network.protocol.game.PlayerMoveC2SPacket$LookOnly",
                "net.minecraft.network.protocol.game.PlayerMoveC2SPacket$Rotation",
                "net.minecraft.network.protocol.game.ServerboundPlayerRotationPacket"
        };

        boolean onGround;

        try {
            onGround = mc.player.onGround();
        } catch (Exception e) {
            onGround = true;
        }

        for (String className : candidates) {
            try {
                Class<?> pktClass = Class.forName(className);

                Constructor<?> ctor = null;

                try {
                    ctor = pktClass.getConstructor(float.class, float.class, boolean.class);
                } catch (NoSuchMethodException e) {
                    try {
                        ctor = pktClass.getConstructor(double.class, double.class, boolean.class);
                    } catch (NoSuchMethodException ignored) {
                    }
                }

                if (ctor == null) continue;

                Object pktObj;

                if (ctor.getParameterTypes()[0] == float.class) {
                    pktObj = ctor.newInstance(yaw, pitch, onGround);
                } else {
                    pktObj = ctor.newInstance((double) yaw, (double) pitch, onGround);
                }

                Packet<?> packet = (Packet<?>) pktObj;
                mc.getConnection().send(packet);

                return true;
            } catch (ClassNotFoundException ignored) {
            } catch (Exception ignored) {
            }
        }

        return false;
    }
}
