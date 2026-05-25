package com.example.client.systems.commands.commands;

import com.example.client.systems.commands.Command;
import com.example.client.systems.commands.CommandManager;
import com.example.client.systems.utils.ChatUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

public class HelpCommand extends Command {

    public HelpCommand() {
        super("help", "Shows all commands");
    }

    @Override
    public void execute(String[] args) {
        for (Command command : CommandManager.COMMANDS) {
            ChatUtils.info(Component.empty()
                    .append(Component.literal("." + command.getName()).withStyle(ChatFormatting.AQUA))
                    .append(Component.literal(" - ").withStyle(ChatFormatting.GRAY))
                    .append(Component.literal(command.getDescription()).withStyle(ChatFormatting.WHITE)));
        }
    }
}