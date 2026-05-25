package com.example.client.systems.modules.render;

import com.example.client.systems.modules.Module;
import com.example.client.systems.modules.ModuleManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

import java.awt.Color;
import java.util.Comparator;
import java.util.List;

public class ArraylistRenderer {

    public static void render(GuiGraphicsExtractor extractor) {
        if (!ModuleManager.HUD.isEnabled()) {
            return;
        }

        Minecraft client = Minecraft.getInstance();

        List<Module> enabledModules = ModuleManager.MODULES.stream()
                .filter(Module::isEnabled)
                .filter(Module::isVisibleInArraylist)
                .sorted(Comparator.comparingInt(module -> -client.font.width(module.getName())))
                .toList();

        int y = 8;

        for (int i = 0; i < enabledModules.size(); i++) {
            Module module = enabledModules.get(i);

            int color = getRainbowColor(i * 250);
            int textWidth = client.font.width(module.getName());

            int x = extractor.guiWidth() - textWidth - 8;

            extractor.text(
                    client.font,
                    Component.literal(module.getName()),
                    x,
                    y,
                    color,
                    true
            );

            y += 10;
        }
    }

    private static int getRainbowColor(int offset) {
        float hue = ((System.currentTimeMillis() + offset) % 6000L) / 6000.0F;
        return Color.HSBtoRGB(hue, 0.8F, 1.0F);
    }
}