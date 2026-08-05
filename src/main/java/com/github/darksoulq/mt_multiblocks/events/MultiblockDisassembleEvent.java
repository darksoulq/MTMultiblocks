package com.github.darksoulq.mt_multiblocks.events;

import com.MT.xxxtrigger50xxx.Devices.Device;
import com.github.darksoulq.mt_multiblocks.MultiblockBlueprint;
import com.github.darksoulq.mt_multiblocks.StructureComponent;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.Map;

public class MultiblockDisassembleEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Device coreDevice;
    private final MultiblockBlueprint blueprint;
    private final Player player;
    private final Block coreBlock;
    private final Map<Location, StructureComponent> structureBlocks;

    public MultiblockDisassembleEvent(Device coreDevice, MultiblockBlueprint blueprint, Player player, Block coreBlock, Map<Location, StructureComponent> structureBlocks) {
        this.coreDevice = coreDevice;
        this.blueprint = blueprint;
        this.player = player;
        this.coreBlock = coreBlock;
        this.structureBlocks = structureBlocks;
    }

    public Device getCoreDevice() {
        return coreDevice;
    }

    public MultiblockBlueprint getBlueprint() {
        return blueprint;
    }

    public Player getPlayer() {
        return player;
    }

    public Block getCoreBlock() {
        return coreBlock;
    }

    public Map<Location, StructureComponent> getStructureBlocks() {
        return structureBlocks;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}