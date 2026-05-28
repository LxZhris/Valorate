package com.example.client.systems.ui;

import com.example.client.systems.targets.TargetManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class TargetSelectionScreen extends Screen {
    private final Minecraft mc = Minecraft.getInstance();
    private final List<Row> rows = new ArrayList<>();

    private String search = "";
    private int scroll = 0;

    private int x = 0;
    private int y = 0;
    private final int w = 360;
    private final int h = 430;

    private boolean dragging = false;
    private int dragX;
    private int dragY;

    private boolean leftWasDown = false;

    public TargetSelectionScreen() {
        super(Component.literal("Target Selector"));
        buildRows();
    }

    private void buildRows() {
        rows.clear();

        rows.add(new GroupHeader("Players"));
        rows.add(new TargetRow("Player", TargetManager.PLAYER, Type.FRIENDLY));
        rows.add(new TargetRow("Invisible", TargetManager.INVISIBLE, Type.HOSTILE));

        rows.add(new GroupHeader("Animals"));
        addCategory(MobCategory.CREATURE, Type.FRIENDLY);
        addCategory(MobCategory.WATER_CREATURE, Type.FRIENDLY);
        addCategory(MobCategory.AMBIENT, Type.FRIENDLY);
        addCategory(MobCategory.WATER_AMBIENT, Type.FRIENDLY);

        rows.add(new GroupHeader("Mobs"));
        addCategory(MobCategory.MONSTER, Type.HOSTILE);

        rows.add(new GroupHeader("Bosses"));
        addTarget("Ender Dragon", EntityType.ENDER_DRAGON, Type.HOSTILE);
        addTarget("Wither", EntityType.WITHER, Type.HOSTILE);
    }

    private void addCategory(MobCategory category, Type type) {
        BuiltInRegistries.ENTITY_TYPE.stream()
                .filter(entityType -> entityType.getCategory() == category)
                .sorted(Comparator.comparing(entityType -> EntityType.getKey(entityType).toString()))
                .forEach(entityType -> addTarget(formatName(EntityType.getKey(entityType).getPath()), entityType, type));
    }

    private void addTarget(String name, EntityType<?> entityType, Type type) {
        rows.add(new TargetRow(name, TargetManager.id(entityType), type));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        if (x == 0 && y == 0) {
            x = (width - w) / 2;
            y = (height - h) / 2;
        }

        boolean leftDown = GLFW.glfwGetMouseButton(
                GLFW.glfwGetCurrentContext(),
                GLFW.GLFW_MOUSE_BUTTON_LEFT
        ) == GLFW.GLFW_PRESS;

        if (leftDown && !leftWasDown) {
            handleMouseClick(mouseX, mouseY);
        }

        if (!leftDown) {
            dragging = false;
        }

        leftWasDown = leftDown;

        if (dragging) {
            x = mouseX - dragX;
            y = mouseY - dragY;
        }

        int pad = 10;
        int headerH = 26;

        g.fill(x, y, x + w, y + h, 0xEE07070A);
        g.fill(x, y, x + w, y + headerH, 0xFF11131C);
        g.text(mc.font, "Target Selector", x + pad, y + 9, 0xFFFFFF00, false);

        int searchY = y + headerH + pad;
        g.fill(x + pad, searchY, x + w - pad, searchY + 24, 0xFF151822);
        g.text(mc.font, search.isEmpty() ? "Search..." : search, x + pad + 7, searchY + 8,
                search.isEmpty() ? 0xFF777777 : 0xFFFFFFFF, false);

        int buttonY = searchY + 34;
        button(g, x + pad, buttonY, 95, 24, "Select All", 0xFF213D25);
        button(g, x + pad + 105, buttonY, 110, 24, "Unselect All", 0xFF402020);

        g.text(mc.font, "Selected: " + getVisibleSelectedCount() + " / " + getTargetCount(),
                x + pad + 230, buttonY + 8, 0xFFFFFFFF, false);

        int listX = x + pad;
        int listY = buttonY + 34;
        int listW = w - pad * 2;
        int listH = h - (listY - y) - pad;

        g.fill(listX, listY, listX + listW, listY + listH, 0xAA11141D);

        int rowY = listY + 8 - scroll;

        for (Row row : rows) {
            if (!row.visible(search)) continue;

            int rh = row.height();

            if (rowY + rh >= listY && rowY <= listY + listH - 6) {
                row.render(g, listX + 8, rowY, listW - 16, mouseX, mouseY);
            }

            rowY += rh;
        }

        drawScrollbar(g, listX, listY, listW, listH);
        limitScroll(listH);
    }

    private void button(GuiGraphicsExtractor g, int bx, int by, int bw, int bh, String text, int color) {
        g.fill(bx, by, bx + bw, by + bh, color);
        g.text(mc.font, text, bx + 8, by + 8, 0xFFFFFFFF, false);
    }

    private boolean handleMouseClick(double mouseX, double mouseY) {
        int pad = 10;
        int headerH = 26;

        if (inside(mouseX, mouseY, x, y, w, headerH)) {
            dragging = true;
            dragX = (int) mouseX - x;
            dragY = (int) mouseY - y;
            return true;
        }

        int searchY = y + headerH + pad;
        int buttonY = searchY + 34;

        if (inside(mouseX, mouseY, x + pad, buttonY, 95, 24)) {
            for (Row row : rows) {
                if (row instanceof TargetRow target && target.visible(search)) {
                    TargetManager.setSelected(target.id, true);
                }
            }
            return true;
        }

        if (inside(mouseX, mouseY, x + pad + 105, buttonY, 110, 24)) {
            for (Row row : rows) {
                if (row instanceof TargetRow target && target.visible(search)) {
                    TargetManager.setSelected(target.id, false);
                }
            }
            return true;
        }

        int listX = x + pad;
        int listY = buttonY + 34;
        int listW = w - pad * 2;
        int listH = h - (listY - y) - pad;

        int rowY = listY + 8 - scroll;

        for (Row row : rows) {
            if (!row.visible(search)) continue;

            if (rowY + row.height() >= listY && rowY <= listY + listH - 6) {
                if (inside(mouseX, mouseY, listX + 8, rowY, listW - 16, row.height())) {
                    row.click();
                    return true;
                }
            }

            rowY += row.height();
        }

        return true;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double scrollY) {
        scroll -= (int) (scrollY * 22);
        if (scroll < 0) scroll = 0;
        return true;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        scroll -= (int) (scrollY * 22);
        if (scroll < 0) scroll = 0;
        return true;
    }

    public boolean charTyped(char chr, int modifiers) {
        if (Character.isLetterOrDigit(chr) || chr == ' ' || chr == '_' || chr == '-') {
            search += chr;
            scroll = 0;
        }

        return true;
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            mc.setScreen(null);
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_BACKSPACE && !search.isEmpty()) {
            search = search.substring(0, search.length() - 1);
            scroll = 0;
            return true;
        }

        return true;
    }

    private void drawScrollbar(GuiGraphicsExtractor g, int listX, int listY, int listW, int listH) {
        int content = getContentHeight();
        if (content <= listH) return;

        int barH = Math.max(24, (int) ((listH / (double) content) * listH));
        int maxScroll = Math.max(1, content - listH);
        int barY = listY + (int) ((scroll / (double) maxScroll) * (listH - barH));

        g.fill(listX + listW - 3, listY, listX + listW - 1, listY + listH, 0x55111111);
        g.fill(listX + listW - 3, barY, listX + listW - 1, barY + barH, 0xFFFFFF00);
    }

    private void limitScroll(int listH) {
        int content = getContentHeight();
        int max = Math.max(0, content - listH + 8);

        if (scroll > max) scroll = max;
        if (scroll < 0) scroll = 0;
    }

    private int getContentHeight() {
        int content = 8;

        for (Row row : rows) {
            if (row.visible(search)) content += row.height();
        }

        return content;
    }

    private int getTargetCount() {
        int count = 0;

        for (Row row : rows) {
            if (row instanceof TargetRow && row.visible(search)) count++;
        }

        return count;
    }

    private int getVisibleSelectedCount() {
        int count = 0;

        for (Row row : rows) {
            if (row instanceof TargetRow target && target.visible(search) && TargetManager.isSelected(target.id)) {
                count++;
            }
        }

        return count;
    }

    private boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    private String formatName(String id) {
        String[] parts = id.split("_");
        StringBuilder out = new StringBuilder();

        for (String part : parts) {
            if (part.isEmpty()) continue;
            out.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1)).append(" ");
        }

        return out.toString().trim();
    }

    private abstract class Row {
        abstract void render(GuiGraphicsExtractor g, int x, int y, int w, int mouseX, int mouseY);
        abstract int height();
        abstract void click();

        boolean visible(String search) {
            return true;
        }
    }

    private class GroupHeader extends Row {
        private final String name;

        private GroupHeader(String name) {
            this.name = name;
        }

        @Override
        void render(GuiGraphicsExtractor g, int x, int y, int w, int mouseX, int mouseY) {
            boolean allSelected = true;
            boolean hasAny = false;

            for (Row row : rows) {
                if (row instanceof TargetRow target && target.group().equals(name) && target.visible(search)) {
                    hasAny = true;

                    if (!TargetManager.isSelected(target.id)) {
                        allSelected = false;
                        break;
                    }
                }
            }

            g.text(mc.font, name, x, y + 7, 0xFFFFFF00, false);
            g.fill(x, y + 22, x + w, y + 23, 0x55FFFFFF);

            int boxX = x + w - 18;
            g.fill(boxX, y + 5, boxX + 12, y + 17, 0xFF050505);

            if (hasAny && allSelected) {
                g.fill(boxX + 2, y + 7, boxX + 10, y + 15, 0xFFFFFF00);
            }
        }

        @Override
        int height() {
            return 30;
        }

        @Override
        void click() {
            boolean allSelected = true;
            boolean hasAny = false;

            for (Row row : rows) {
                if (row instanceof TargetRow target && target.group().equals(name) && target.visible(search)) {
                    hasAny = true;

                    if (!TargetManager.isSelected(target.id)) {
                        allSelected = false;
                        break;
                    }
                }
            }

            if (!hasAny) return;

            for (Row row : rows) {
                if (row instanceof TargetRow target && target.group().equals(name) && target.visible(search)) {
                    TargetManager.setSelected(target.id, !allSelected);
                }
            }
        }

        @Override
        boolean visible(String search) {
            return search.isEmpty();
        }
    }

    private class TargetRow extends Row {
        private final String name;
        private final String id;
        private final Type type;
        private String cachedGroup = "";

        private TargetRow(String name, String id, Type type) {
            this.name = name;
            this.id = id;
            this.type = type;
        }

        @Override
        void render(GuiGraphicsExtractor g, int x, int y, int w, int mouseX, int mouseY) {
            boolean selected = TargetManager.isSelected(id);
            boolean hovered = inside(mouseX, mouseY, x, y, w, 22);

            if (hovered) g.fill(x - 4, y, x + w, y + 22, 0x33444444);

            int nameColor = type == Type.FRIENDLY ? 0xFF66FF66 : 0xFFFF5555;
            g.text(mc.font, name, x + 4, y + 7, selected ? 0xFFFFFF00 : nameColor, false);

            int boxX = x + w - 18;
            g.fill(boxX, y + 5, boxX + 12, y + 17, 0xFF050505);

            if (selected) {
                g.fill(boxX + 2, y + 7, boxX + 10, y + 15, 0xFFFFFF00);
            }
        }

        @Override
        int height() {
            return 24;
        }

        @Override
        void click() {
            TargetManager.toggle(id);
        }

        @Override
        boolean visible(String search) {
            return search.isEmpty() || name.toLowerCase().contains(search.toLowerCase());
        }

        String group() {
            if (!cachedGroup.isEmpty()) return cachedGroup;

            int index = rows.indexOf(this);

            for (int i = index; i >= 0; i--) {
                if (rows.get(i) instanceof GroupHeader header) {
                    cachedGroup = header.name;
                    return cachedGroup;
                }
            }

            return "";
        }
    }

    private enum Type {
        FRIENDLY,
        HOSTILE
    }
}