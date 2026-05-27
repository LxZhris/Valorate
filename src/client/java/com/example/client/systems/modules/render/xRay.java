package com.example.client.systems.modules.render;

import com.example.client.systems.modules.Category;
import com.example.client.systems.modules.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashSet;
import java.util.Set;

public class xRay extends Module {
    private final Set<Object> visibleBlocks = new HashSet<>();
    private double oldGamma = 1.0;

    public xRay() {
        super("XRay", Category.RENDER, false);

        visibleBlocks.add(Blocks.DIAMOND_ORE);
        visibleBlocks.add(Blocks.DEEPSLATE_DIAMOND_ORE);
        visibleBlocks.add(Blocks.GOLD_ORE);
        visibleBlocks.add(Blocks.DEEPSLATE_GOLD_ORE);
        visibleBlocks.add(Blocks.IRON_ORE);
        visibleBlocks.add(Blocks.DEEPSLATE_IRON_ORE);
        visibleBlocks.add(Blocks.EMERALD_ORE);
        visibleBlocks.add(Blocks.DEEPSLATE_EMERALD_ORE);
        visibleBlocks.add(Blocks.REDSTONE_ORE);
        visibleBlocks.add(Blocks.DEEPSLATE_REDSTONE_ORE);
        visibleBlocks.add(Blocks.LAPIS_ORE);
        visibleBlocks.add(Blocks.DEEPSLATE_LAPIS_ORE);
        visibleBlocks.add(Blocks.COAL_ORE);
        visibleBlocks.add(Blocks.DEEPSLATE_COAL_ORE);
        visibleBlocks.add(Blocks.COPPER_ORE);
        visibleBlocks.add(Blocks.DEEPSLATE_COPPER_ORE);
        visibleBlocks.add(Blocks.ANCIENT_DEBRIS);
        visibleBlocks.add(Blocks.NETHER_QUARTZ_ORE);
        visibleBlocks.add(Blocks.NETHER_GOLD_ORE);
        visibleBlocks.add(Blocks.SPAWNER);
    }

    @Override
    public void onEnable() {
        Minecraft client = Minecraft.getInstance();

        oldGamma = client.options.gamma().get();
        client.options.gamma().set(16.0);

        reloadWorld();
    }

    @Override
    public void onDisable() {
        Minecraft client = Minecraft.getInstance();

        client.options.gamma().set(oldGamma);

        reloadWorld();
    }

    @Override
    public void onTick() {
        if (!isEnabled()) return;

        Minecraft client = Minecraft.getInstance();

        if (client.levelRenderer != null) {
            client.levelRenderer.needsUpdate();
        }
    }

    public boolean shouldShow(BlockState state) {
        return visibleBlocks.contains(state.getBlock());
    }

    private void reloadWorld() {
        Minecraft client = Minecraft.getInstance();

        if (client.levelRenderer != null) {
            client.levelRenderer.allChanged();
        }
    }
}