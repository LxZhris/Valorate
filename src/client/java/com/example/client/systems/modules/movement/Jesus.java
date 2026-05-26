package com.example.client.systems.modules.movement;

import com.example.client.systems.modules.Category;
import com.example.client.systems.modules.Module;
import com.example.client.systems.settings.BooleanSetting;
import com.example.client.systems.settings.ModeSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.tags.FluidTags;

public class Jesus extends Module {

    private final ModeSetting mode =
            new ModeSetting(
                    "Mode",
                    "Vanilla",
                    "Vanilla",
                    "Strict"
            );

    private final BooleanSetting water =
            new BooleanSetting("Water", true);

    private final BooleanSetting lava =
            new BooleanSetting("Lava", false);

    public Jesus() {
        super("Jesus", Category.MOVEMENT, false);

        addSetting(mode);
        addSetting(water);
        addSetting(lava);
    }

    public void onTick() {
        if (!isEnabled()) return;

        Minecraft client = Minecraft.getInstance();

        if (client.player == null) return;

        boolean inWater =
                client.player.level()
                        .getFluidState(client.player.blockPosition())
                        .is(FluidTags.WATER);

        boolean inLava =
                client.player.level()
                        .getFluidState(client.player.blockPosition())
                        .is(FluidTags.LAVA);

        boolean active =
                (water.get() && inWater)
                        ||
                        (lava.get() && inLava);

        if (!active) return;

        switch (mode.get()) {

            case "Vanilla" -> {
                client.player.setDeltaMovement(
                        client.player.getDeltaMovement().x,
                        0.1,
                        client.player.getDeltaMovement().z
                );
            }

            case "Strict" -> {
                client.player.setDeltaMovement(
                        client.player.getDeltaMovement().x,
                        0.03,
                        client.player.getDeltaMovement().z
                );
            }
        }
    }
}