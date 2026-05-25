package com.example.client.systems.config;

import com.example.client.systems.modules.Module;
import com.example.client.systems.modules.ModuleManager;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

public class ConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File FILE = FabricLoader.getInstance()
            .getConfigDir()
            .resolve("valorate.json")
            .toFile();

    public static void save() {
        try {
            JsonObject root = new JsonObject();

            for (Module module : ModuleManager.MODULES) {
                JsonObject object = new JsonObject();
                object.addProperty("enabled", module.isEnabled());
                object.addProperty("key", module.getKey());

                root.add(module.getName(), object);
            }

            FileWriter writer = new FileWriter(FILE);
            GSON.toJson(root, writer);
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void load() {
        try {
            if (!FILE.exists()) {
                save();
                return;
            }

            JsonObject root = GSON.fromJson(new FileReader(FILE), JsonObject.class);

            for (Module module : ModuleManager.MODULES) {
                if (!root.has(module.getName())) {
                    continue;
                }

                JsonObject object = root.getAsJsonObject(module.getName());

                if (object.has("enabled")) {
                    module.setEnabled(object.get("enabled").getAsBoolean());
                }

                if (object.has("key")) {
                    module.setKey(object.get("key").getAsInt());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}