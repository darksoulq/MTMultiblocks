package com.github.darksoulq.mt_multiblocks.core;

import com.MT.xxxtrigger50xxx.API.MTEvents.BreakDeviceEvent;
import com.MT.xxxtrigger50xxx.API.MTEvents.PlaceDeviceEvent;
import com.MT.xxxtrigger50xxx.Devices.Device;
import com.MT.xxxtrigger50xxx.MineMain;
import com.github.darksoulq.mt_multiblocks.BlueprintContext;
import com.github.darksoulq.mt_multiblocks.MultiblockBlueprint;
import com.github.darksoulq.mt_multiblocks.StructureComponent;
import com.github.darksoulq.mt_multiblocks.events.MultiblockAssembleEvent;
import com.github.darksoulq.mt_multiblocks.events.MultiblockComponentAddEvent;
import com.github.darksoulq.mt_multiblocks.events.MultiblockComponentRemoveEvent;
import com.github.darksoulq.mt_multiblocks.events.MultiblockDisassembleEvent;
import com.github.darksoulq.mt_multiblocks.events.MultiblockExplodeEvent;
import com.github.darksoulq.mt_multiblocks.port.RedstonePort;
import com.github.darksoulq.mt_multiblocks.registry.MultiblockRegistry;
import com.github.darksoulq.mt_multiblocks.util.MultiblockUtil;
import com.github.darksoulq.mt_multiblocks.util.RotationUtil;
import com.github.darksoulq.mt_multiblocks.util.ValidationResult;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
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
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class Behaviors {

    public static void handleMultiblockPlace(PlaceDeviceEvent event) {
        Device device = event.getDevice();
        BlueprintContext context = new BlueprintContext(device.getLocationREADONLY(), RotationUtil.getFacingSafely(device), event.getPlayer(), event.getPlayer().getInventory().getItemInMainHand(), device);
        MultiblockBlueprint blueprint = MultiblockUtil.getTargetBlueprint(device, context);

        if (blueprint != null) {
            Location coreLoc = context.getCoreLocation();
            BlockFace facing = context.getFacing();

            ValidationResult validation = blueprint.canPlace(context);
            if (!validation.isValid()) {
                event.setCancelled(true);
                event.setReason(validation.getMessage() != null ? validation.getMessage() : "Placement conditions not met for this multiblock.");
                return;
            }

            Map<Vector, StructureComponent> structure = blueprint.buildStructure(context);
            Map<Location, StructureComponent> structureBlocks = new HashMap<>(structure.size());

            for (Map.Entry<Vector, StructureComponent> entry : structure.entrySet()) {
                StructureComponent component = entry.getValue();
                Vector rotated = RotationUtil.rotateVector(entry.getKey(), facing);
                Location target = coreLoc.clone().add(rotated);

                if (!target.getWorld().isChunkLoaded(target.getBlockX() >> 4, target.getBlockZ() >> 4)) {
                    event.setCancelled(true);
                    event.setReason("Cannot build multiblock into unloaded chunks.");
                    return;
                }

                if (component.isRequired()) {
                    structureBlocks.put(target, component);
                    if (!target.getBlock().getType().isAir() && !target.getBlock().isReplaceable()) {
                        event.setCancelled(true);
                        event.setReason("Not enough space to assemble multiblock structure.");
                        return;
                    }
                }
            }

            MultiblockAssembleEvent assembleEvent = new MultiblockAssembleEvent(device, blueprint, event.getPlayer(), coreLoc.getBlock(), structureBlocks);
            Bukkit.getPluginManager().callEvent(assembleEvent);

            if (assembleEvent.isCancelled()) {
                event.setCancelled(true);
                return;
            }

            for (Map.Entry<Vector, StructureComponent> entry : structure.entrySet()) {
                StructureComponent component = entry.getValue();
                Vector rotated = RotationUtil.rotateVector(entry.getKey(), facing);
                Location target = coreLoc.clone().add(rotated);
                Block targetBlock = target.getBlock();

                if (component.isRequired()) {
                    ServerLevel nmsLevel = ((CraftWorld) target.getWorld()).getHandle();
                    BlockPos pos = new BlockPos(target.getBlockX(), target.getBlockY(), target.getBlockZ());
                    nmsLevel.setBlock(pos, component.getNMSDisplayData(facing), 18);

                    MultiblockRegistry.instance.registerSecondaryBlock(target, device, component);
                } else if (component.matches(targetBlock.getBlockData(), facing)) {
                    MultiblockComponentAddEvent addEvent = new MultiblockComponentAddEvent(device, blueprint, targetBlock, component);
                    Bukkit.getPluginManager().callEvent(addEvent);
                    if (!addEvent.isCancelled()) {
                        MultiblockRegistry.instance.registerSecondaryBlock(target, device, component);
                    }
                }
            }
        }
    }

    public static void handlePlayerBlockPlace(BlockPlaceEvent event) {
        Block placedBlock = event.getBlockPlaced();
        Location placedLoc = placedBlock.getLocation();

        for (Device device : Device.getNearbyDevices(placedLoc, 15)) {
            BlueprintContext context = new BlueprintContext(device.getLocationREADONLY(), RotationUtil.getFacingSafely(device), event.getPlayer(), event.getItemInHand(), device);
            MultiblockBlueprint blueprint = MultiblockUtil.getTargetBlueprint(device, context);
            if (blueprint != null) {
                BlockFace facing = context.getFacing();
                Location coreLoc = context.getCoreLocation();
                Map<Vector, StructureComponent> structure = blueprint.buildStructure(context);

                for (Map.Entry<Vector, StructureComponent> entry : structure.entrySet()) {
                    Vector rotated = RotationUtil.rotateVector(entry.getKey(), facing);
                    Location targetLoc = coreLoc.clone().add(rotated);

                    if (!targetLoc.getWorld().isChunkLoaded(targetLoc.getBlockX() >> 4, targetLoc.getBlockZ() >> 4)) {
                        continue;
                    }

                    Block targetBlock = targetLoc.getBlock();

                    if (targetBlock.equals(placedBlock)) {
                        StructureComponent component = entry.getValue();
                        if (component.matches(placedBlock.getBlockData(), facing)) {
                            MultiblockComponentAddEvent addEvent = new MultiblockComponentAddEvent(device, blueprint, placedBlock, component);
                            Bukkit.getPluginManager().callEvent(addEvent);
                            if (!addEvent.isCancelled()) {
                                MultiblockRegistry.instance.registerSecondaryBlock(targetLoc, device, component);
                            }
                        }
                    }
                }
            }
        }
    }

    public static void handleMultiblockBreak(BreakDeviceEvent event) {
        Device device = event.getDevice();
        BlueprintContext context = new BlueprintContext(device.getLocationREADONLY(), RotationUtil.getFacingSafely(device), event.getPlayer(), null, device);
        MultiblockBlueprint blueprint = MultiblockUtil.getTargetBlueprint(device, context);

        if (blueprint != null) {
            disassembleMultiblock(device, blueprint, event.getPlayer(), false);
        }
    }

    public static void handleBlockBreak(BlockBreakEvent event) {
        Device device = Device.getDevice(event.getBlock().getLocation());

        if (device == null && MultiblockRegistry.instance != null) {
            MultiblockRegistry.instance.restoreProxies();
            device = Device.getDevice(event.getBlock().getLocation());
        }

        if (device instanceof PortProxyDevice proxy) {
            Device core = proxy.getCoreDevice();
            if (core != null && !core.isRemoved()) {
                if (proxy.isRequired()) {
                    event.setCancelled(true);
                    Location coreLoc = core.getLocationREADONLY();
                    if (!coreLoc.getWorld().isChunkLoaded(coreLoc.getBlockX() >> 4, coreLoc.getBlockZ() >> 4)) {
                        return;
                    }
                    Bukkit.getScheduler().runTask(MineMain.getPlugin(), () -> {
                        Block coreBlock = coreLoc.getBlock();
                        if (coreBlock.getType() != Material.AIR) {
                            BlockBreakEvent coreEvent = new BlockBreakEvent(coreBlock, event.getPlayer());
                            Bukkit.getPluginManager().callEvent(coreEvent);
                            if (!coreEvent.isCancelled()) {
                                ServerLevel nmsLevel = ((CraftWorld) coreLoc.getWorld()).getHandle();
                                BlockPos pos = new BlockPos(coreLoc.getBlockX(), coreLoc.getBlockY(), coreLoc.getBlockZ());
                                nmsLevel.setBlock(pos, Blocks.AIR.defaultBlockState(), 18);
                            }
                        }
                    });
                } else {
                    BlueprintContext context = new BlueprintContext(core.getLocationREADONLY(), RotationUtil.getFacingSafely(core), event.getPlayer(), null, core);
                    MultiblockBlueprint blueprint = MultiblockUtil.getTargetBlueprint(core, context);
                    MultiblockComponentRemoveEvent removeEvent = new MultiblockComponentRemoveEvent(core, blueprint, event.getBlock(), proxy.getComponent());
                    Bukkit.getPluginManager().callEvent(removeEvent);

                    MultiblockRegistry.instance.unregisterSecondaryBlock(event.getBlock().getLocation());
                    proxy.removeDevice();
                }
            }
        }
    }

    public static void handleExplosion(List<Block> blockList, MultiblockExplodeEvent.Cause cause, Entity entity, Block sourceBlock) {
        Set<Device> coresToDisassemble = new HashSet<>();
        Set<PortProxyDevice> optionalProxiesHit = new HashSet<>();
        Set<Block> proxyBlocksInExplosion = new HashSet<>();

        for (Block block : blockList) {
            Device device = Device.getDevice(block.getLocation());

            if (device instanceof PortProxyDevice proxy) {
                Device core = proxy.getCoreDevice();
                if (core != null && !core.isRemoved()) {
                    if (proxy.isRequired()) {
                        coresToDisassemble.add(core);
                    } else {
                        optionalProxiesHit.add(proxy);
                    }
                }
                proxyBlocksInExplosion.add(block);
            } else if (device != null && MultiblockUtil.isCoreDevice(device)) {
                coresToDisassemble.add(device);
            }
        }

        blockList.removeAll(proxyBlocksInExplosion);

        MultiblockExplodeEvent event = new MultiblockExplodeEvent(cause, entity, sourceBlock, blockList, coresToDisassemble, optionalProxiesHit, proxyBlocksInExplosion);
        Bukkit.getPluginManager().callEvent(event);

        if (event.isCancelled()) {
            return;
        }

        for (PortProxyDevice optionalProxy : event.getOptionalProxiesHit()) {
            Device core = optionalProxy.getCoreDevice();
            if (core != null && !event.getCoresToDisassemble().contains(core)) {
                Location loc = optionalProxy.getLocationREADONLY();
                loc.getWorld().dropItemNaturally(loc, new ItemStack(loc.getBlock().getType()));
                MultiblockRegistry.instance.unregisterSecondaryBlock(loc);
                optionalProxy.removeDevice();
                ServerLevel nmsLevel = ((CraftWorld) loc.getWorld()).getHandle();
                BlockPos pos = new BlockPos(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
                nmsLevel.setBlock(pos, Blocks.AIR.defaultBlockState(), 18);
            }
        }

        for (Device core : event.getCoresToDisassemble()) {
            if (core != null && !core.isRemoved()) {
                Block coreBlock = core.getLocationREADONLY().getBlock();
                blockList.remove(coreBlock);

                Bukkit.getScheduler().runTask(MineMain.getPlugin(), () -> {
                    if (coreBlock.getType() != Material.AIR) {
                        Player player = null;
                        if (cause == MultiblockExplodeEvent.Cause.ENTITY && entity != null) {
                            if (entity instanceof org.bukkit.entity.TNTPrimed tnt && tnt.getSource() instanceof Player p) {
                                player = p;
                            } else if (entity instanceof org.bukkit.entity.Creeper creeper && creeper.getTarget() instanceof Player p) {
                                player = p;
                            }
                        }
                        if (player == null) {
                            if (core.getPlacerUUIDString() != null) {
                                try {
                                    player = Bukkit.getPlayer(UUID.fromString(core.getPlacerUUIDString()));
                                } catch (Exception ignored) {}
                            }
                        }
                        if (player == null) {
                            player = Bukkit.getOnlinePlayers().stream().findFirst().orElse(null);
                        }

                        if (player != null) {
                            BlockBreakEvent coreEvent = new BlockBreakEvent(coreBlock, player);
                            Bukkit.getPluginManager().callEvent(coreEvent);
                            if (!coreEvent.isCancelled()) {
                                ServerLevel nmsLevel = ((CraftWorld) coreBlock.getWorld()).getHandle();
                                BlockPos pos = new BlockPos(coreBlock.getX(), coreBlock.getY(), coreBlock.getZ());
                                nmsLevel.setBlock(pos, Blocks.AIR.defaultBlockState(), 18);
                            }
                        } else {
                            handleValidationFailure(core);
                        }
                    } else {
                        handleValidationFailure(core);
                    }
                });
            }
        }
    }

    public static void handleValidationFailure(Device coreDevice) {
        if (coreDevice == null || coreDevice.isRemoved()) return;
        BlueprintContext context = new BlueprintContext(coreDevice.getLocationREADONLY(), RotationUtil.getFacingSafely(coreDevice), null, null, coreDevice);
        MultiblockBlueprint blueprint = MultiblockUtil.getTargetBlueprint(coreDevice, context);
        if (blueprint != null) {
            Location coreLoc = context.getCoreLocation();
            if (coreLoc.getWorld().isChunkLoaded(coreLoc.getBlockX() >> 4, coreLoc.getBlockZ() >> 4)) {
                coreLoc.getWorld().dropItemNaturally(coreLoc, coreDevice.getDeviceStack());
                coreDevice.dropInventory();
            }
            disassembleMultiblock(coreDevice, blueprint, null, true);
            coreDevice.onDeviceRemove();
            coreDevice.cleanup();
            coreDevice.iconCleanup();
            coreDevice.removeDevice();
        }
    }

    public static void handleOptionalValidationFailure(PortProxyDevice proxy) {
        Device core = proxy.getCoreDevice();
        if (core != null && !core.isRemoved()) {
            BlueprintContext context = new BlueprintContext(core.getLocationREADONLY(), RotationUtil.getFacingSafely(core), null, null, core);
            MultiblockBlueprint blueprint = MultiblockUtil.getTargetBlueprint(core, context);
            MultiblockComponentRemoveEvent removeEvent = new MultiblockComponentRemoveEvent(core, blueprint, proxy.getLocationREADONLY().getBlock(), proxy.getComponent());
            Bukkit.getPluginManager().callEvent(removeEvent);
        }
        MultiblockRegistry.instance.unregisterSecondaryBlock(proxy.getLocationREADONLY());
        proxy.removeDevice();
    }

    public static void disassembleMultiblock(Device coreDevice, MultiblockBlueprint blueprint, Player player, boolean removeCoreBlock) {
        Location coreLoc = coreDevice.getLocationREADONLY();
        BlockFace facing = RotationUtil.getFacingSafely(coreDevice);
        BlueprintContext context = new BlueprintContext(coreLoc, facing, player, null, coreDevice);

        Map<Vector, StructureComponent> structure = blueprint.buildStructure(context);
        Map<Location, StructureComponent> structureBlocks = new HashMap<>(structure.size());

        for (Map.Entry<Vector, StructureComponent> entry : structure.entrySet()) {
            Vector rotated = RotationUtil.rotateVector(entry.getKey(), facing);
            Location targetLoc = coreLoc.clone().add(rotated);
            if (targetLoc.getWorld().isChunkLoaded(targetLoc.getBlockX() >> 4, targetLoc.getBlockZ() >> 4)) {
                structureBlocks.put(targetLoc, entry.getValue());
                Device dev = Device.getDevice(targetLoc);
                if (dev instanceof PortProxyDevice proxy && proxy.getCoreDevice().equals(coreDevice)) {
                    Block block = targetLoc.getBlock();
                    if (!entry.getValue().isRequired()) {
                        targetLoc.getWorld().dropItemNaturally(targetLoc, new ItemStack(block.getType()));
                    }
                    ServerLevel nmsLevel = ((CraftWorld) targetLoc.getWorld()).getHandle();
                    BlockPos pos = new BlockPos(targetLoc.getBlockX(), targetLoc.getBlockY(), targetLoc.getBlockZ());
                    nmsLevel.setBlock(pos, Blocks.AIR.defaultBlockState(), 18);

                    MultiblockRegistry.instance.unregisterSecondaryBlock(targetLoc);
                    proxy.removeDevice();
                }
            }
        }

        MultiblockDisassembleEvent disassembleEvent = new MultiblockDisassembleEvent(coreDevice, blueprint, player, coreLoc.getBlock(), structureBlocks);
        Bukkit.getPluginManager().callEvent(disassembleEvent);

        if (removeCoreBlock && coreLoc.getWorld().isChunkLoaded(coreLoc.getBlockX() >> 4, coreLoc.getBlockZ() >> 4)) {
            ServerLevel nmsLevel = ((CraftWorld) coreLoc.getWorld()).getHandle();
            BlockPos pos = new BlockPos(coreLoc.getBlockX(), coreLoc.getBlockY(), coreLoc.getBlockZ());
            nmsLevel.setBlock(pos, Blocks.AIR.defaultBlockState(), 18);
        }
        MultiblockRegistry.instance.unregisterCore(coreDevice);
    }

    public static boolean isProtectedBlock(Block block) {
        Device device = Device.getDevice(block.getLocation());
        if (device instanceof PortProxyDevice proxy) {
            return proxy.isRequired();
        }
        return MultiblockUtil.isCoreDevice(device);
    }

    public static void handlePistonExtend(BlockPistonExtendEvent event) {
        for (Block block : event.getBlocks()) {
            if (isProtectedBlock(block)) {
                event.setCancelled(true);
                return;
            }
        }
    }

    public static void handlePistonRetract(BlockPistonRetractEvent event) {
        for (Block block : event.getBlocks()) {
            if (isProtectedBlock(block)) {
                event.setCancelled(true);
                return;
            }
        }
    }

    public static void handleBlockForm(BlockFormEvent event) {
        if (isProtectedBlock(event.getBlock())) event.setCancelled(true);
    }

    public static void handleBlockFade(BlockFadeEvent event) {
        if (isProtectedBlock(event.getBlock())) event.setCancelled(true);
    }

    public static void handleMoistureChange(MoistureChangeEvent event) {
        if (isProtectedBlock(event.getBlock())) event.setCancelled(true);
    }

    public static void handleBlockBurn(BlockBurnEvent event) {
        if (isProtectedBlock(event.getBlock())) event.setCancelled(true);
    }

    public static void handleBlockIgnite(BlockIgniteEvent event) {
        if (isProtectedBlock(event.getBlock())) event.setCancelled(true);
    }

    public static void handleBlockPhysics(BlockPhysicsEvent event) {
        if (isProtectedBlock(event.getBlock())) event.setCancelled(true);
    }

    public static void handleEntityChangeBlock(EntityChangeBlockEvent event) {
        if (isProtectedBlock(event.getBlock())) event.setCancelled(true);
    }

    public static void handleInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK && event.getAction() != Action.LEFT_CLICK_BLOCK) return;
        Block block = event.getClickedBlock();
        if (block != null && isProtectedBlock(block)) {
            if (event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getItem() != null) {
                String mat = event.getItem().getType().toString();
                if (mat.endsWith("_AXE") || mat.equals("HONEYCOMB") || mat.endsWith("_SHOVEL")) {
                    event.setCancelled(true);
                }
            }
        }
    }

    public static void handleRedstone(BlockRedstoneEvent event) {
        Device device = Device.getDevice(event.getBlock().getLocation());
        if (device instanceof PortProxyDevice proxy) {
            if (!(proxy.getPort() instanceof RedstonePort rp)) {
                event.setNewCurrent(0);
            } else {
                if (!rp.canEmit()) {
                    event.setNewCurrent(0);
                } else {
                    event.setNewCurrent(Math.max(event.getNewCurrent(), rp.getEmissionLevel(proxy.getCoreDevice())));
                }

                if (rp.canReceive()) {
                    rp.onRedstoneReceive(proxy.getCoreDevice(), event.getNewCurrent());
                }
            }
        }
    }
}