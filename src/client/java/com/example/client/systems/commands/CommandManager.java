package com.example.client.systems.commands;

import com.example.client.systems.commands.commands.BindCommand;
import com.example.client.systems.commands.commands.HelpCommand;
import com.example.client.systems.commands.commands.RenameCommand;
import com.example.client.systems.utils.ChatUtils;
import com.example.client.systems.commands.commands.DevCommand;

import java.util.ArrayList;
import java.util.List;

public class CommandManager {
    public static final String PREFIX = ".";
    public static final List<Command> COMMANDS = new ArrayList<>();

    public static void init() {
        COMMANDS.add(new BindCommand());
        COMMANDS.add(new HelpCommand());
        COMMANDS.add(new DevCommand());
        COMMANDS.add(new RenameCommand());
    }

    public static void execute(String message) {
        if (!message.startsWith(PREFIX)) return;

        String[] args = message.substring(PREFIX.length()).split(" ");
        String commandName = args[0];

        for (Command command : COMMANDS) {
            if (command.getName().equalsIgnoreCase(commandName)) {
                command.execute(args);
                return;
            }
        }

        ChatUtils.error("Command not found: " + commandName);
    }
}