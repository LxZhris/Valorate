package com.example.client.mixin;

import com.example.client.systems.modules.ModuleManager;
import net.minecraft.client.renderer.SectionOcclusionGraph;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SectionOcclusionGraph.class)
public class XRayChunkOcclusionMixin {

    @Inject(method = "isVisible", at = @At("HEAD"), cancellable = true)
    private void valorate$isVisible(CallbackInfoReturnable<Boolean> cir) {
        if (ModuleManager.XRAY != null && ModuleManager.XRAY.isEnabled()) {
            cir.setReturnValue(true);
        }
    }
}