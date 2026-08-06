package com.github.darksoulq.mt_multiblocks;

import com.MT.xxxtrigger50xxx.Devices.Device;
import com.github.darksoulq.mt_multiblocks.command.MultiblockCommand;
import com.github.darksoulq.mt_multiblocks.core.MultiblockRegistry;
import com.github.darksoulq.mt_multiblocks.example.VanillaOverrideExample;
import com.github.darksoulq.mt_multiblocks.listener.MultiblockEventListener;
import com.github.darksoulq.mt_multiblocks.port.BasicInteractPort;
import com.github.darksoulq.mt_multiblocks.port.BasicItemPort;
import com.github.darksoulq.mt_multiblocks.port.BasicLiquidPort;
import com.github.darksoulq.mt_multiblocks.port.BasicRedstonePort;
import com.github.darksoulq.mt_multiblocks.port.BasicStructuralPort;
import com.github.darksoulq.mt_multiblocks.preview.MultiblockPreviewManager;
import com.github.darksoulq.mt_multiblocks.registry.PortRegistry;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public final class MTMultiblocks extends JavaPlugin {
    private static MTMultiblocks instance;

    @Override
    public void onEnable() {
        instance = this;
        registerDefaultPorts();

        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            MultiblockCommand.register(event.registrar());
        });

        new MultiblockRegistry();
        getServer().getPluginManager().registerEvents(new MultiblockEventListener(), this);
        MultiblockPreviewManager.start();
        VanillaOverrideExample.registerOverrides(this);
    }

    @Override
    public void onDisable() {
        MultiblockPreviewManager.stop();
        if (MultiblockRegistry.instance != null) {
            MultiblockRegistry.instance.removeAllProxies();
            MultiblockRegistry.instance.save();
        }
    }

    public static MTMultiblocks getInstance() {
        return instance;
    }

    private void registerDefaultPorts() {
        NamespacedKey itemPortKey = new NamespacedKey(this, "item_port");
        PortRegistry.register(itemPortKey, config -> new BasicItemPort(
            config.getBoolean("is_input", false),
            config.getBoolean("is_output", false)
        ));

        NamespacedKey liquidPortKey = new NamespacedKey(this, "liquid_port");
        PortRegistry.register(liquidPortKey, config -> {
            List<String> liquidStrs = config.getStringList("liquids");
            Device.LiquidType[] liquids = null;
            if (!liquidStrs.isEmpty()) {
                List<Device.LiquidType> list = new ArrayList<>();
                for (String s : liquidStrs) {
                    try {
                        list.add(Device.LiquidType.valueOf(s.toUpperCase()));
                    } catch (IllegalArgumentException ignored) {}
                }
                liquids = list.toArray(new Device.LiquidType[0]);
            }
            return new BasicLiquidPort(
                config.getBoolean("is_input", false),
                config.getBoolean("is_output", false),
                liquids
            );
        });

        NamespacedKey structuralKey = new NamespacedKey(this, "structural_port");
        PortRegistry.register(structuralKey, _ -> new BasicStructuralPort());

        NamespacedKey interactKey = new NamespacedKey(this, "interact_port");
        PortRegistry.register(interactKey, _ -> new BasicInteractPort());

        NamespacedKey redstoneKey = new NamespacedKey(this, "redstone_port");
        PortRegistry.register(redstoneKey, config -> new BasicRedstonePort(
            config.getBoolean("can_receive", false),
            config.getBoolean("can_emit", false)
        ));
    }
}