package com.example.client.mixin;

import com.example.client.systems.modules.ModuleManager;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public class CrosshairMixin {

    @Inject(
            method = "extractCrosshair",
            at = @At("HEAD"),
            cancellable = true
    )
    private void valorate$hideCrosshair(
            GuiGraphicsExtractor graphics,
            DeltaTracker deltaTracker,
            CallbackInfo ci
    ) {
        if (ModuleManager.CROSSHAIR != null
                && ModuleManager.CROSSHAIR.isEnabled()) {
            ci.cancel();
        }
    }
}