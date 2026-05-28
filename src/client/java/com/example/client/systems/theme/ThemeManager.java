package com.example.client.systems.theme;

public class ThemeManager {
    public enum Theme {
        VALORATE,
        SS
    }

    private static Theme currentTheme = Theme.VALORATE;

    public static Theme getTheme() {
        return currentTheme;
    }

    public static void setTheme(String theme) {
        if (theme.equalsIgnoreCase("SS")) {
            currentTheme = Theme.SS;
        } else {
            currentTheme = Theme.VALORATE;
        }
    }

    public static boolean isSS() {
        return currentTheme == Theme.SS;
    }

    public static int accent() {
        return isSS() ? 0xFFFF2222 : 0xFFFFDD00;
    }

    public static int accentDark() {
        return isSS() ? 0xFF551111 : 0xFF554A00;
    }

    public static int text() {
        return 0xFFFFFFFF;
    }

    public static int mutedText() {
        return 0xFFAAAAAA;
    }

    public static int background() {
        return isSS() ? 0xEE000000 : 0xEE07070A;
    }

    public static int panel() {
        return isSS() ? 0xDD050505 : 0xDD0A0A0D;
    }

    public static int button() {
        return isSS() ? 0xFF000000 : 0xFF151822;
    }
}