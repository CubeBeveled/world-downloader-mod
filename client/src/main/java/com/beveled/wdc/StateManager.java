package com.beveled.wdc;

public class StateManager {
    private static boolean saveChunks = false;

    public static boolean getSaveChunks() {
        return saveChunks;
    }

    public static void setSaveChunks(boolean newValue) {
        saveChunks = newValue;
    }
}
