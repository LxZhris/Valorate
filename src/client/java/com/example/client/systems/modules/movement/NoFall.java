package com.example.client.systems.modules.movement;

import com.example.client.systems.modules.Category;
import com.example.client.systems.modules.Module;
import com.example.client.systems.settings.BooleanSetting;
import com.example.client.systems.settings.ModeSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;

public class NoFall extends Module {
    private final ModeSetting mode = new ModeSetting("Mode", "Packet", "Packet", "Bucket");
    private final BooleanSetting antiBounce = new BooleanSetting("Anti Bounce", true);

    private int oldSlot = -1;
    private boolean placedWater = false;
    private int pickupTimer = 0;

    public NoFall() {
        super("NoFall", Category.MOVEMENT, false, "Prevents fall damage.");

        addSetting(mode);
        addSetting(antiBounce);
    }

    @Override
    public void onTick() {
        if (!isEnabled()) return;

        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null) return;
        if (client.player.getAbilities().instabuild) return;

        if (mode.get().equalsIgnoreCase("Bucket")) {
            handleBucket(client);
        }

        if (antiBounce.get() && client.player.onGround()) {
            client.player.setDeltaMovement(
                    client.player.getDeltaMovement().x,
                    Math.min(client.player.getDeltaMovement().y, 0.0),
                    client.player.getDeltaMovement().z
            );
        }
    }

    private void handleBucket(Minecraft client) {
        if (!placedWater && client.player.fallDistance > 4.0F) {
            int bucketSlot = findWaterBucket(client);

            if (bucketSlot == -1) return;

            oldSlot = client.player.getInventory().getSelectedSlot();
            client.player.getInventory().setSelectedSlot(bucketSlot);

            client.player.setXRot(90F);
            client.gameMode.useItem(client.player, InteractionHand.MAIN_HAND);

            placedWater = true;
            pickupTimer = 0;
            return;
        }

        if (placedWater) {
            pickupTimer++;

            if (pickupTimer >= 6) {
                int bucketSlot = findBucket(client);

                if (bucketSlot != -1) {
                    client.player.getInventory().setSelectedSlot(bucketSlot);
                    client.player.setXRot(90F);
                    client.gameMode.useItem(client.player, InteractionHand.MAIN_HAND);
                }

                if (oldSlot != -1) {
                    client.player.getInventory().setSelectedSlot(oldSlot);
                }

                oldSlot = -1;
                placedWater = false;
                pickupTimer = 0;
            }
        }
    }

    private int findWaterBucket(Minecraft client) {
        for (int i = 0; i < 9; i++) {
            if (client.player.getInventory().getItem(i).getItem() == Items.WATER_BUCKET) {
                return i;
            }
        }

        return -1;
    }

    private int findBucket(Minecraft client) {
        for (int i = 0; i < 9; i++) {
            if (client.player.getInventory().getItem(i).getItem() == Items.BUCKET) {
                return i;
            }
        }

        return -1;
    }

    public boolean isPacketMode() {
        return isEnabled() && mode.get().equalsIgnoreCase("Packet");
    }
}