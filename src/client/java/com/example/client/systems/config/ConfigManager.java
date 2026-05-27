package com.example.client.systems.config;

import com.example.client.systems.modules.Module;
import com.example.client.systems.modules.ModuleManager;
import com.example.client.systems.settings.BooleanSetting;
import com.example.client.systems.settings.ModeSetting;
import com.example.client.systems.settings.NumberSetting;
import com.example.client.systems.settings.Setting;
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
                object.addProperty("visibleInArraylist", module.isVisibleInArraylist());

                JsonObject settings = new JsonObject();

                for (Setting<?> setting : module.getSettings()) {
                    if (setting instanceof BooleanSetting) {
                        settings.addProperty(setting.getName(), (Boolean) setting.get());
                    } else if (setting instanceof NumberSetting) {
                        settings.addProperty(setting.getName(), (Double) setting.get());
                    } else if (setting instanceof ModeSetting) {
                        settings.addProperty(setting.getName(), (String) setting.get());
                    }
                }

                object.add("settings", settings);
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
                if (!root.has(module.getName())) continue;

                JsonObject object = root.getAsJsonObject(module.getName());

                if (object.has("enabled")) {
                    module.setEnabled(object.get("enabled").getAsBoolean());
                }

                if (object.has("key")) {
                    module.setKey(object.get("key").getAsInt());
                }

                if (object.has("visibleInArraylist")) {
                    module.setVisibleInArraylist(object.get("visibleInArraylist").getAsBoolean());
                }

                if (object.has("settings")) {
                    JsonObject settings = object.getAsJsonObject("settings");

                    for (Setting<?> setting : module.getSettings()) {
                        if (!settings.has(setting.getName())) continue;

                        if (setting instanceof BooleanSetting bool) {
                            bool.set(settings.get(setting.getName()).getAsBoolean());
                        } else if (setting instanceof NumberSetting number) {
                            number.setValue(settings.get(setting.getName()).getAsDouble());
                        } else if (setting instanceof ModeSetting mode) {
                            mode.setMode(settings.get(setting.getName()).getAsString());
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}