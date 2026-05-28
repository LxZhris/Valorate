package com.example.client.systems.ui;

import com.example.client.systems.config.ConfigManager;
import com.example.client.systems.modules.Category;
import com.example.client.systems.modules.Module;
import com.example.client.systems.modules.ModuleManager;
import com.example.client.systems.settings.BooleanSetting;
import com.example.client.systems.settings.ButtonSetting;
import com.example.client.systems.settings.ModeSetting;
import com.example.client.systems.settings.NumberSetting;
import com.example.client.systems.settings.Setting;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import java.lang.reflect.Method;

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
    private String hoveredDescription;

    private static final int PANEL_WIDTH = 128;
    private static final int HEADER_HEIGHT = 20;
    private static final int MODULE_HEIGHT = 17;
    private static final int MAX_PANEL_HEIGHT = 360;

    public ClickGui() {
        super(Component.literal("Valorate ClickGUI"));

        if (panels.isEmpty()) {
            int startX = 20;

            for (Category category : Category.values()) {
                panels.put(category, new Panel(category, startX, 25));
                startX += PANEL_WIDTH + 10;
            }
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        Minecraft client = Minecraft.getInstance();
        hoveredDescription = null;

        boolean leftDown = GLFW.glfwGetMouseButton(GLFW.glfwGetCurrentContext(), GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        boolean rightDown = GLFW.glfwGetMouseButton(GLFW.glfwGetCurrentContext(), GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;

        if (leftDown && draggingSlider != null) {
            double percent = (mouseX - draggingSliderX) / (double) draggingSliderWidth;
            percent = Math.max(0.0, Math.min(1.0, percent));

            double value = draggingSlider.getMin() + (draggingSlider.getMax() - draggingSlider.getMin()) * percent;
            draggingSlider.setValue(value);
            ConfigManager.save();
        }

        if (!leftDown) draggingSlider = null;

        if (leftDown && !leftWasDown) handleClick(mouseX, mouseY, 0);
        if (rightDown && !rightWasDown) handleClick(mouseX, mouseY, 1);

        leftWasDown = leftDown;
        rightWasDown = rightDown;

        if (leftDown && draggingPanel != null) {
            draggingPanel.x = mouseX - dragOffsetX;
            draggingPanel.y = mouseY - dragOffsetY;
        }

        if (!leftDown) draggingPanel = null;

        for (Panel panel : panels.values()) {
            renderPanel(g, client, panel, mouseX, mouseY);
        }

        renderDescriptionTooltip(g, client, mouseX, mouseY);
    }

    private void renderPanel(GuiGraphicsExtractor g, Minecraft client, Panel panel, int mouseX, int mouseY) {
        int headerColor = 0xDD0A0A0D;
        int bodyColor = 0xCC111116;
        int lineColor = 0x55333333;
        int accent = 0xFFFFDD00;
        int text = 0xFFFFFFFF;
        int hover = 0x33FFDD00;

        boolean headerHovered = isInside(mouseX, mouseY, panel.x, panel.y, panel.width, HEADER_HEIGHT);

        g.fill(panel.x, panel.y, panel.x + panel.width, panel.y + HEADER_HEIGHT, headerHovered ? 0xAAFFDD00 : headerColor);

        renderCategoryIcon(g, client, panel.category, panel.x + 7, panel.y + 2);

        g.text(client.font, Component.literal(panel.category.name()).withStyle(ChatFormatting.YELLOW),
                panel.x + 30, panel.y + 6, headerHovered ? 0xFF000000 : accent, false);

        g.text(client.font, Component.literal(panel.open ? "-" : "+"),
                panel.x + panel.width - 12, panel.y + 6, headerHovered ? 0xFF000000 : accent, false);

        if (!panel.open) return;

        int contentHeight = getPanelContentHeight(panel);
        int visibleHeight = Math.min(contentHeight, MAX_PANEL_HEIGHT);
        panel.scroll = Math.max(0, Math.min(panel.scroll, Math.max(0, contentHeight - visibleHeight)));

        int bodyY = panel.y + HEADER_HEIGHT;
        g.fill(panel.x, bodyY, panel.x + panel.width, bodyY + visibleHeight, 0xBB07070A);

        int yOff = bodyY - panel.scroll;

        for (Module module : ModuleManager.MODULES) {
            if (module.getCategory() != panel.category) continue;
            if (module.getName().equalsIgnoreCase("ClickGUI")) continue;

            boolean opened = moduleOpen.getOrDefault(module, false);
            boolean moduleVisible = yOff + MODULE_HEIGHT >= bodyY && yOff <= bodyY + visibleHeight;

            if (moduleVisible) {
                boolean moduleHovered = isInside(mouseX, mouseY, panel.x, yOff, panel.width, MODULE_HEIGHT);

                if (moduleHovered) hoveredDescription = module.getDescription();

                g.fill(panel.x, yOff, panel.x + panel.width, yOff + MODULE_HEIGHT, moduleHovered || opened ? hover : bodyColor);

                int moduleColor = module.isEnabled() ? rainbow(yOff * 30) : text;
                g.text(client.font, Component.literal(module.getName()), panel.x + 7, yOff + 5, moduleColor, false);
            }

            yOff += MODULE_HEIGHT;

            if (opened) {
                int settingsHeight = getSettingsHeight(module);

                if (yOff + settingsHeight >= bodyY && yOff <= bodyY + visibleHeight) {
                    g.fill(panel.x, Math.max(yOff, bodyY), panel.x + panel.width, Math.min(yOff + settingsHeight, bodyY + visibleHeight), 0xCC0B0B10);

                    int settingY = yOff + 6;

                    if (!hasVisibleSettings(module)) {
                        if (settingY >= bodyY && settingY <= bodyY + visibleHeight) {
                            g.text(client.font, Component.literal("No settings"), panel.x + 8, settingY, 0xFF999999, false);
                        }
                    } else {
                        for (Setting<?> setting : module.getSettings()) {
                            if (!setting.isVisible()) continue;

                            int h = getSettingHeight(setting);

                            if (settingY + h >= bodyY && settingY <= bodyY + visibleHeight) {
                                renderSetting(g, client, setting, panel.x + 8, settingY, panel.width - 16);
                                drawSeparator(g, panel, settingY + h);
                            }

                            settingY += h + 5;
                        }
                    }

                    int keyY = yOff + settingsHeight - 26;
                    if (keyY >= bodyY && keyY <= bodyY + visibleHeight) {
                        renderKeybind(g, client, module, panel.x + 8, keyY, panel.width - 16);
                    }
                }

                yOff += settingsHeight;
            }

            if (yOff >= bodyY && yOff <= bodyY + visibleHeight) {
                g.fill(panel.x, yOff - 1, panel.x + panel.width, yOff, lineColor);
            }
        }

        if (contentHeight > visibleHeight) {
            int barH = Math.max(24, (int) ((visibleHeight / (double) contentHeight) * visibleHeight));
            int barY = bodyY + (int) ((panel.scroll / (double) Math.max(1, contentHeight - visibleHeight)) * (visibleHeight - barH));

            g.fill(panel.x + panel.width - 3, bodyY, panel.x + panel.width - 1, bodyY + visibleHeight, 0x55111111);
            g.fill(panel.x + panel.width - 3, barY, panel.x + panel.width - 1, barY + barH, accent);
        }
    }

    private void renderCategoryIcon(GuiGraphicsExtractor g, Minecraft client, Category category, int x, int y) {
        ItemStack stack = switch (category.name().toLowerCase()) {
            case "combat" -> new ItemStack(Items.DIAMOND_SWORD);
            case "movement" -> new ItemStack(Items.FEATHER);
            case "player" -> new ItemStack(Items.PLAYER_HEAD);
            case "render" -> new ItemStack(Items.ENDER_EYE);
            case "world" -> new ItemStack(Items.GRASS_BLOCK);
            case "miscellaneous", "misc" -> new ItemStack(Items.COMPASS);
            default -> new ItemStack(Items.BARRIER);
        };

        renderItemStack(g, stack, x, y);
    }

    private void renderItemStack(GuiGraphicsExtractor g, ItemStack stack, int x, int y) {
        try {
            for (Method method : g.getClass().getMethods()) {
                String name = method.getName().toLowerCase();

                if (!name.contains("item")) continue;

                Class<?>[] params = method.getParameterTypes();

                if (params.length == 3
                        && params[0].isAssignableFrom(ItemStack.class)
                        && params[1] == int.class
                        && params[2] == int.class) {
                    method.invoke(g, stack, x, y);
                    return;
                }

                if (params.length == 4
                        && params[0].isAssignableFrom(ItemStack.class)
                        && params[1] == int.class
                        && params[2] == int.class
                        && params[3] == int.class) {
                    method.invoke(g, stack, x, y, 0);
                    return;
                }
            }
        } catch (Exception ignored) {
        }

        g.fill(x, y, x + 10, y + 10, 0xFF111111);
        g.fill(x + 2, y + 2, x + 8, y + 8, 0xFFFFDD00);
    }

    private void renderDescriptionTooltip(GuiGraphicsExtractor g, Minecraft client, int mouseX, int mouseY) {
        if (hoveredDescription == null || hoveredDescription.isEmpty()) return;

        int padding = 5;
        int x = mouseX + 10;
        int y = mouseY + 10;
        int width = client.font.width(hoveredDescription) + padding * 2;
        int height = 16;

        g.fill(x, y, x + width, y + height, 0xEE08080A);
        g.fill(x, y, x + width, y + 1, 0xFFFFDD00);
        g.fill(x, y + height - 1, x + width, y + height, 0x55333333);

        g.text(client.font, Component.literal(hoveredDescription), x + padding, y + 5, 0xFFCCCCCC, false);
    }

    private void renderKeybind(GuiGraphicsExtractor g, Minecraft client, Module module, int x, int y, int width) {
        String keyText;

        if (bindingModule == module) {
            keyText = "Press key...";
        } else if (module.getKey() == 0) {
            keyText = "[...]";
        } else {
            keyText = GLFW.glfwGetKeyName(module.getKey(), 0);
            if (keyText == null) keyText = "Key " + module.getKey();
            keyText = keyText.toUpperCase();
        }

        g.text(client.font, Component.literal("Keybind:"), x, y + 5, 0xFFFFFFFF, false);
        g.text(client.font, Component.literal(keyText), x + width - client.font.width(keyText), y + 5, 0xFFFFDD00, false);
    }

    private void renderSetting(GuiGraphicsExtractor g, Minecraft client, Setting<?> setting, int x, int y, int width) {
        int accent = 0xFFFFDD00;
        int text = 0xFFFFFFFF;
        int muted = 0xFF999999;

        if (setting instanceof ButtonSetting button) {
            g.text(client.font, Component.literal(setting.getName()), x, y + 4, text, false);

            String value = button.get();
            g.text(client.font, Component.literal(value), x + width - client.font.width(value), y + 4, accent, false);
            return;
        }

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
            if (isInside(mouseX, mouseY, panel.x, panel.y, panel.width, HEADER_HEIGHT)) {
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

            int bodyY = panel.y + HEADER_HEIGHT;
            int contentHeight = getPanelContentHeight(panel);
            int visibleHeight = Math.min(contentHeight, MAX_PANEL_HEIGHT);

            if (!isInside(mouseX, mouseY, panel.x, bodyY, panel.width, visibleHeight)) continue;

            int yOff = bodyY - panel.scroll;

            for (Module module : ModuleManager.MODULES) {
                if (module.getCategory() != panel.category) continue;
                if (module.getName().equalsIgnoreCase("ClickGUI")) continue;

                boolean opened = moduleOpen.getOrDefault(module, false);

                if (isInside(mouseX, mouseY, panel.x, yOff, panel.width, MODULE_HEIGHT)) {
                    if (button == 0) {
                        module.toggle();
                        ConfigManager.save();
                    }

                    if (button == 1) {
                        moduleOpen.put(module, !opened);
                    }

                    return true;
                }

                yOff += MODULE_HEIGHT;

                if (opened) {
                    int settingY = yOff + 6;

                    for (Setting<?> setting : module.getSettings()) {
                        if (!setting.isVisible()) continue;

                        int h = getSettingHeight(setting);

                        if (isInside(mouseX, mouseY, panel.x + 8, settingY, panel.width - 16, h)) {
                            handleSettingClick(setting, mouseX, mouseY, panel.x + 8, settingY, panel.width - 16, button);
                            return true;
                        }

                        settingY += h + 5;
                    }

                    int keyY = yOff + getSettingsHeight(module) - 26;

                    if (isInside(mouseX, mouseY, panel.x + 8, keyY, panel.width - 16, 22)) {
                        if (button == 0) bindingModule = module;
                        return true;
                    }

                    yOff += getSettingsHeight(module);
                }
            }
        }

        return true;
    }

    private void handleSettingClick(Setting<?> setting, double mouseX, double mouseY, int x, int y, int width, int button) {
        if (!setting.isVisible()) return;

        if (setting instanceof ButtonSetting buttonSetting) {
            if (button == 0) {
                buttonSetting.press();
                ConfigManager.save();
            }

            return;
        }

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

    public boolean mouseScrolled(double mouseX, double mouseY, double scrollY) {
        for (Panel panel : panels.values()) {
            if (!panel.open) continue;

            int bodyY = panel.y + HEADER_HEIGHT;
            int contentHeight = getPanelContentHeight(panel);
            int visibleHeight = Math.min(contentHeight, MAX_PANEL_HEIGHT);

            if (!isInside(mouseX, mouseY, panel.x, bodyY, panel.width, visibleHeight)) continue;

            panel.scroll -= (int) (scrollY * 22);

            if (panel.scroll < 0) panel.scroll = 0;

            int maxScroll = Math.max(0, contentHeight - visibleHeight);
            if (panel.scroll > maxScroll) panel.scroll = maxScroll;

            return true;
        }

        return true;
    }

    private boolean hasVisibleSettings(Module module) {
        for (Setting<?> setting : module.getSettings()) {
            if (setting.isVisible()) return true;
        }

        return false;
    }

    private int getSettingHeight(Setting<?> setting) {
        if (setting instanceof ButtonSetting) return 18;

        if (setting instanceof ModeSetting mode) {
            if (mode.isOpen()) {
                return 44 + mode.getModes().length * 15;
            }

            return 28;
        }

        if (setting instanceof NumberSetting) return 30;

        return 18;
    }

    private int getSettingsHeight(Module module) {
        int height = 6;

        if (!hasVisibleSettings(module)) {
            height += 18;
        } else {
            for (Setting<?> setting : module.getSettings()) {
                if (!setting.isVisible()) continue;

                height += getSettingHeight(setting);
                height += 5;
            }
        }

        height += 22;
        height += 4;

        return height;
    }

    private int getPanelContentHeight(Panel panel) {
        int height = 0;

        for (Module module : ModuleManager.MODULES) {
            if (module.getCategory() != panel.category) continue;
            if (module.getName().equalsIgnoreCase("ClickGUI")) continue;

            height += MODULE_HEIGHT;

            if (moduleOpen.getOrDefault(module, false)) {
                height += getSettingsHeight(module);
            }
        }

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
        private final int width = PANEL_WIDTH;
        private boolean open = false;
        private int scroll = 0;

        private Panel(Category category, int x, int y) {
            this.category = category;
            this.x = x;
            this.y = y;
        }
    }
}