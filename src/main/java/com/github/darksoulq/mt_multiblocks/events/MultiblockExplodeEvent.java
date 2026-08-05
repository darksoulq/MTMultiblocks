package com.github.darksoulq.mt_multiblocks.events;

import com.MT.xxxtrigger50xxx.Devices.Device;
import com.github.darksoulq.mt_multiblocks.core.PortProxyDevice;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.List;
import java.util.Set;

public class MultiblockExplodeEvent extends Event implements Cancellable {
    public enum Cause {
        BLOCK,
        ENTITY,
        PLUGIN,
        UNKNOWN
    }

    private static final HandlerList HANDLERS = new HandlerList();
    private final Cause cause;
    private final Entity entity;
    private final Block block;
    private final List<Block> blockList;
    private final Set<Device> coresToDisassemble;
    private final Set<PortProxyDevice> optionalProxiesHit;
    private final Set<Block> requiredBlocksHit;
    private boolean cancelled;

    public MultiblockExplodeEvent(Cause cause, Entity entity, Block block, List<Block> blockList, Set<Device> coresToDisassemble, Set<PortProxyDevice> optionalProxiesHit, Set<Block> requiredBlocksHit) {
        this.cause = cause;
        this.entity = entity;
        this.block = block;
        this.blockList = blockList;
        this.coresToDisassemble = coresToDisassemble;
        this.optionalProxiesHit = optionalProxiesHit;
        this.requiredBlocksHit = requiredBlocksHit;
    }

    public Cause getCause() {
        return cause;
    }

    public Entity getEntity() {
        return entity;
    }

    public Block getBlock() {
        return block;
    }

    public List<Block> getBlockList() {
        return blockList;
    }

    public Set<Device> getCoresToDisassemble() {
        return coresToDisassemble;
    }

    public Set<PortProxyDevice> getOptionalProxiesHit() {
        return optionalProxiesHit;
    }

    public Set<Block> getRequiredBlocksHit() {
        return requiredBlocksHit;
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