package com.example.client.systems.commands.commands;

import com.example.client.systems.commands.Command;
import com.example.client.systems.config.ConfigManager;
import com.example.client.systems.modules.Module;
import com.example.client.systems.modules.ModuleManager;
import com.example.client.systems.utils.ChatUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.lang.reflect.Field;

public class BindCommand extends Command {

    public BindCommand() {
        super("bind", "Bind a module to a key");
    }

    @Override
    public void execute(String[] args) {
        if (args.length < 3) {
            ChatUtils.info(Component.empty()
                    .append(Component.literal("Usage: ").withStyle(ChatFormatting.GRAY))
                    .append(Component.literal(".bind <module> <key>").withStyle(ChatFormatting.AQUA)));
            return;
        }

        Module module = ModuleManager.getModuleByName(args[1]);

        if (module == null) {
            ChatUtils.error("Module not found: " + args[1]);
            return;
        }

        String keyName = args[2].toUpperCase();

        if (keyName.equals("NONE") || keyName.equals("UNBIND")) {
            module.setKey(0);
            ConfigManager.save();

            ChatUtils.info(Component.empty()
                    .append(Component.literal(module.getName()).withStyle(ChatFormatting.YELLOW))
                    .append(Component.literal(" unbound").withStyle(ChatFormatting.AQUA)));
            return;
        }

        int key = getKeyCode(keyName);

        if (key == -1) {
            ChatUtils.error("Unknown key: " + keyName);
            return;
        }

        module.setKey(key);
        ConfigManager.save();

        ChatUtils.info(Component.empty()
                .append(Component.literal(module.getName()).withStyle(ChatFormatting.YELLOW))
                .append(Component.literal(" bound ").withStyle(ChatFormatting.AQUA))
                .append(Component.literal("to ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(keyName).withStyle(ChatFormatting.GREEN)));
    }

    private int getKeyCode(String keyName) {
        if (keyName.length() == 1) {
            int key = GLFW.glfwGetKeyScancode(keyName.charAt(0));
            if (keyName.length() == 1) {
                return keyName.charAt(0);
            }
        }

        String glfwName = "GLFW_KEY_" + keyName;

        try {
            Field field = GLFW.class.getField(glfwName);
            return field.getInt(null);
        } catch (Exception ignored) {
            return -1;
        }
    }
}