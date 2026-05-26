package com.example.client.systems.settings;

public class ModeSetting extends Setting<String> {
    private final String[] modes;
    private boolean open;

    public ModeSetting(String name, String defaultMode, String... modes) {
        super(name, defaultMode);
        this.modes = modes;
    }

    public String[] getModes() {
        return modes;
    }

    public boolean isOpen() {
        return open;
    }

    public void toggleOpen() {
        open = !open;
    }

    public void setMode(String mode) {
        set(mode);
        open = false;
    }
}