package com.github.darksoulq.mt_multiblocks;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ExternalMultiblocks {
    private static final Map<String, BlueprintProvider> BLUEPRINT_OVERRIDES = new ConcurrentHashMap<>();

    public static void registerBlueprint(String deviceName, BlueprintProvider provider) {
        BLUEPRINT_OVERRIDES.put(deviceName, provider);
    }

    public static void registerBlueprint(String deviceName, MultiblockBlueprint blueprint) {
        BLUEPRINT_OVERRIDES.put(deviceName, ctx -> blueprint);
    }

    public static boolean hasBlueprint(String deviceName) {
        return BLUEPRINT_OVERRIDES.containsKey(deviceName);
    }

    public static MultiblockBlueprint getBlueprint(String deviceName, BlueprintContext context) {
        BlueprintProvider provider = BLUEPRINT_OVERRIDES.get(deviceName);
        return provider != null ? provider.getBlueprint(context) : null;
    }
}