package com.example.client.systems.ui;

import net.minecraft.client.Minecraft;
import com.example.client.systems.ui.TargetSelectionScreen;

public class UIManager {
    public static boolean open(String name) {
        Minecraft client = Minecraft.getInstance();

        switch (name.toLowerCase()) {
            case "clickgui" -> client.setScreen(new ClickGui());

            case "targetselectionscreen" ->
                    client.setScreen(new TargetSelectionScreen());

            default -> {
                return false;
            }
        }

        return true;
    }
}