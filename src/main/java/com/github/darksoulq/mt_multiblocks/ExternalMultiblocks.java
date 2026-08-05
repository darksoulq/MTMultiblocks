package com.github.darksoulq.mt_multiblocks;

import java.util.HashMap;
import java.util.Map;

public class ExternalMultiblocks {
    private static final Map<String, MultiblockBlueprint> OVERRIDES = new HashMap<>();

    public static void registerBlueprint(String deviceName, MultiblockBlueprint blueprint) {
        OVERRIDES.put(deviceName, blueprint);
    }

    public static boolean hasBlueprint(String deviceName) {
        return OVERRIDES.containsKey(deviceName);
    }

    public static MultiblockBlueprint getBlueprint(String deviceName) {
        return OVERRIDES.get(deviceName);
    }
}