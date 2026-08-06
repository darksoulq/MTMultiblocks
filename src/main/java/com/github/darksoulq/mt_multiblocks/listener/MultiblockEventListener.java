package com.github.darksoulq.mt_multiblocks.listener;

import com.MT.xxxtrigger50xxx.API.MTEvents.BreakDeviceEvent;
import com.MT.xxxtrigger50xxx.API.MTEvents.PlaceDeviceEvent;
import com.MT.xxxtrigger50xxx.MineMain;
import com.github.darksoulq.mt_multiblocks.core.Behaviors;
import com.github.darksoulq.mt_multiblocks.core.MultiblockRegistry;
import com.github.darksoulq.mt_multiblocks.events.MultiblockExplodeEvent;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockFadeEvent;
import org.bukkit.event.block.BlockFormEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.event.block.MoistureChangeEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.WorldLoadEvent;

public class MultiblockEventListener implements Listener {

    @EventHandler
    public void onWorldLoad(WorldLoadEvent event) {
        Bukkit.getScheduler().runTaskLater(MineMain.getPlugin(), () -> {
            if (MultiblockRegistry.instance != null) {
                MultiblockRegistry.instance.restoreProxies();
            }
        }, 5L);
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        Bukkit.getScheduler().runTaskLater(MineMain.getPlugin(), () -> {
            if (MultiblockRegistry.instance != null) {
                MultiblockRegistry.instance.restoreProxies();
            }
        }, 2L);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onMultiblockPlace(PlaceDeviceEvent event) {
        Behaviors.handleMultiblockPlace(event);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerBlockPlace(BlockPlaceEvent event) {
        Behaviors.handlePlayerBlockPlace(event);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onMultiblockBreak(BreakDeviceEvent event) {
        Behaviors.handleMultiblockBreak(event);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onProxyBlockBreak(BlockBreakEvent event) {
        Behaviors.handleBlockBreak(event);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        Behaviors.handleExplosion(event.blockList(), MultiblockExplodeEvent.Cause.ENTITY, event.getEntity(), null);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        Behaviors.handleExplosion(event.blockList(), MultiblockExplodeEvent.Cause.BLOCK, null, event.getBlock());
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onProxyRedstone(BlockRedstoneEvent event) {
        Behaviors.handleRedstone(event);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        Behaviors.handlePistonExtend(event);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        Behaviors.handlePistonRetract(event);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBlockForm(BlockFormEvent event) {
        Behaviors.handleBlockForm(event);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBlockFade(BlockFadeEvent event) {
        Behaviors.handleBlockFade(event);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onMoistureChange(MoistureChangeEvent event) {
        Behaviors.handleMoistureChange(event);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBlockBurn(BlockBurnEvent event) {
        Behaviors.handleBlockBurn(event);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBlockIgnite(BlockIgniteEvent event) {
        Behaviors.handleBlockIgnite(event);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBlockPhysics(BlockPhysicsEvent event) {
        Behaviors.handleBlockPhysics(event);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onEntityChangeBlock(EntityChangeBlockEvent event) {
        Behaviors.handleEntityChangeBlock(event);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        Behaviors.handleInteract(event);
    }
}