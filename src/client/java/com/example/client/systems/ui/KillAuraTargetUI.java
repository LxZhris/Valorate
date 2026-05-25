package com.example.client.systems.ui;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class KillAuraTargetUI extends Screen {
    private int x;
    private int y;
    private final int width = 520;
    private final int height = 360;

    private boolean positioned;
    private boolean players = true;
    private boolean invisible = false;

    private String search = "";
    private boolean typingSearch = false;

    private static final Set<String> SELECTED = new HashSet<>();

    private final List<TargetEntry> entries = new ArrayList<>();

    public KillAuraTargetUI() {
        super(Component.literal("KillAura Target UI"));

        for (net.minecraft.world.entity.EntityType<?> type : net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE) {
            String id = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(type).getPath();
            String name = formatName(id);

            boolean friendly = type.getCategory().isFriendly();

            String group = friendly ? "Animals" : "Hostile";

            entries.add(new TargetEntry(group, name, type, friendly));
        }

    }

    private String formatName(String id) {
        String[] parts = id.split("_");
        StringBuilder builder = new StringBuilder();

        for (String part : parts) {
            if (part.isEmpty()) continue;

            builder.append(Character.toUpperCase(part.charAt(0)))
                    .append(part.substring(1))
                    .append(" ");
        }

        return builder.toString().trim();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        Minecraft client = Minecraft.getInstance();

        if (!positioned) {
            x = (g.guiWidth() - width) / 2;
            y = (g.guiHeight() - height) / 2;
            positioned = true;
        }

        int bg = 0xF0101014;
        int header = 0xF008080B;
        int panel = 0xAA17171D;
        int line = 0x55333333;
        int accent = 0xFFFFDD00;
        int hover = 0x33FFDD00;
        int text = 0xFFFFFFFF;
        int muted = 0xFF999999;

        g.fill(x, y, x + width, y + height, bg);
        g.fill(x, y, x + width, y + 34, header);

        g.text(client.font, Component.literal("KillAura Targets").withStyle(ChatFormatting.YELLOW), x + 14, y + 12, accent, false);

        drawToggle(g, client, "Players", players, x + width - 150, y + 11);
        drawToggle(g, client, "Invisible", invisible, x + width - 78, y + 11);

        int searchX = x + 20;
        int searchY = y + 52;
        int searchW = width - 40;

        g.fill(searchX, searchY, searchX + searchW, searchY + 22, panel);
        g.fill(searchX, searchY + 22, searchX + searchW, searchY + 23, typingSearch ? accent : line);

        String shownSearch = search.isEmpty() && !typingSearch ? "Search entities..." : search;
        g.text(client.font, Component.literal(shownSearch), searchX + 8, searchY + 7, search.isEmpty() ? muted : text, false);

        int btnY = y + 85;
        drawButton(g, client, "Select All", x + 20, btnY, 90, mouseX, mouseY);
        drawButton(g, client, "Deselect All", x + 118, btnY, 100, mouseX, mouseY);

        int listX = x + 20;
        int listY = y + 120;
        int listW = width - 40;

        renderGroup(g, client, "Animals", listX, listY, listW, mouseX, mouseY, true);
        listY = getNextGroupY("Animals", listY);

        renderGroup(g, client, "Monsters", listX, listY, listW, mouseX, mouseY, false);
    }

    private void renderGroup(GuiGraphicsExtractor g, Minecraft client, String group, int startX, int startY, int width, int mouseX, int mouseY, boolean friendly) {
        int accent = 0xFFFFDD00;
        int line = 0x55333333;
        int hover = 0x33FFDD00;
        int groupColor = friendly ? 0xFF55FF55 : 0xFFFF5555;

        boolean groupHovered = inside(mouseX, mouseY, startX, startY, width, 22);
        if (groupHovered) g.fill(startX, startY, startX + width, startY + 22, hover);

        g.text(client.font, Component.literal(group), startX + 8, startY + 7, accent, false);
        g.text(client.font, Component.literal(isWholeGroupSelected(group) ? "[x]" : "[ ]"), startX + width - 28, startY + 7, groupColor, false);
        g.fill(startX, startY + 24, startX + width, startY + 25, line);

        int yOff = startY + 30;

        for (TargetEntry entry : entries) {
            if (!entry.group.equals(group)) continue;
            if (!search.isEmpty() && !entry.name.toLowerCase().contains(search.toLowerCase())) continue;

            boolean hovered = inside(mouseX, mouseY, startX + 8, yOff, width - 16, 24);
            if (hovered) g.fill(startX + 8, yOff, startX + width - 8, yOff + 24, hover);

            g.item(new ItemStack(entry.friendly ? Items.WHEAT : Items.ROTTEN_FLESH), startX + 16, yOff + 4);
            g.text(
                    client.font,
                    Component.literal(entry.name),
                    startX + 40,
                    yOff + 8,
                    entry.friendly ? 0xFF55FF55 : 0xFFFF5555,
                    false
            );

            g.text(
                    client.font,
                    Component.literal(SELECTED.contains(entry.name) ? "[x]" : "[ ]"),
                    startX + width - 36,
                    yOff + 8,
                    entry.friendly ? 0xFF55FF55 : 0xFFFF5555,
                    false
            );

            yOff += 28;
        }
    }

    private int getNextGroupY(String group, int startY) {
        int yOff = startY + 30;

        for (TargetEntry entry : entries) {
            if (!entry.group.equals(group)) continue;
            if (!search.isEmpty() && !entry.name.toLowerCase().contains(search.toLowerCase())) continue;
            yOff += 28;
        }

        return yOff + 12;
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return false;

        if (inside(mouseX, mouseY, x + width - 150, y + 8, 65, 18)) {
            players = !players;
            return true;
        }

        if (inside(mouseX, mouseY, x + width - 78, y + 8, 68, 18)) {
            invisible = !invisible;
            return true;
        }

        if (inside(mouseX, mouseY, x + 20, y + 52, width - 40, 22)) {
            typingSearch = true;
            return true;
        } else {
            typingSearch = false;
        }

        if (inside(mouseX, mouseY, x + 20, y + 85, 90, 20)) {
            for (TargetEntry entry : entries) SELECTED.add(entry.name);
            return true;
        }

        if (inside(mouseX, mouseY, x + 118, y + 85, 100, 20)) {
            SELECTED.clear();
            return true;
        }

        int listX = x + 20;
        int listY = y + 120;
        int listW = width - 40;

        if (handleGroupClick("Animals", mouseX, mouseY, listX, listY, listW)) return true;
        listY = getNextGroupY("Animals", listY);

        return handleGroupClick("Monsters", mouseX, mouseY, listX, listY, listW);
    }

    private boolean handleGroupClick(String group, double mouseX, double mouseY, int startX, int startY, int width) {
        if (inside(mouseX, mouseY, startX, startY, width, 22)) {
            boolean select = !isWholeGroupSelected(group);

            for (TargetEntry entry : entries) {
                if (entry.group.equals(group)) {
                    if (select) SELECTED.add(entry.name);
                    else SELECTED.remove(entry.name);
                }
            }

            return true;
        }

        int yOff = startY + 30;

        for (TargetEntry entry : entries) {
            if (!entry.group.equals(group)) continue;
            if (!search.isEmpty() && !entry.name.toLowerCase().contains(search.toLowerCase())) continue;

            if (inside(mouseX, mouseY, startX + 8, yOff, width - 16, 24)) {
                if (SELECTED.contains(entry.name)) SELECTED.remove(entry.name);
                else SELECTED.add(entry.name);

                return true;
            }

            yOff += 28;
        }

        return false;
    }

    public boolean charTyped(char chr, int modifiers) {
        if (!typingSearch) return false;

        if (Character.isLetterOrDigit(chr) || chr == ' ') {
            search += chr;
            return true;
        }

        return false;
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!typingSearch) return false;

        if (keyCode == GLFW.GLFW_KEY_BACKSPACE && !search.isEmpty()) {
            search = search.substring(0, search.length() - 1);
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            typingSearch = false;
            return true;
        }

        return false;
    }

    private void drawToggle(GuiGraphicsExtractor g, Minecraft client, String name, boolean enabled, int x, int y) {
        g.text(client.font, Component.literal(name), x, y, enabled ? 0xFF55FF55 : 0xFFFF5555, false);
    }

    private void drawButton(GuiGraphicsExtractor g, Minecraft client, String name, int x, int y, int w, int mouseX, int mouseY) {
        int color = inside(mouseX, mouseY, x, y, w, 20) ? 0x55FFDD00 : 0xAA17171D;
        g.fill(x, y, x + w, y + 20, color);
        g.text(client.font, Component.literal(name), x + 8, y + 7, 0xFFFFDD00, false);
    }

    private boolean isWholeGroupSelected(String group) {
        for (TargetEntry entry : entries) {
            if (entry.group.equals(group) && !SELECTED.contains(entry.name)) {
                return false;
            }
        }

        return true;
    }

    private boolean inside(double mouseX, double mouseY, int x, int y, int w, int h) {
        return mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
    }

    private record TargetEntry(
            String group,
            String name,
            net.minecraft.world.entity.EntityType<?> type,
            boolean friendly
    )
    {
    }
}