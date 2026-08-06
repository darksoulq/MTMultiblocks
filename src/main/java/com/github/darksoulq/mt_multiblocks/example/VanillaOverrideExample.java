package com.github.darksoulq.mt_multiblocks.example;

import com.MT.xxxtrigger50xxx.API.MTEvents.OpenDeviceInventoryEvent;
import com.MT.xxxtrigger50xxx.Devices.Device;
import com.MT.xxxtrigger50xxx.Devices.Liquids.LiquidSilo;
import com.MT.xxxtrigger50xxx.MineUtil;
import com.github.darksoulq.mt_multiblocks.ExternalMultiblocks;
import com.github.darksoulq.mt_multiblocks.MultiblockBlueprint;
import com.github.darksoulq.mt_multiblocks.OptionalComponent;
import com.github.darksoulq.mt_multiblocks.RequiredComponent;
import com.github.darksoulq.mt_multiblocks.choice.BlockDataChoice;
import com.github.darksoulq.mt_multiblocks.core.MultiblockRegistry;
import com.github.darksoulq.mt_multiblocks.events.MultiblockAssembleEvent;
import com.github.darksoulq.mt_multiblocks.events.MultiblockComponentAddEvent;
import com.github.darksoulq.mt_multiblocks.events.MultiblockComponentRemoveEvent;
import com.github.darksoulq.mt_multiblocks.events.MultiblockDisassembleEvent;
import com.github.darksoulq.mt_multiblocks.port.BasicItemPort;
import com.github.darksoulq.mt_multiblocks.port.BasicLiquidPort;
import com.github.darksoulq.mt_multiblocks.util.PatternBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;

public class VanillaOverrideExample implements Listener {

    public static void registerOverrides(Plugin plugin) {
        Bukkit.getPluginManager().registerEvents(new VanillaOverrideExample(), plugin);

        MultiblockBlueprint crusherBlueprint = new PatternBuilder()
            .addMapping('P', new RequiredComponent(Bukkit.createBlockData(Material.PISTON), new BasicItemPort(true, false)))
            .addMapping('H', new RequiredComponent(Bukkit.createBlockData(Material.HOPPER), new BasicItemPort(false, true)))
            .addLayer("H")
            .addLayer("C")
            .addLayer("P")
            .build();
        ExternalMultiblocks.registerBlueprint("Crusher", crusherBlueprint);

        MultiblockBlueprint boilerBlueprint = new PatternBuilder()
            .addMapping('O', new RequiredComponent(Bukkit.createBlockData(Material.COPPER_BLOCK), new BasicLiquidPort(true, false)))
            .addMapping('A', new RequiredComponent(Bukkit.createBlockData(Material.POLISHED_ANDESITE), new BasicLiquidPort(false, true)))
            .addMapping('B', new RequiredComponent(Bukkit.createBlockData(Material.BRICKS), new BasicItemPort(true, false)))
            .addLayer("BCO")
            .addLayer(" A ")
            .build();
        ExternalMultiblocks.registerBlueprint("Boiler", boilerBlueprint);

        BlockData glass = Bukkit.createBlockData(Material.GLASS);
        BlockData stainedGlass = Bukkit.createBlockData(Material.GRAY_STAINED_GLASS);
        List<BlockData> glasses = List.of(glass, stainedGlass);
        BasicLiquidPort port = new BasicLiquidPort(true, true);

        MultiblockBlueprint siloBlueprint = new PatternBuilder()
            .addMapping('G', new OptionalComponent(new BlockDataChoice(glasses), port))
            .addLayer(
                "GGG",
                "GCG",
                "GGG"
            )
            .addLayer(
                "GGG",
                "G G",
                "GGG"
            )
            .addLayer(
                "GGG",
                "GGG",
                "GGG"
            )
            .build();
        ExternalMultiblocks.registerBlueprint("Liquid Silo", siloBlueprint);
    }

    @EventHandler
    public void onSiloAssemble(MultiblockAssembleEvent event) {
        if (event.getCoreDevice() instanceof LiquidSilo silo) {
            refreshSiloCapacity(silo);
        }
    }

    @EventHandler
    public void onSiloDisassemble(MultiblockDisassembleEvent event) {
        if (event.getCoreDevice() instanceof LiquidSilo silo) {
            resetSiloCapacity(silo);
        }
    }

    @EventHandler
    public void onComponentAdd(MultiblockComponentAddEvent event) {
        if (event.getCoreDevice() instanceof LiquidSilo silo) {
            refreshSiloCapacity(silo);
        }
    }

    @EventHandler
    public void onComponentRemove(MultiblockComponentRemoveEvent event) {
        if (event.getCoreDevice() instanceof LiquidSilo silo) {
            refreshSiloCapacity(silo);
        }
    }

    @EventHandler
    public void onSiloOpen(OpenDeviceInventoryEvent event) {
        if (event.getDevice() instanceof LiquidSilo silo) {
            refreshSiloCapacity(silo);
        }
    }

    @SuppressWarnings("unchecked")
    public static void refreshSiloCapacity(LiquidSilo silo) {
        int count = MultiblockRegistry.instance.getSecondaryBlockCount(silo);
        int newCapacity = 10000 + (count * 5000);

        try {
            Field maxLiquidsField = Device.class.getDeclaredField("maxLiquids");
            maxLiquidsField.setAccessible(true);
            HashMap<Device.LiquidType, Integer> maxLiquids = (HashMap<Device.LiquidType, Integer>) maxLiquidsField.get(silo);

            for (Device.LiquidType type : silo.getLiquidTypes()) {
                maxLiquids.put(type, newCapacity);
                if (silo.getStoredLiquid(type) > newCapacity) {
                    silo.setStoredLiquid(type, newCapacity);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        silo.updateUI();
        ItemStack info = silo.getInventory().getItem(13);
        if (info != null && info.hasItemMeta()) {
            ItemMeta meta = info.getItemMeta();
            List<String> lore = meta.getLore();
            if (lore != null) {
                for (int i = 0; i < lore.size(); i++) {
                    if (lore.get(i).contains("Stored")) {
                        lore.set(i, MineUtil.colon(" Stored", silo.getStoredLiquid(silo.getTargetLiquidType()) + " / " + newCapacity));
                    }
                }
                meta.setLore(lore);
                info.setItemMeta(meta);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static void resetSiloCapacity(LiquidSilo silo) {
        try {
            Field maxLiquidsField = Device.class.getDeclaredField("maxLiquids");
            maxLiquidsField.setAccessible(true);
            HashMap<Device.LiquidType, Integer> maxLiquids = (HashMap<Device.LiquidType, Integer>) maxLiquidsField.get(silo);

            for (Device.LiquidType type : silo.getLiquidTypes()) {
                maxLiquids.put(type, 10000);
                if (silo.getStoredLiquid(type) > 10000) {
                    silo.setStoredLiquid(type, 10000);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}