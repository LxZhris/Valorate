package com.example.client.mixin;

import com.example.client.systems.modules.ModuleManager;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(LevelRenderer.class)
public class ClickTPOutlineMixin {

    @ModifyVariable(
            method = "renderHitOutline",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 0,
            require = 0
    )
    private int valorate$clickTpOutlineColor(int color) {
        if (ModuleManager.CLICK_TP != null && ModuleManager.CLICK_TP.hasTarget()) {
            int alpha = ModuleManager.CLICK_TP.getOverlayOpacity();
            alpha = Math.max(0, Math.min(255, alpha));

            return (alpha << 24) | 0x00FFDD00;
        }

        return color;
    }

    @ModifyVariable(
            method = "renderHitOutline",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 0,
            require = 0
    )
    private float valorate$clickTpOutlineWidth(float width) {
        if (ModuleManager.CLICK_TP != null && ModuleManager.CLICK_TP.hasTarget()) {
            return 3.0F;
        }

        return width;
    }
}