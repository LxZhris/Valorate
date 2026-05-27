package com.example.client.systems.modules.render;

import com.example.client.systems.modules.Module;
import com.example.client.systems.modules.ModuleManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class NotificationRenderer {
    private static final List<Notify> NOTIFICATIONS = new ArrayList<>();

    public static void add(Module module, boolean enabled) {
        if (ModuleManager.NOTIFICATIONS == null || !ModuleManager.NOTIFICATIONS.isEnabled()) {
            return;
        }

        NOTIFICATIONS.add(0, new Notify(module.getName(), module.getCategory().name(), enabled));

        while (NOTIFICATIONS.size() > ModuleManager.NOTIFICATIONS.getMaxAmount()) {
            NOTIFICATIONS.remove(NOTIFICATIONS.size() - 1);
        }

        if (ModuleManager.NOTIFICATIONS.shouldPlaySound()) {
            Minecraft mc = Minecraft.getInstance();

            if (mc.player != null) {
                mc.player.playSound(
                        net.minecraft.sounds.SoundEvents.EXPERIENCE_ORB_PICKUP,
                        0.45F,
                        1.8F
                );
            }
        }
    }

    public static void render(GuiGraphicsExtractor g) {
        Minecraft mc = Minecraft.getInstance();

        if (mc.player == null || ModuleManager.NOTIFICATIONS == null || !ModuleManager.NOTIFICATIONS.isEnabled()) {
            return;
        }

        int screenW = mc.getWindow().getGuiScaledWidth();
        int screenH = mc.getWindow().getGuiScaledHeight();

        int width = 145;
        int height = 34;
        int spacing = 6;

        double duration = ModuleManager.NOTIFICATIONS.getDurationSeconds() * 1000.0;

        Iterator<Notify> iterator = NOTIFICATIONS.iterator();
        int index = 0;

        while (iterator.hasNext()) {
            Notify n = iterator.next();

            long alive = System.currentTimeMillis() - n.created;

            if (alive > duration + 350) {
                iterator.remove();
                continue;
            }

            double progress;

            if (alive < 300) {
                progress = alive / 300.0;
            } else if (alive > duration) {
                progress = 1.0 - ((alive - duration) / 350.0);
            } else {
                progress = 1.0;
            }

            progress = Math.max(0.0, Math.min(1.0, progress));

            double eased = 1.0 - Math.pow(1.0 - progress, 3.0);

            int targetX = screenW - width - 8;
            int hiddenX = screenW + 8;
            int x = (int) (hiddenX + (targetX - hiddenX) * eased);
            int y = screenH - 48 - index * (height + spacing);

            int accent = n.enabled ? 0xFF36FF65 : 0xFFFF4040;
            int bg = 0xDD0B0B10;

            g.fill(x, y, x + width, y + height, bg);
            g.fill(x, y, x + 3, y + height, accent);

            g.text(
                    mc.font,
                    Component.literal(n.enabled ? "Enabled" : "Disabled"),
                    x + 9,
                    y + 5,
                    accent,
                    false
            );

            g.text(
                    mc.font,
                    Component.literal(n.name),
                    x + 9,
                    y + 16,
                    rainbow(index * 350),
                    false
            );

            String category = n.category;
            g.text(
                    mc.font,
                    Component.literal(category),
                    x + width - mc.font.width(category) - 8,
                    y + 16,
                    0xFF999999,
                    false
            );

            index++;
        }
    }

    private static int rainbow(int offset) {
        float hue = ((System.currentTimeMillis() + offset) % 4000L) / 4000.0F;
        return java.awt.Color.HSBtoRGB(hue, 0.85F, 1.0F);
    }

    private static class Notify {
        private final String name;
        private final String category;
        private final boolean enabled;
        private final long created;

        private Notify(String name, String category, boolean enabled) {
            this.name = name;
            this.category = category;
            this.enabled = enabled;
            this.created = System.currentTimeMillis();
        }
    }
}