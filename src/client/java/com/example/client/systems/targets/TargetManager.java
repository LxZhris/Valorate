package com.example.client.systems.targets;

import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.HashSet;
import java.util.Set;

public class TargetManager {
    public static final String PLAYER = "valorate:player";
    public static final String INVISIBLE = "valorate:invisible";

    private static final Set<String> SELECTED = new HashSet<>();

    public static boolean isSelected(String id) {
        return SELECTED.contains(id);
    }

    public static void setSelected(String id, boolean selected) {
        if (selected) SELECTED.add(id);
        else SELECTED.remove(id);
    }

    public static void toggle(String id) {
        setSelected(id, !isSelected(id));
    }

    public static void clear() {
        SELECTED.clear();
    }

    public static int selectedCount() {
        return SELECTED.size();
    }

    public static Set<String> getSelected() {
        return SELECTED;
    }

    public static String id(EntityType<?> type) {
        Identifier id = EntityType.getKey(type);
        return id.toString();
    }

    public static boolean isTarget(LivingEntity entity) {
        if (entity instanceof Player) {
            if (entity.isInvisible()) return isSelected(INVISIBLE);
            return isSelected(PLAYER);
        }

        return isSelected(id(entity.getType()));
    }
}