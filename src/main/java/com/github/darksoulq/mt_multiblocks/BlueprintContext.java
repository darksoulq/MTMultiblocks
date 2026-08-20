package com.github.darksoulq.mt_multiblocks;

import com.MT.xxxtrigger50xxx.Devices.Device;
import com.github.darksoulq.mt_multiblocks.util.RotationUtil;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class BlueprintContext {
    private final Location coreLocation;
    private final BlockFace facing;
    private final Player player;
    private final ItemStack itemStack;
    private final Device device;

    public BlueprintContext(Location coreLocation, BlockFace facing, Player player, ItemStack itemStack, Device device) {
        this.coreLocation = coreLocation;
        this.facing = facing;
        this.player = player;
        this.itemStack = itemStack;
        this.device = device;
    }

    public BlueprintContext(Device device) {
        this.device = device;
        this.coreLocation = device != null ? device.getLocationREADONLY() : null;
        this.facing = RotationUtil.getFacingSafely(device);
        this.player = null;
        this.itemStack = null;
    }

    public Location getCoreLocation() {
        return coreLocation;
    }

    public BlockFace getFacing() {
        return facing;
    }

    public Player getPlayer() {
        return player;
    }

    public ItemStack getItemStack() {
        return itemStack;
    }

    public Device getDevice() {
        return device;
    }
}