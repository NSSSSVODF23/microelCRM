package com.microel.trackerbackend.misc;

public class ColorGenerator {
    public static String fromStringToHsl(String string, Integer sMin, Integer sMax, Integer lMin, Integer lMax) {
        int hash = 0;
        for (int i = 0; i < string.length(); i++) {
            hash = string.charAt(i) + ((hash << 5) - hash);
        }
        int h = hash % 360;
        int s = Math.max(sMin, Math.min(sMax, (hash % 100) / 100 * (sMax - sMin) + sMin));
        int l = Math.max(lMin, Math.min(lMax, (hash % 100) / 100 * (lMax - lMin) + lMin));
        return "hsl(" + h + ", " + s + "%, " + l + "%)";
    }

    public static String fromStringToHsl(String string) {
        return fromStringToHsl(string, 70, 100, 50, 80);
    }
}
