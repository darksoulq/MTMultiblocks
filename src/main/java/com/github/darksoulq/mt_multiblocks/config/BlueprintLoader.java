package com.github.darksoulq.mt_multiblocks.config;

import com.github.darksoulq.mt_multiblocks.MultiblockBlueprint;
import com.github.darksoulq.mt_multiblocks.port.BasicStructuralPort;
import com.github.darksoulq.mt_multiblocks.port.Port;
import com.github.darksoulq.mt_multiblocks.port.PortFactory;
import com.github.darksoulq.mt_multiblocks.registry.PortRegistry;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.util.Vector;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class BlueprintLoader {

    public static MultiblockBlueprint loadBlueprint(File file) {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        return loadBlueprint(config);
    }

    public static MultiblockBlueprint loadBlueprint(ConfigurationSection config) {
        MultiblockBlueprint blueprint = new MultiblockBlueprint();

        ConfigurationSection paletteSection = config.getConfigurationSection("palette");
        Map<String, PaletteEntry> palette = new HashMap<>();

        if (paletteSection != null) {
            for (String key : paletteSection.getKeys(false)) {
                ConfigurationSection pConfig = paletteSection.getConfigurationSection(key);
                if (pConfig == null) continue;

                String blockDataStr = pConfig.getString("block_data");
                BlockData data = blockDataStr != null ? Bukkit.createBlockData(blockDataStr) : Bukkit.createBlockData(org.bukkit.Material.AIR);
                Port port = parsePort(pConfig.getConfigurationSection("port"));

                palette.put(key, new PaletteEntry(data, port));
            }
        }

        ConfigurationSection blocksSection = config.getConfigurationSection("blocks");
        if (blocksSection != null) {
            for (String key : blocksSection.getKeys(false)) {
                ConfigurationSection bConfig = blocksSection.getConfigurationSection(key);
                if (bConfig == null) continue;

                double x = bConfig.getDouble("x");
                double y = bConfig.getDouble("y");
                double z = bConfig.getDouble("z");
                Vector vec = new Vector(x, y, z);

                String pKey = bConfig.getString("palette_key");
                boolean required = bConfig.getBoolean("required", true);

                PaletteEntry entry = palette.get(pKey);
                if (entry != null) {
                    if (required) {
                        blueprint.addComponent(vec, entry.data, entry.port);
                    } else {
                        blueprint.addOptionalComponent(vec, entry.data, entry.port);
                    }
                }
            }
        }

        return blueprint;
    }

    private static Port parsePort(ConfigurationSection portConfig) {
        if (portConfig == null) return new BasicStructuralPort();

        String typeStr = portConfig.getString("type");
        if (typeStr == null) return new BasicStructuralPort();

        NamespacedKey key;
        try {
            key = NamespacedKey.fromString(typeStr);
        } catch (IllegalArgumentException e) {
            return new BasicStructuralPort();
        }

        if (key == null) return new BasicStructuralPort();

        PortFactory factory = PortRegistry.get(key);
        if (factory != null) {
            return factory.create(portConfig);
        }

        return new BasicStructuralPort();
    }

    private static class PaletteEntry {
        BlockData data;
        Port port;
        PaletteEntry(BlockData data, Port port) {
            this.data = data;
            this.port = port;
        }
    }
}