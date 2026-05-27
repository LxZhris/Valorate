package com.example.client.mixin;

import com.example.client.systems.modules.ModuleManager;
import com.example.client.systems.modules.render.xRay;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Block.class)
public class XRayBlockMixin {

    @Inject(method = "shouldRenderFace", at = @At("HEAD"), cancellable = true)
    private static void valorate$shouldRenderFace(
            BlockState state,
            BlockState neighborState,
            Direction direction,
            CallbackInfoReturnable<Boolean> cir
    ) {
        xRay xray = ModuleManager.XRAY;

        if (xray == null || !xray.isEnabled()) {
            return;
        }

        cir.setReturnValue(
                xray.shouldShow(state) || xray.shouldShow(neighborState)
        );
    }
}