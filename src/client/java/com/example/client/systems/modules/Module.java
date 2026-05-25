package com.example.client.systems.modules;

public abstract class Module {
    private final String name;
    private final Category category;
    private boolean enabled;


    private boolean visibleInArraylist = true;

    public boolean isVisibleInArraylist() {
        return visibleInArraylist;
    }

    public void setVisibleInArraylist(boolean visibleInArraylist) {
        this.visibleInArraylist = visibleInArraylist;
    }
    private boolean keyPressed;

    public boolean isKeyPressed() {
        return keyPressed;
    }

    public void setKeyPressed(boolean keyPressed) {
        this.keyPressed = keyPressed;
    }

    private int key;

    public int getKey() {
        return key;
    }

    public void setKey(int key) {
        this.key = key;
    }

    public Module(String name, Category category, boolean enabled) {
        this.name = name;
        this.category = category;
        this.enabled = enabled;
    }

    public String getName() {
        return name;
    }

    public Category getCategory() {
        return category;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void toggle() {
        enabled = !enabled;
    }
}