package com.github.darksoulq.mt_multiblocks.core;

import com.MT.xxxtrigger50xxx.API.MTAPI;
import com.github.darksoulq.mt_multiblocks.MTMultiblocks;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class AddonDeviceRegistry {

    private final JavaPlugin plugin;
    private final List<AddonDeviceTemplate> templates = new ArrayList<>();
    private final AddonDeviceDatabase database;

    public AddonDeviceRegistry(JavaPlugin plugin) {
        this.plugin = plugin;
        this.database = new AddonDeviceDatabase(plugin);
    }

    public void register(AddonDeviceTemplate template) {
        templates.add(template);
        if (template.getDeviceClass() != null) {
            AddonDeviceDatabase.registerClass(template.getDeviceClass(), database);
        }
    }

    public void apply() {
        File devicesFile = new File(plugin.getDataFolder(), "devices.yml");
        if (!devicesFile.exists()) {
            plugin.getDataFolder().mkdirs();
            try {
                devicesFile.createNewFile();
            } catch (Exception ignored) {
            }
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(devicesFile);
        boolean needsSave = false;

        for (AddonDeviceTemplate reg : templates) {
            String basePath = "Devices." + reg.getId();

            if (!config.contains(basePath)) {
                config.set(basePath + ".Name", reg.getName());
                config.set(basePath + ".Description", reg.getDescription());
                config.set(basePath + ".Model Data", reg.getModelData());
                needsSave = true;
            }
        }

        if (needsSave) {
            try {
                config.save(devicesFile);
            } catch (Exception ignored) {
            }
        }

        MTAPI.loadCustomDevices(devicesFile);

        for (AddonDeviceTemplate reg : templates) {
            if (reg.getDeviceClass() != null && reg.getPrototypeStack() != null) {
                MTAPI.addCustomDeviceClass(
                    reg.getName(),
                    reg.getDeviceClass(),
                    reg.getCategory(),
                    reg.getPrototypeStack()
                );
            }
        }

        database.loadAll();
        Bukkit.getScheduler().scheduleSyncRepeatingTask(JavaPlugin.getPlugin(MTMultiblocks.class), database::saveAll, 800L, 800L);
    }
}