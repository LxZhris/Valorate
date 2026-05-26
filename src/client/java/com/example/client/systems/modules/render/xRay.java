package com.example.client.systems.modules.render;

import com.example.client.systems.modules.Category;
import com.example.client.systems.modules.Module;
import com.example.client.systems.settings.BooleanSetting;
import com.example.client.systems.settings.ModeSetting;
import com.example.client.systems.settings.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.tags.FluidTags;

import java.util.List;

public class xRay extends Module {
    public static xRay INSTANCE;

    private final NumberSetting opacity = new NumberSetting("Opacity", 25.0, 0.0, 255.0, 1.0);
    private final ModeSetting fluidOpacity = new ModeSetting("Fluid Opacity", "Both", "None", "Water", "Lava", "Both");
    private final BooleanSetting exposedOnly = new BooleanSetting("Exposed Only", false);

    private final BooleanSetting ores = new BooleanSetting("Ores", true);
    private final BooleanSetting ancientDebris = new BooleanSetting("Ancient Debris", true);
    private final BooleanSetting spawners = new BooleanSetting("Spawners", true);

    public static final List<Block> ORE_BLOCKS = List.of(
            Blocks.COAL_ORE,
            Blocks.DEEPSLATE_COAL_ORE,
            Blocks.IRON_ORE,
            Blocks.DEEPSLATE_IRON_ORE,
            Blocks.COPPER_ORE,
            Blocks.DEEPSLATE_COPPER_ORE,
            Blocks.GOLD_ORE,
            Blocks.DEEPSLATE_GOLD_ORE,
            Blocks.REDSTONE_ORE,
            Blocks.DEEPSLATE_REDSTONE_ORE,
            Blocks.LAPIS_ORE,
            Blocks.DEEPSLATE_LAPIS_ORE,
            Blocks.DIAMOND_ORE,
            Blocks.DEEPSLATE_DIAMOND_ORE,
            Blocks.EMERALD_ORE,
            Blocks.DEEPSLATE_EMERALD_ORE,
            Blocks.NETHER_GOLD_ORE,
            Blocks.NETHER_QUARTZ_ORE
    );

    public xRay() {
        super("XRay", Category.RENDER, false);

        INSTANCE = this;

        addSetting(opacity);
        addSetting(fluidOpacity);
        addSetting(exposedOnly);
        addSetting(ores);
        addSetting(ancientDebris);
        addSetting(spawners);
    }

    @Override
    public void onEnable() {
        Minecraft mc = Minecraft.getInstance();

        if (mc.levelRenderer != null) {
            mc.levelRenderer.allChanged();
        }
    }

    @Override
    public void onDisable() {
        Minecraft mc = Minecraft.getInstance();

        if (mc.levelRenderer != null) {
            mc.levelRenderer.allChanged();
        }
    }



    public boolean shouldShow(BlockState state) {
        Block block = state.getBlock();

        if (ores.get() && ORE_BLOCKS.contains(block)) return true;
        if (ancientDebris.get() && block == Blocks.ANCIENT_DEBRIS) return true;
        if (spawners.get() && block == Blocks.SPAWNER) return true;

        return false;
    }

    public boolean shouldHide(BlockState state) {
        if (!isEnabled()) return false;

        return !shouldShow(state);
    }

    public int getAlpha(BlockState state) {
        if (!isEnabled()) return -1;

        if (shouldShow(state)) {
            return -1;
        }

        return opacity.get().intValue();
    }

    public int getFluidAlpha(FluidState state) {
        if (!isEnabled()) return -1;

        boolean water = state.is(FluidTags.WATER);
        boolean lava = state.is(FluidTags.LAVA);

        boolean apply = switch (fluidOpacity.get()) {
            case "None" -> false;
            case "Water" -> water;
            case "Lava" -> lava;
            case "Both" -> water || lava;
            default -> false;
        };

        if (!apply) return -1;

        return opacity.get().intValue();
    }

    public boolean isExposedOnly() {
        return exposedOnly.get();
    }

    public int getOpacity() {
        return opacity.get().intValue();
    }

    public void reloadChunks() {
        Minecraft client = Minecraft.getInstance();

        if (client.levelRenderer != null) {
            client.levelRenderer.allChanged();
        }
    }
}