package com.example.client.systems.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.network.chat.Component;

public class ValorateSelectWorldScreen extends SelectWorldScreen {

    public ValorateSelectWorldScreen(Screen parent) {
        super(parent);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        Minecraft mc = Minecraft.getInstance();

        // Valorate Background wie Main Menu
        g.fill(0, 0, this.width, this.height, 0xFF07100F);

        // Original World-Screen rendern: Weltenliste + Buttons bleiben normal
        super.extractRenderState(g, mouseX, mouseY, delta);

        // Valorate Footer
        g.text(mc.font, Component.literal("Developer Build"), 4, this.height - 12, 0xFF55FFFF, false);
        g.text(
                mc.font,
                Component.literal("FPS: " + Minecraft.getInstance().getFps()),
                this.width - 60,
                this.height - 12,
                0xFFFFDD00,
                false
        );    }
}