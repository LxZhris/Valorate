package com.example.client.systems.settings;

public class ButtonSetting extends Setting<String> {
    private final Runnable action;

    public ButtonSetting(String name, String value, Runnable action) {
        super(name, value);
        this.action = action;
    }

    public void press() {
        if (action != null) action.run();
    }
}