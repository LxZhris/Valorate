package com.example.client.systems.commands.commands;

import com.example.client.systems.commands.Command;
import com.example.client.systems.config.ConfigManager;
import com.example.client.systems.modules.Module;
import com.example.client.systems.modules.ModuleManager;
import com.example.client.systems.utils.ChatUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public class BindCommand extends Command {

    public BindCommand() {
        super("bind", "Binds a module to a key");
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
        int key = getKeyByName(keyName);

        if (key == 0) {
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

    private int getKeyByName(String keyName) {
        return switch (keyName) {
            case "A" -> GLFW.GLFW_KEY_A;
            case "B" -> GLFW.GLFW_KEY_B;
            case "C" -> GLFW.GLFW_KEY_C;
            case "D" -> GLFW.GLFW_KEY_D;
            case "E" -> GLFW.GLFW_KEY_E;
            case "F" -> GLFW.GLFW_KEY_F;
            case "G" -> GLFW.GLFW_KEY_G;
            case "H" -> GLFW.GLFW_KEY_H;
            case "I" -> GLFW.GLFW_KEY_I;
            case "J" -> GLFW.GLFW_KEY_J;
            case "K" -> GLFW.GLFW_KEY_K;
            case "L" -> GLFW.GLFW_KEY_L;
            case "M" -> GLFW.GLFW_KEY_M;
            case "N" -> GLFW.GLFW_KEY_N;
            case "O" -> GLFW.GLFW_KEY_O;
            case "P" -> GLFW.GLFW_KEY_P;
            case "Q" -> GLFW.GLFW_KEY_Q;
            case "R" -> GLFW.GLFW_KEY_R;
            case "S" -> GLFW.GLFW_KEY_S;
            case "T" -> GLFW.GLFW_KEY_T;
            case "U" -> GLFW.GLFW_KEY_U;
            case "V" -> GLFW.GLFW_KEY_V;
            case "W" -> GLFW.GLFW_KEY_W;
            case "X" -> GLFW.GLFW_KEY_X;
            case "Y" -> GLFW.GLFW_KEY_Y;
            case "Z" -> GLFW.GLFW_KEY_Z;

            case "LEFT_ALT" -> GLFW.GLFW_KEY_LEFT_ALT;
            case "RIGHT_ALT" -> GLFW.GLFW_KEY_RIGHT_ALT;
            case "LEFT_SHIFT" -> GLFW.GLFW_KEY_LEFT_SHIFT;
            case "RIGHT_SHIFT" -> GLFW.GLFW_KEY_RIGHT_SHIFT;
            case "SPACE" -> GLFW.GLFW_KEY_SPACE;
            case "TAB" -> GLFW.GLFW_KEY_TAB;
            case "ENTER" -> GLFW.GLFW_KEY_ENTER;
            case "ESC" -> GLFW.GLFW_KEY_ESCAPE;

            default -> 0;
        };
    }
}