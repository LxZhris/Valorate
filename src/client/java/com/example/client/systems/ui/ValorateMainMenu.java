package com.example.client.systems.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class ValorateMainMenu extends Screen {

    private final List<MenuButton> buttons = new ArrayList<>();
    private boolean wasLeftDown = false;

    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("HH:mm:ss");

    public ValorateMainMenu() {
        super(Component.literal("Valorate Main Menu"));
    }

    protected void init() {
        buttons.clear();

        int w = 170;
        int h = 22;

        int x = width / 2 - w / 2;
        int y = height / 2 - 40;

        buttons.add(new MenuButton(
                x,
                y,
                w,
                h,
                "Singleplayer",
                () -> minecraft.setScreen(
                        new ValorateSelectWorldScreen(this)
                )
        ));

        buttons.add(new MenuButton(
                x,
                y + 24,
                w,
                h,
                "Multiplayer",
                () -> minecraft.setScreen(
                        new JoinMultiplayerScreen(this)
                )
        ));

        buttons.add(new MenuButton(
                x,
                y + 48,
                82,
                h,
                "Options...",
                () -> minecraft.setScreen(
                        new net.minecraft.client.gui.screens.options.OptionsScreen(
                                this,
                                minecraft.options,
                                false
                        )
                )
        ));

        buttons.add(new MenuButton(
                x + 88,
                y + 48,
                82,
                h,
                "Quit Game",
                () -> minecraft.stop()
        ));
    }

    @Override
    public void extractRenderState(
            GuiGraphicsExtractor g,
            int mouseX,
            int mouseY,
            float delta
    ) {

        Minecraft mc = Minecraft.getInstance();

        g.fill(
                0,
                0,
                width,
                height,
                0xFF07100F
        );

        String title = "VALORATE";

        int titleX =
                width / 2 - mc.font.width(title) / 2;

        int titleY =
                height / 2 - 80;

        g.text(
                mc.font,
                Component.literal(title),
                titleX,
                titleY,
                0xFFFFDD00,
                false
        );

        // Uhr

        String time =
                LocalTime.now().format(TIME_FORMAT);

        // kurzer &k ähnlicher Effekt

        if ((System.currentTimeMillis() % 5000) < 120) {

            char[] chars =
                    time.toCharArray();

            for (int i = 0; i < chars.length; i++) {

                if (chars[i] != ':') {

                    chars[i] =
                            (char)
                                    ('0' + (int)
                                            (Math.random() * 10));
                }
            }

            time =
                    new String(chars);
        }

        g.text(
                mc.font,
                Component.literal(time),
                width / 2 - mc.font.width(time) / 2,
                titleY + 14,
                0xFFFFDD00,
                false
        );

        // Text oben rechts

        String p1 = "Valorate Client";
        String p2 = " by ";
        String p3 = "Chris";
        String p4 = " & ";
        String p5 = "Maxi";
        String p6 = " 25.05.2026";

        String fullText = p1 + p2 + p3 + p4 + p5 + p6;

// 6 Pixel Abstand vom rechten Rand
        int xPos = width - mc.font.width(fullText) - 6;

        int yPos =
                6;

        g.text(
                mc.font,
                Component.literal(p1),
                xPos,
                yPos,
                0xFFFFFF55,
                false
        );

        xPos += mc.font.width(p1);

        g.text(
                mc.font,
                Component.literal(p2),
                xPos,
                yPos,
                0xFFAAAAAA,
                false
        );

        xPos += mc.font.width(p2);

        g.text(
                mc.font,
                Component.literal(p3),
                xPos,
                yPos,
                0xFF55FFFF,
                false
        );

        xPos += mc.font.width(p3);

        g.text(
                mc.font,
                Component.literal(p4),
                xPos,
                yPos,
                0xFFAAAAAA,
                false
        );

        xPos += mc.font.width(p4);

        g.text(
                mc.font,
                Component.literal(p5),
                xPos,
                yPos,
                0xFF55FFFF,
                false
        );

        xPos += mc.font.width(p5);

        g.text(
                mc.font,
                Component.literal(p6),
                xPos,
                yPos,
                0xFFFFFF55,
                false
        );

        for (MenuButton button : buttons) {
            button.render(
                    g,
                    mc,
                    mouseX,
                    mouseY
            );
        }

        boolean leftDown =
                GLFW.glfwGetMouseButton(
                        GLFW.glfwGetCurrentContext(),
                        GLFW.GLFW_MOUSE_BUTTON_LEFT
                ) == GLFW.GLFW_PRESS;

        if (leftDown && !wasLeftDown) {

            for (MenuButton button : buttons) {

                if (button.isHovered(mouseX, mouseY)) {
                    button.action.run();
                    break;
                }
            }
        }

        wasLeftDown = leftDown;
    }

    private static class MenuButton {

        private final int x;
        private final int y;
        private final int width;
        private final int height;

        private final String text;
        private final Runnable action;

        private MenuButton(
                int x,
                int y,
                int width,
                int height,
                String text,
                Runnable action
        ) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.text = text;
            this.action = action;
        }

        private void render(
                GuiGraphicsExtractor g,
                Minecraft mc,
                int mouseX,
                int mouseY
        ) {

            boolean hovered =
                    isHovered(mouseX, mouseY);

            int bg =
                    hovered
                            ? 0xAA1F2F2B
                            : 0x88101010;

            int border =
                    hovered
                            ? 0xFFFFDD00
                            : 0x66333333;

            int textColor =
                    hovered
                            ? 0xFFFFFFFF
                            : 0xFFDADADA;

            g.fill(
                    x,
                    y,
                    x + width,
                    y + height,
                    bg
            );

            g.fill(
                    x,
                    y,
                    x + width,
                    y + 1,
                    border
            );

            g.fill(
                    x,
                    y + height - 1,
                    x + width,
                    y + height,
                    border
            );

            g.fill(
                    x,
                    y,
                    x + 1,
                    y + height,
                    border
            );

            g.fill(
                    x + width - 1,
                    y,
                    x + width,
                    y + height,
                    border
            );

            g.text(
                    mc.font,
                    Component.literal(text),
                    x + width / 2 - mc.font.width(text) / 2,
                    y + 7,
                    textColor,
                    false
            );
        }

        private boolean isHovered(
                double mouseX,
                double mouseY
        ) {
            return mouseX >= x
                    && mouseX <= x + width
                    && mouseY >= y
                    && mouseY <= y + height;
        }
    }
}