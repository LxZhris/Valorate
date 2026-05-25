package com.example.client.systems.ui;

import com.example.client.systems.config.ConfigManager;
import com.example.client.systems.modules.Category;
import com.example.client.systems.modules.Module;
import com.example.client.systems.modules.ModuleManager;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;



import java.awt.Color;
import java.util.HashMap;
import java.util.Map;


public class ClickGui extends Screen {
    private static final Map<Category, Panel> panels = new HashMap<>();
    private final Map<Module, Boolean> moduleOpen = new HashMap<>();

    private Panel draggingPanel;
    private int dragOffsetX;
    private int dragOffsetY;

    private boolean leftWasDown;
    private boolean rightWasDown;

    public ClickGui() {
        super(Component.literal("Valorate ClickGUI"));

        if (panels.isEmpty()) {
            int startX = 20;

            for (Category category : Category.values()) {
                panels.put(category, new Panel(category, startX, 25));
                startX += 105;
            }
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        Minecraft client = Minecraft.getInstance();

        boolean leftDown = GLFW.glfwGetMouseButton(GLFW.glfwGetCurrentContext(), GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        boolean rightDown = GLFW.glfwGetMouseButton(
                GLFW.glfwGetCurrentContext(),
                GLFW.GLFW_MOUSE_BUTTON_RIGHT
        ) == GLFW.GLFW_PRESS;

        if (leftDown && !leftWasDown) {
            handleClick(mouseX, mouseY, 0);
        }

        if (rightDown && !rightWasDown) {
            handleClick(mouseX, mouseY, 1);
        }

        leftWasDown = leftDown;
        rightWasDown = rightDown;

        if (leftDown && draggingPanel != null) {
            draggingPanel.x = mouseX - dragOffsetX;
            draggingPanel.y = mouseY - dragOffsetY;
        }

        if (!leftDown) {
            draggingPanel = null;
        }

        for (Panel panel : panels.values()) {
            renderPanel(g, client, panel, mouseX, mouseY);
        }
    }

    private void renderPanel(GuiGraphicsExtractor g, Minecraft client, Panel panel, int mouseX, int mouseY) {
        int headerColor = 0xDD0A0A0D;
        int bodyColor = 0xCC111116;
        int lineColor = 0x55333333;
        int accent = 0xFFFFDD00;
        int text = 0xFFFFFFFF;
        int hover = 0x33FFDD00;

        int panelWidth = panel.width;
        int headerHeight = 18;
        int moduleHeight = 16;

        boolean headerHovered = isInside(mouseX, mouseY, panel.x, panel.y, panelWidth, headerHeight);

        g.fill(panel.x, panel.y, panel.x + panelWidth, panel.y + headerHeight, headerHovered ? 0xAAFFDD00 : headerColor);

        g.text(
                client.font,
                Component.literal(panel.category.name()).withStyle(ChatFormatting.YELLOW),
                panel.x + 5,
                panel.y + 5,
                headerHovered ? 0xFF000000 : accent,
                false
        );

        g.text(
                client.font,
                Component.literal(panel.open ? "-" : "+"),
                panel.x + panelWidth - 10,
                panel.y + 5,
                headerHovered ? 0xFF000000 : accent,
                false
        );

        if (!panel.open) {
            return;
        }

        int yOff = panel.y + headerHeight;

        for (Module module : ModuleManager.MODULES) {
            if (module.getCategory() != panel.category) continue;
            if (module.getName().equalsIgnoreCase("ClickGUI")) continue;

            boolean moduleHovered = isInside(mouseX, mouseY, panel.x, yOff, panelWidth, moduleHeight);
            boolean opened = moduleOpen.getOrDefault(module, false);

            g.fill(panel.x, yOff, panel.x + panelWidth, yOff + moduleHeight, moduleHovered || opened ? hover : bodyColor);

            int moduleColor = module.isEnabled() ? rainbow(yOff * 30) : text;

            g.text(
                    client.font,
                    Component.literal(module.getName()),
                    panel.x + 5,
                    yOff + 4,
                    moduleColor,
                    false
            );

            yOff += moduleHeight;

            if (opened) {
                g.fill(panel.x, yOff, panel.x + panelWidth, yOff + 24, 0xAA08080B);

                g.text(
                        client.font,
                        Component.literal("Settings..."),
                        panel.x + 8,
                        yOff + 8,
                        0xFF999999,
                        false
                );

                yOff += 24;
            }

            g.fill(panel.x, yOff - 1, panel.x + panelWidth, yOff, lineColor);
        }
    }

    private boolean handleClick(double mouseX, double mouseY, int button) {
        for (Panel panel : panels.values()) {
            if (isInside(mouseX, mouseY, panel.x, panel.y, panel.width, 18)) {
                if (button == 0) {
                    draggingPanel = panel;
                    dragOffsetX = (int) mouseX - panel.x;
                    dragOffsetY = (int) mouseY - panel.y;
                }

                if (button == 1) {
                    panel.open = !panel.open;
                }

                return true;
            }

            if (!panel.open) continue;

            int yOff = panel.y + 18;

            for (Module module : ModuleManager.MODULES) {
                if (module.getCategory() != panel.category) continue;
                if (module.getName().equalsIgnoreCase("ClickGUI")) continue;

                if (isInside(mouseX, mouseY, panel.x, yOff, panel.width, 16)) {
                    if (button == 0) {
                        module.toggle();
                        ConfigManager.save();
                    }

                    if (button == 1) {
                        moduleOpen.put(module, !moduleOpen.getOrDefault(module, false));
                    }

                    return true;
                }

                yOff += 16;

                if (moduleOpen.getOrDefault(module, false)) {
                    yOff += 24;
                }
            }
        }

        return true;
    }

    private int rainbow(int offset) {
        float hue = ((System.currentTimeMillis() + offset) % 6000L) / 6000.0F;
        return Color.HSBtoRGB(hue, 0.85F, 1.0F);
    }

    private boolean isInside(double mouseX, double mouseY, int x, int y, int w, int h) {
        return mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
    }

    private static class Panel {
        private final Category category;
        private int x;
        private int y;
        private final int width = 95;
        private boolean open = false;

        private Panel(Category category, int x, int y) {
            this.category = category;
            this.x = x;
            this.y = y;
        }
    }


    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_RIGHT_SHIFT
                || keyCode == GLFW.GLFW_KEY_ESCAPE) {

            Minecraft.getInstance().setScreen(null);

            ConfigManager.save();

            return true;
        }

        return false;
    }


    public void toggle() {
        Minecraft client = Minecraft.getInstance();

        if (client.screen instanceof com.example.client.systems.ui.ClickGui) {
            client.setScreen(null);
            return;
        }

        client.setScreen(new com.example.client.systems.ui.ClickGui());
    }
}