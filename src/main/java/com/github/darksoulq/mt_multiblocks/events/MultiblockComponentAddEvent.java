package com.github.darksoulq.mt_multiblocks.events;

import com.MT.xxxtrigger50xxx.Devices.Device;
import com.github.darksoulq.mt_multiblocks.MultiblockBlueprint;
import com.github.darksoulq.mt_multiblocks.StructureComponent;
import org.bukkit.block.Block;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class MultiblockComponentAddEvent extends Event implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Device coreDevice;
    private final MultiblockBlueprint blueprint;
    private final Block block;
    private final StructureComponent component;
    private boolean cancelled;

    public MultiblockComponentAddEvent(Device coreDevice, MultiblockBlueprint blueprint, Block block, StructureComponent component) {
        this.coreDevice = coreDevice;
        this.blueprint = blueprint;
        this.block = block;
        this.component = component;
    }

    public Device getCoreDevice() {
        return coreDevice;
    }

    public MultiblockBlueprint getBlueprint() {
        return blueprint;
    }

    public Block getBlock() {
        return block;
    }

    public StructureComponent getComponent() {
        return component;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}