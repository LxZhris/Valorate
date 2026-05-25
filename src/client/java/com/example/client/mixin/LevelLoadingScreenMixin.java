package com.example.client.mixin;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.awt.Color;

@Mixin(Screen.class)
public class LevelLoadingScreenMixin {

    @Inject(method = "extractRenderState", at = @At("HEAD"), cancellable = true)
    private void valorateLoadingScreen(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (!((Object) this instanceof LevelLoadingScreen)) {
            return;
        }

        Minecraft client = Minecraft.getInstance();

        int sw = graphics.guiWidth();
        int sh = graphics.guiHeight();

        int centerX = sw / 2;
        int centerY = sh / 2;

        float progress = ((System.currentTimeMillis() % 3000L) / 3000.0F);

        graphics.fill(0, 0, sw, sh, 0xFF08080B);

        Component title = Component.literal("Valorate").withStyle(ChatFormatting.YELLOW);
        graphics.text(client.font, title, centerX - client.font.width(title) / 2, centerY - 55, 0xFFFFDD00, false);

        int barWidth = 260;
        int barHeight = 8;
        int barX = centerX - barWidth / 2;
        int barY = centerY;

        int filled = (int) (barWidth * progress);

        graphics.fill(barX, barY, barX + barWidth, barY + barHeight, 0xFF151515);
        graphics.fill(barX, barY, barX + filled, barY + barHeight, 0xFFFFDD00);

        String percent = (int) (progress * 100.0F) + "%";
        graphics.text(client.font, Component.literal(percent), centerX - client.font.width(percent) / 2, barY + 18, 0xFFFFFFFF, false);

        int headSize = 32;
        int headX = centerX - headSize / 2;
        int headY = centerY - 38;

        graphics.fill(headX, headY, headX + headSize, headY + headSize, 0xFF202020);
        graphics.fill(headX, headY + headSize - (int) (headSize * progress), headX + headSize, headY + headSize, rainbow(0));

        ci.cancel();
    }

    private int rainbow(int offset) {
        float hue = ((System.currentTimeMillis() + offset) % 6000L) / 6000.0F;
        return Color.HSBtoRGB(hue, 0.85F, 1.0F);
    }
}