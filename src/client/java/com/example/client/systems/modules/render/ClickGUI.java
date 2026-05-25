package com.example.client.systems.modules.render;

import com.example.client.systems.ui.ClickGui;
import com.example.client.systems.modules.Category;
import com.example.client.systems.modules.Module;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

public class ClickGUI extends Module {

    public ClickGUI() {
        super("ClickGUI", Category.RENDER, false);

        setKey(GLFW.GLFW_KEY_RIGHT_SHIFT);
        setVisibleInArraylist(false);
    }

    @Override
    public void toggle() {
        Minecraft client = Minecraft.getInstance();

        if (client.screen instanceof com.example.client.systems.ui.ClickGui) {
            client.setScreen(null);
            return;
        }

        client.setScreen(new com.example.client.systems.ui.ClickGui());
    }
}