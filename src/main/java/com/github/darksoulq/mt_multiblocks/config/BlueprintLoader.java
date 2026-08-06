package com.github.darksoulq.mt_multiblocks.config;

import com.github.darksoulq.mt_multiblocks.MultiblockBlueprint;
import com.github.darksoulq.mt_multiblocks.OptionalComponent;
import com.github.darksoulq.mt_multiblocks.choice.BlockDataChoice;
import com.github.darksoulq.mt_multiblocks.choice.ComponentChoice;
import com.github.darksoulq.mt_multiblocks.choice.TagChoice;
import com.github.darksoulq.mt_multiblocks.port.BasicStructuralPort;
import com.github.darksoulq.mt_multiblocks.port.Port;
import com.github.darksoulq.mt_multiblocks.port.PortFactory;
import com.github.darksoulq.mt_multiblocks.registry.PortRegistry;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.util.Vector;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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

                ComponentChoice choice;
                BlockData primaryData;

                if (pConfig.contains("tag")) {
                    String tagStr = pConfig.getString("tag");
                    String defaultStr = pConfig.getString("default_block_data");
                    BlockData defaultData;
                    try {
                        defaultData = defaultStr != null ? Bukkit.createBlockData(defaultStr) : Bukkit.createBlockData(Material.STONE);
                    } catch (IllegalArgumentException e) {
                        defaultData = Bukkit.createBlockData(Material.STONE);
                    }

                    NamespacedKey namespacedKey;
                    if (tagStr != null && tagStr.contains(":")) {
                        namespacedKey = NamespacedKey.fromString(tagStr);
                    } else {
                        namespacedKey = tagStr != null ? NamespacedKey.minecraft(tagStr) : NamespacedKey.minecraft("stone");
                    }

                    Tag<Material> tag = namespacedKey != null ? Bukkit.getTag(Tag.REGISTRY_BLOCKS, namespacedKey, Material.class) : null;
                    if (tag != null) {
                        choice = new TagChoice(tag, defaultData);
                        primaryData = defaultData;
                    } else {
                        choice = new BlockDataChoice(defaultData);
                        primaryData = defaultData;
                    }
                } else if (pConfig.isList("block_data")) {
                    List<String> list = pConfig.getStringList("block_data");
                    List<BlockData> dataList = new ArrayList<>();
                    for (String s : list) {
                        try {
                            dataList.add(Bukkit.createBlockData(s));
                        } catch (IllegalArgumentException ignored) {}
                    }
                    if (dataList.isEmpty()) {
                        dataList.add(Bukkit.createBlockData(Material.AIR));
                    }
                    choice = new BlockDataChoice(dataList);
                    primaryData = dataList.get(0);
                } else {
                    String blockDataStr = pConfig.getString("block_data");
                    BlockData data = blockDataStr != null ? Bukkit.createBlockData(blockDataStr) : Bukkit.createBlockData(Material.AIR);
                    choice = new BlockDataChoice(data);
                    primaryData = data;
                }

                Port port = parsePort(pConfig.getConfigurationSection("port"));
                palette.put(key, new PaletteEntry(primaryData, choice, port));
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
                        blueprint.addComponentObj(vec, new OptionalComponent(entry.choice, entry.port));
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
        ComponentChoice choice;
        Port port;

        PaletteEntry(BlockData data, ComponentChoice choice, Port port) {
            this.data = data;
            this.choice = choice;
            this.port = port;
        }
    }
}