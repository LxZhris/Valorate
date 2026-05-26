package com.example.client.systems.ui;

import net.minecraft.client.Minecraft;

public class UIManager {
    public static boolean open(String name) {
        Minecraft client = Minecraft.getInstance();

        switch (name.toLowerCase()) {
            case "clickgui" -> client.setScreen(new ClickGui());
            default -> {
                return false;
            }
        }

        return true;
    }
}