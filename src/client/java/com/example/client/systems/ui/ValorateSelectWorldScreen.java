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

        g.fill(0, 0, this.width, this.height, 0xFF07100F);

        super.extractRenderState(g, mouseX, mouseY, delta);

        drawButtonStyle(g, mc, mouseX, mouseY, this.width / 2 - 155, this.height - 52, 150, 20, "Play Selected World");
        drawButtonStyle(g, mc, mouseX, mouseY, this.width / 2 + 5, this.height - 52, 150, 20, "Create New World");

        drawButtonStyle(g, mc, mouseX, mouseY, this.width / 2 - 155, this.height - 28, 72, 20, "Edit");
        drawButtonStyle(g, mc, mouseX, mouseY, this.width / 2 - 77, this.height - 28, 72, 20, "Delete");
        drawButtonStyle(g, mc, mouseX, mouseY, this.width / 2 + 5, this.height - 28, 72, 20, "Re-Create");
        drawButtonStyle(g, mc, mouseX, mouseY, this.width / 2 + 83, this.height - 28, 72, 20, "Back");

        g.text(mc.font, Component.literal("Developer Build"), 4, this.height - 12, 0xFF55FFFF, false);

        g.text(
                mc.font,
                Component.literal("FPS: " + Minecraft.getInstance().getFps()),
                this.width - 60,
                this.height - 12,
                0xFFFFDD00,
                false
        );
    }

    private void drawButtonStyle(
            GuiGraphicsExtractor g,
            Minecraft mc,
            int mouseX,
            int mouseY,
            int x,
            int y,
            int width,
            int height,
            String text
    ) {
        // Vanilla Button verstecken
        g.fill(
                x - 2,
                y - 2,
                x + width + 2,
                y + height + 2,
                0xFF07100F
        );

        boolean hovered =
                mouseX >= x &&
                        mouseX <= x + width &&
                        mouseY >= y &&
                        mouseY <= y + height;

        boolean important =
                text.equals("Create New World")
                        || text.equals("Back");

        int bg = hovered
                ? 0xAA1F2F2B
                : 0x88101010;

        int border;

        if (hovered) {
            border = 0xFFFFDD00;
        }
        else if (important) {
            border = 0xAAFFFFFF;
        }
        else {
            border = 0x66333333;
        }

        int textColor =
                hovered
                        ? 0xFFFFFFFF
                        : 0xFFDADADA;

        g.fill(x, y, x + width, y + height, bg);

        g.fill(x, y, x + width, y + 1, border);
        g.fill(x, y + height - 1, x + width, y + height, border);
        g.fill(x, y, x + 1, y + height, border);
        g.fill(x + width - 1, y, x + width, y + height, border);

        g.text(
                mc.font,
                Component.literal(text),
                x + width / 2 - mc.font.width(text) / 2,
                y + height / 2 - 4,
                textColor,
                false
        );
    }
}