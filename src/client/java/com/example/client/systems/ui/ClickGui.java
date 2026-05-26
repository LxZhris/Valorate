package com.example.client.systems.ui;

import com.example.client.systems.config.ConfigManager;
import com.example.client.systems.modules.Category;
import com.example.client.systems.modules.Module;
import com.example.client.systems.modules.ModuleManager;
import com.example.client.systems.settings.BooleanSetting;
import com.example.client.systems.settings.ModeSetting;
import com.example.client.systems.settings.NumberSetting;
import com.example.client.systems.settings.Setting;
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
    private static final Map<Module, Boolean> moduleOpen = new HashMap<>();

    private Panel draggingPanel;
    private int dragOffsetX;
    private int dragOffsetY;

    private boolean leftWasDown;
    private boolean rightWasDown;

    private NumberSetting draggingSlider;
    private int draggingSliderX;
    private int draggingSliderWidth;

    private Module bindingModule;

    public ClickGui() {
        super(Component.literal("Valorate ClickGUI"));

        if (panels.isEmpty()) {
            int startX = 20;

            for (Category category : Category.values()) {
                panels.put(category, new Panel(category, startX, 25));
                startX += 110;
            }
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        Minecraft client = Minecraft.getInstance();

        boolean leftDown = GLFW.glfwGetMouseButton(GLFW.glfwGetCurrentContext(), GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        boolean rightDown = GLFW.glfwGetMouseButton(GLFW.glfwGetCurrentContext(), GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;

        if (leftDown && draggingSlider != null) {
            double percent = (mouseX - draggingSliderX) / (double) draggingSliderWidth;
            percent = Math.max(0.0, Math.min(1.0, percent));

            double value = draggingSlider.getMin() + (draggingSlider.getMax() - draggingSlider.getMin()) * percent;
            draggingSlider.setValue(value);
            ConfigManager.save();
        }

        if (!leftDown) {
            draggingSlider = null;
        }

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

        g.text(client.font, Component.literal(panel.category.name()).withStyle(ChatFormatting.YELLOW),
                panel.x + 5, panel.y + 5, headerHovered ? 0xFF000000 : accent, false);

        g.text(client.font, Component.literal(panel.open ? "-" : "+"),
                panel.x + panelWidth - 10, panel.y + 5, headerHovered ? 0xFF000000 : accent, false);

        if (!panel.open) return;

        int yOff = panel.y + headerHeight;

        for (Module module : ModuleManager.MODULES) {
            if (module.getCategory() != panel.category) continue;
            if (module.getName().equalsIgnoreCase("ClickGUI")) continue;

            boolean moduleHovered = isInside(mouseX, mouseY, panel.x, yOff, panelWidth, moduleHeight);
            boolean opened = moduleOpen.getOrDefault(module, false);

            g.fill(panel.x, yOff, panel.x + panelWidth, yOff + moduleHeight, moduleHovered || opened ? hover : bodyColor);

            int moduleColor = module.isEnabled() ? rainbow(yOff * 30) : text;

            g.text(client.font, Component.literal(module.getName()),
                    panel.x + 5, yOff + 4, moduleColor, false);

            yOff += moduleHeight;

            if (opened) {
                int settingsHeight = getSettingsHeight(module);
                g.fill(panel.x, yOff, panel.x + panelWidth, yOff + settingsHeight, 0xCC0B0B10);

                int settingY = yOff + 6;

                if (module.getSettings().isEmpty()) {
                    g.text(client.font, Component.literal("No settings"), panel.x + 8, settingY, 0xFF999999, false);
                    settingY += 18;
                } else {
                    for (int i = 0; i < module.getSettings().size(); i++) {
                        Setting<?> setting = module.getSettings().get(i);

                        renderSetting(g, client, setting, panel.x + 8, settingY, panelWidth - 16);
                        settingY += getSettingHeight(setting);

                        boolean nextIsBoolean = i + 1 < module.getSettings().size()
                                && module.getSettings().get(i + 1) instanceof BooleanSetting;

                        boolean currentIsBoolean = setting instanceof BooleanSetting;

                        if (!currentIsBoolean || !nextIsBoolean) {
                            drawSeparator(g, panel, settingY);
                            settingY += 5;
                        }
                    }
                }

                renderKeybind(g, client, module, panel.x + 8, settingY, panelWidth - 16);
                settingY += 22;

                yOff += settingsHeight;
            }

            g.fill(panel.x, yOff - 1, panel.x + panelWidth, yOff, lineColor);
        }
    }

    private void renderKeybind(GuiGraphicsExtractor g, Minecraft client, Module module, int x, int y, int width) {
        int text = 0xFFFFFFFF;
        int accent = 0xFFFFDD00;

        String keyText;

        if (bindingModule == module) {
            keyText = "Press key...";
        } else if (module.getKey() == 0) {
            keyText = "[...]";
        } else {
            keyText = GLFW.glfwGetKeyName(module.getKey(), 0);

            if (keyText == null) {
                keyText = "Key " + module.getKey();
            }

            keyText = keyText.toUpperCase();
        }

        g.text(client.font, Component.literal("Keybind:"), x, y + 5, text, false);
        g.text(client.font, Component.literal(keyText), x + width - client.font.width(keyText), y + 5, accent, false);
    }

    private void renderSetting(GuiGraphicsExtractor g, Minecraft client, Setting<?> setting, int x, int y, int width) {
        int accent = 0xFFFFDD00;
        int text = 0xFFFFFFFF;
        int muted = 0xFF999999;

        if (setting instanceof BooleanSetting bool) {
            g.text(client.font, Component.literal(setting.getName()), x, y + 4, text, false);

            int boxX = x + width - 14;
            int boxY = y + 3;

            g.fill(boxX, boxY, boxX + 10, boxY + 10, 0xFF111111);

            if (bool.get()) {
                g.fill(boxX + 2, boxY + 2, boxX + 8, boxY + 8, accent);
            }

            return;
        }

        if (setting instanceof ModeSetting mode) {
            g.text(client.font, Component.literal(setting.getName()), x, y + 3, text, false);
            g.text(client.font, Component.literal(mode.get()), x, y + 15, accent, false);

            if (mode.isOpen()) {
                int optionY = y + 31;

                for (String option : mode.getModes()) {
                    boolean selected = option.equalsIgnoreCase(mode.get());

                    g.fill(x + 6, optionY - 2, x + width - 6, optionY + 12, selected ? 0x22111111 : 0x66111111);
                    g.text(client.font, Component.literal(option), x + 12, optionY + 1, selected ? accent : muted, false);

                    optionY += 15;
                }
            }

            return;
        }

        if (setting instanceof NumberSetting number) {
            double value = number.get();
            double min = number.getMin();
            double max = number.getMax();

            double percent = (value - min) / (max - min);
            percent = Math.max(0.0, Math.min(1.0, percent));

            String valueText = String.format("%.2f", value);

            g.text(client.font, Component.literal(setting.getName() + ": " + valueText), x, y, text, false);

            int sliderX = x;
            int sliderY = y + 16;
            int sliderW = width - 6;
            int filled = (int) (sliderW * percent);

            g.fill(sliderX, sliderY, sliderX + sliderW, sliderY + 3, 0xFF333333);
            g.fill(sliderX, sliderY, sliderX + filled, sliderY + 3, accent);
            g.fill(sliderX + filled - 3, sliderY - 4, sliderX + filled + 3, sliderY + 7, accent);
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

                boolean opened = moduleOpen.getOrDefault(module, false);

                if (isInside(mouseX, mouseY, panel.x, yOff, panel.width, 16)) {
                    if (button == 0) {
                        module.toggle();
                        ConfigManager.save();
                    }

                    if (button == 1) {
                        moduleOpen.put(module, !opened);
                    }

                    return true;
                }

                yOff += 16;

                if (opened) {
                    int settingY = yOff + 6;

                    for (int i = 0; i < module.getSettings().size(); i++) {
                        Setting<?> setting = module.getSettings().get(i);
                        int h = getSettingHeight(setting);

                        if (isInside(mouseX, mouseY, panel.x + 8, settingY, panel.width - 16, h)) {
                            handleSettingClick(setting, mouseX, mouseY, panel.x + 8, settingY, panel.width - 16, button);
                            return true;
                        }

                        settingY += h;

                        boolean nextIsBoolean = i + 1 < module.getSettings().size()
                                && module.getSettings().get(i + 1) instanceof BooleanSetting;

                        boolean currentIsBoolean = setting instanceof BooleanSetting;

                        if (!currentIsBoolean || !nextIsBoolean) {
                            settingY += 5;
                        }
                    }

                    if (isInside(mouseX, mouseY, panel.x + 8, settingY, panel.width - 16, 22)) {
                        if (button == 0) {
                            bindingModule = module;
                        }

                        return true;
                    }

                    yOff += getSettingsHeight(module);
                }
            }
        }

        return true;
    }

    private void handleSettingClick(Setting<?> setting, double mouseX, double mouseY, int x, int y, int width, int button) {
        if (setting instanceof BooleanSetting bool) {
            if (button == 0) {
                bool.toggle();
                ConfigManager.save();
            }

            return;
        }

        if (setting instanceof ModeSetting mode) {
            if (button == 0) {
                if (!mode.isOpen()) {
                    mode.toggleOpen();
                    return;
                }

                int optionY = y + 31;

                for (String option : mode.getModes()) {
                    if (isInside(mouseX, mouseY, x + 6, optionY - 2, width - 12, 14)) {
                        mode.setMode(option);
                        ConfigManager.save();
                        return;
                    }

                    optionY += 15;
                }

                mode.toggleOpen();
            }

            return;
        }

        if (setting instanceof NumberSetting number) {
            int sliderX = x;
            int sliderY = y + 16;
            int sliderW = width - 6;

            boolean onSlider = isInside(mouseX, mouseY, sliderX, sliderY - 5, sliderW, 13);

            if (button == 0 && onSlider) {
                draggingSlider = number;
                draggingSliderX = sliderX;
                draggingSliderWidth = sliderW;

                double percent = (mouseX - draggingSliderX) / (double) draggingSliderWidth;
                percent = Math.max(0.0, Math.min(1.0, percent));

                double value = number.getMin() + (number.getMax() - number.getMin()) * percent;
                number.setValue(value);

                ConfigManager.save();
            }
        }
    }

    private int getSettingHeight(Setting<?> setting) {
        if (setting instanceof ModeSetting mode) {
            if (mode.isOpen()) {
                return 44 + mode.getModes().length * 15;
            }

            return 28;
        }

        if (setting instanceof NumberSetting) {
            return 30;
        }

        return 18;
    }

    private int getSettingsHeight(Module module) {
        int height = 6;

        if (module.getSettings().isEmpty()) {
            height += 18;
        } else {
            for (int i = 0; i < module.getSettings().size(); i++) {
                Setting<?> setting = module.getSettings().get(i);

                height += getSettingHeight(setting);

                boolean nextIsBoolean = i + 1 < module.getSettings().size()
                        && module.getSettings().get(i + 1) instanceof BooleanSetting;

                boolean currentIsBoolean = setting instanceof BooleanSetting;

                if (!currentIsBoolean || !nextIsBoolean) {
                    height += 5;
                }
            }
        }

        height += 22;
        height += 4;

        return height;
    }

    private void drawSeparator(GuiGraphicsExtractor g, Panel panel, int y) {
        g.fill(panel.x + 6, y, panel.x + panel.width - 6, y + 1, 0x55333333);
    }

    private int rainbow(int offset) {
        float hue = ((System.currentTimeMillis() + offset) % 6000L) / 6000.0F;
        return Color.HSBtoRGB(hue, 0.85F, 1.0F);
    }

    private boolean isInside(double mouseX, double mouseY, int x, int y, int w, int h) {
        return mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (bindingModule != null) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                bindingModule.setKey(0);
            } else {
                bindingModule.setKey(keyCode);
            }

            bindingModule = null;
            ConfigManager.save();
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_RIGHT_SHIFT || keyCode == GLFW.GLFW_KEY_ESCAPE) {
            Minecraft.getInstance().setScreen(null);
            ConfigManager.save();
            return true;
        }

        return false;
    }

    private static class Panel {
        private final Category category;
        private int x;
        private int y;
        private final int width = 105;
        private boolean open = false;

        private Panel(Category category, int x, int y) {
            this.category = category;
            this.x = x;
            this.y = y;
        }
    }
}