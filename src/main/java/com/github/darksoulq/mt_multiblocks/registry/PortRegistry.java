package com.github.darksoulq.mt_multiblocks.registry;

import com.github.darksoulq.mt_multiblocks.port.PortFactory;
import org.bukkit.NamespacedKey;

import java.util.HashMap;
import java.util.Map;

public class PortRegistry {
    private static final Map<NamespacedKey, PortFactory> REGISTRY = new HashMap<>();

    public static void register(NamespacedKey key, PortFactory factory) {
        REGISTRY.put(key, factory);
    }

    public static PortFactory get(NamespacedKey key) {
        return REGISTRY.get(key);
    }
}