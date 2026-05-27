package com.example.client.systems.modules;

import com.example.client.systems.settings.Setting;

import java.util.ArrayList;
import java.util.List;

public abstract class Module {
    private final String name;
    private final Category category;
    private final List<Setting<?>> settings = new ArrayList<>();

    private String description = "No description available.";

    private boolean enabled;
    private boolean visibleInArraylist = true;
    private boolean keyPressed;
    private int key;

    public Module(String name, Category category, boolean enabled) {
        this.name = name;
        this.category = category;
        this.enabled = enabled;
    }

    public Module(String name, Category category, boolean enabled, String description) {
        this(name, category, enabled);
        this.description = description;
    }

    public void toggle() {
        setEnabled(!enabled);
    }

    public void setEnabled(boolean enabled) {
        if (this.enabled == enabled) return;

        this.enabled = enabled;

        if (enabled) {
            onEnable();
        } else {
            onDisable();
        }
    }

    public void onEnable() {
    }

    public void onDisable() {
    }

    public void onTick() {
    }

    protected void addSetting(Setting<?> setting) {
        settings.add(setting);
    }

    public List<Setting<?>> getSettings() {
        return settings;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Category getCategory() {
        return category;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isVisibleInArraylist() {
        return visibleInArraylist;
    }

    public void setVisibleInArraylist(boolean visibleInArraylist) {
        this.visibleInArraylist = visibleInArraylist;
    }

    public boolean isKeyPressed() {
        return keyPressed;
    }

    public void setKeyPressed(boolean keyPressed) {
        this.keyPressed = keyPressed;
    }

    public int getKey() {
        return key;
    }

    public void setKey(int key) {
        this.key = key;
    }
}