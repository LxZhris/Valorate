package com.example.client.systems.commands.commands;

import com.example.client.systems.commands.Command;
import com.example.client.systems.ui.UIManager;
import com.example.client.systems.utils.ChatUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

public class DevCommand extends Command {

    public DevCommand() {
        super("dev", "Developer commands");
    }

    @Override
    public void execute(String[] args) {
        if (args.length < 3 || !args[1].equalsIgnoreCase("open")) {
            ChatUtils.info(Component.empty()
                    .append(Component.literal("Usage: ").withStyle(ChatFormatting.GRAY))
                    .append(Component.literal(".dev open <screen>").withStyle(ChatFormatting.AQUA)));
            return;
        }

        String screenName = args[2];

        if (UIManager.open(screenName)) {
            ChatUtils.info(Component.empty()
                    .append(Component.literal("Opened ").withStyle(ChatFormatting.AQUA))
                    .append(Component.literal(screenName).withStyle(ChatFormatting.YELLOW)));
        } else {
            ChatUtils.error("Unknown screen: " + screenName);
        }
    }
}