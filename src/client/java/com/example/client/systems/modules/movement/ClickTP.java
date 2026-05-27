package com.example.client.systems.modules.movement;

import com.example.client.systems.modules.Category;
import com.example.client.systems.modules.Module;
import com.example.client.systems.settings.BooleanSetting;
import com.example.client.systems.settings.ModeSetting;
import com.example.client.systems.settings.NumberSetting;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.VoxelShape;

public class ClickTP extends Module {
    private final ModeSetting mode = new ModeSetting("Mode", "Packet", "Packet", "SetPos");
    private final NumberSetting range = new NumberSetting("Range", 200.0, 10.0, 210.0, 5.0);
    private final NumberSetting overlayOpacity = new NumberSetting("Overlay Opacity", 120.0, 0.0, 255.0, 5.0);
    private final BooleanSetting sneakOnly = new BooleanSetting("Sneak Only", false);
    private final BooleanSetting ignoreInteract = new BooleanSetting("Ignore Interact", true);

    private BlockHitResult currentHit;

    public ClickTP() {
        super("ClickTP", Category.MOVEMENT, false, "Teleports you on top of the block you right click.");

        addSetting(mode);
        addSetting(range);
        addSetting(overlayOpacity);
        addSetting(sneakOnly);
        addSetting(ignoreInteract);
    }

    @Override
    public void onTick() {
        if (!isEnabled()) {
            currentHit = null;
            return;
        }

        Minecraft mc = Minecraft.getInstance();

        if (mc.player == null || mc.level == null) {
            currentHit = null;
            return;
        }

        currentHit = raycast(mc);

        if (!mc.options.keyUse.isDown()) return;
        if (sneakOnly.get() && !mc.player.isShiftKeyDown()) return;

        if (mc.player.getInventory().getSelectedItem().getUseAnimation() != ItemUseAnimation.NONE) return;

        if (ignoreInteract.get() && mc.hitResult != null) {
            if (mc.hitResult.getType() == HitResult.Type.ENTITY) {
                EntityHitResult entityHit = (EntityHitResult) mc.hitResult;

                if (mc.player.interactOn(entityHit.getEntity(), InteractionHand.MAIN_HAND, mc.hitResult.getLocation()) != InteractionResult.PASS) {
                    return;
                }
            }

            if (mc.hitResult.getType() == HitResult.Type.BLOCK
                    && mc.player.getMainHandItem().getItem() instanceof BlockItem) {
                return;
            }
        }

        if (currentHit == null || currentHit.getType() != HitResult.Type.BLOCK) return;

        Vec3 newPos = getTeleportPosition(mc, currentHit);
        if (newPos == null) return;

        if (mode.get().equalsIgnoreCase("Packet")) {
            teleportPacket(mc, newPos);
        } else {
            mc.player.setPos(newPos);
        }
    }

    private BlockHitResult raycast(Minecraft mc) {
        Camera camera = mc.gameRenderer.getMainCamera();
        Vec3 cameraPos = camera.position();

        Vec3 direction = Vec3.directionFromRotation(camera.xRot(), camera.yRot()).scale(range.get());
        Vec3 targetPos = cameraPos.add(direction);

        ClipContext context = new ClipContext(
                cameraPos,
                targetPos,
                ClipContext.Block.OUTLINE,
                ClipContext.Fluid.NONE,
                mc.player
        );

        BlockHitResult hit = mc.level.clip(context);

        if (hit.getType() != HitResult.Type.BLOCK) {
            return null;
        }

        return hit;
    }

    private Vec3 getTeleportPosition(Minecraft mc, BlockHitResult hit) {
        BlockState state = mc.level.getBlockState(hit.getBlockPos());

        if (ignoreInteract.get()) {
            if (state.useWithoutItem(mc.level, mc.player, hit) != InteractionResult.PASS) {
                return null;
            }
        }

        VoxelShape shape = state.getCollisionShape(mc.level, hit.getBlockPos());

        if (shape.isEmpty()) {
            shape = state.getShape(mc.level, hit.getBlockPos());
        }

        double height = shape.isEmpty() ? 1.0 : shape.max(Direction.Axis.Y);

        return new Vec3(
                hit.getBlockPos().getX() + 0.5,
                hit.getBlockPos().getY() + height,
                hit.getBlockPos().getZ() + 0.5
        );
    }

    private void teleportPacket(Minecraft mc, Vec3 newPos) {
        int packetsRequired = (int) Math.ceil(mc.player.position().distanceTo(newPos) / 10.0) - 1;

        if (packetsRequired > 19) {
            packetsRequired = 0;
        }

        for (int i = 0; i < packetsRequired; i++) {
            mc.player.connection.send(new ServerboundMovePlayerPacket.StatusOnly(true, true));
        }

        mc.player.connection.send(new ServerboundMovePlayerPacket.Pos(newPos.x, newPos.y, newPos.z, true, true));
        mc.player.setPos(newPos);
    }

    public boolean hasTarget() {
        return isEnabled() && currentHit != null;
    }

    public BlockHitResult getCurrentHit() {
        return currentHit;
    }

    public int getOverlayOpacity() {
        return overlayOpacity.get().intValue();
    }
}