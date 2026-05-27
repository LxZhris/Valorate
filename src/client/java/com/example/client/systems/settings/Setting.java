package com.example.client.systems.settings;

import java.util.function.Supplier;

public abstract class Setting<T> {
    private final String name;
    private T value;

    private Supplier<Boolean> visible = () -> true;

    public Setting(String name, T value) {
        this.name = name;
        this.value = value;
    }

    public Setting<T> visibleWhen(Supplier<Boolean> condition) {
        this.visible = condition;
        return this;
    }

    public boolean isVisible() {
        return visible.get();
    }

    public String getName() {
        return name;
    }

    public T get() {
        return value;
    }

    public void set(T value) {
        this.value = value;
    }
}