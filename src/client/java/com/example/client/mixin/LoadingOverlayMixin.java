package com.example.client.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.LoadingOverlay;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LoadingOverlay.class)
public class LoadingOverlayMixin {

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)    private void valorateLoading(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();

        int width = g.guiWidth();
        int height = g.guiHeight();

        g.fill(0, 0, width, height, 0xFF000000);

        String title = "VALORATE";
        String sub = "CLIENT";

        int centerX = width / 2;
        int centerY = height / 2;

        g.text(
                mc.font,
                Component.literal(title),
                centerX - mc.font.width(title) / 2,
                centerY - 18,
                0xFFFFDD00,
                false
        );

        g.text(
                mc.font,
                Component.literal(sub),
                centerX - mc.font.width(sub) / 2,
                centerY,
                0xFFFFFFFF,
                false
        );

        ci.cancel();
    }
}