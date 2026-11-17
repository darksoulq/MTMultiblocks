package com.github.darksoulq.mt_multiblocks.core;

import com.MT.triggersUtility.TUMaths;
import com.MT.xxxtrigger50xxx.Devices.Device;
import com.MT.xxxtrigger50xxx.PowerGrid.PowerGrid;
import com.github.darksoulq.mt_multiblocks.BlueprintContext;
import com.github.darksoulq.mt_multiblocks.MultiblockBlueprint;
import com.github.darksoulq.mt_multiblocks.StructureComponent;
import com.github.darksoulq.mt_multiblocks.port.InteractPort;
import com.github.darksoulq.mt_multiblocks.port.ItemPort;
import com.github.darksoulq.mt_multiblocks.port.LiquidPort;
import com.github.darksoulq.mt_multiblocks.port.Port;
import com.github.darksoulq.mt_multiblocks.port.RedstonePort;
import com.github.darksoulq.mt_multiblocks.port.TickablePort;
import com.github.darksoulq.mt_multiblocks.registry.MultiblockRegistry;
import com.github.darksoulq.mt_multiblocks.util.MultiblockUtil;
import com.github.darksoulq.mt_multiblocks.util.RotationUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

public class PortProxyDevice extends AddonDevice {
    private String coreLocationString;
    private transient Device coreDevice;
    private transient StructureComponent component;
    private transient Port port;
    private byte tickCounter = 0;
    private transient boolean shuttingDown = false;

    public PortProxyDevice(Location portLocation, Device coreDevice, StructureComponent component) {
        super(portLocation);
        this.coreLocationString = TUMaths.locationString(coreDevice.getLocationREADONLY());
        this.coreDevice = coreDevice;
        this.component = component;
        this.port = component.getPort();
        this.standardMoverBehavior = true;
        this.chainingMoverBehavior = false;
        this.deviceName = coreDevice.getName() + " [Port]";
    }

    @Override
    public void onCreate(boolean isFirstTime) {
        super.onCreate(isFirstTime);
        if (!isFirstTime) {
            Location coreLoc = TUMaths.getStringLocation(coreLocationString);
            coreDevice = Device.getDevice(coreLoc);
            if (coreDevice != null) {
                this.deviceName = coreDevice.getName() + " [Port]";
                BlueprintContext context = new BlueprintContext(coreDevice);
                MultiblockBlueprint blueprint = MultiblockUtil.getTargetBlueprint(coreDevice, context);
                if (blueprint != null) {
                    BlockFace facing = context.getFacing();
                    Map<Vector, StructureComponent> structure = blueprint.buildStructure(context);
                    
                    Vector relative = this.getLocationREADONLY().clone().subtract(coreLoc).toVector();
                    Vector original = RotationUtil.rotateVector(relative, facing.getOppositeFace());
                    
                    for (Map.Entry<Vector, StructureComponent> compEntry : structure.entrySet()) {
                        if (compEntry.getKey().distanceSquared(original) < 0.1) {
                            this.component = compEntry.getValue();
                            this.port = this.component.getPort();
                            break;
                        }
                    }
                    if (this.component != null) {
                        MultiblockRegistry.instance.registerSecondaryBlock(this.getLocationREADONLY(), coreDevice, component);
                    } else {
                        this.removeDevice();
                    }
                } else {
                    this.removeDevice();
                }
            } else {
                this.removeDevice();
            }
        }
    }

    public void prepareForShutdown() {
        this.shuttingDown = true;
    }

    public Device getCoreDevice() {
        return coreDevice;
    }

    public Port getPort() {
        return port;
    }

    public StructureComponent getComponent() {
        return component;
    }

    public boolean isRequired() {
        return component != null && component.isRequired();
    }

    @Override
    public ItemStack getDeviceStack() {
        return new ItemStack(Material.AIR);
    }

    @Override
    public ItemStack getDeviceStack(boolean prototype) {
        return new ItemStack(Material.AIR);
    }

    @Override
    public boolean allowGridConnectionTick() {
        return false;
    }

    @Override
    public boolean tickGrid() {
        return false;
    }

    @Override
    public int getGridScore() {
        return 0;
    }

    @Override
    public void tickDeviceAction() {
        if (coreDevice == null || coreDevice.isRemoved() || shuttingDown) return;

        if (port instanceof RedstonePort rp) {
            Location loc = this.getLocationREADONLY();
            if (!loc.getWorld().isChunkLoaded(loc.getBlockX() >> 4, loc.getBlockZ() >> 4)) return;

            ServerLevel nmsLevel = ((CraftWorld) loc.getWorld()).getHandle();
            BlockPos pos = new BlockPos(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());

            if (rp.canReceive()) {
                int power = nmsLevel.getBestNeighborSignal(pos);
                rp.onRedstoneReceive(coreDevice, power);
            }

            if (rp.canEmit()) {
                int emission = rp.getEmissionLevel(coreDevice);
                if (emission > 0) {
                    for (BlockFace face : new BlockFace[]{BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST, BlockFace.UP, BlockFace.DOWN}) {
                        Block relative = loc.getBlock().getRelative(face);
                        if (relative.getBlockData() instanceof org.bukkit.block.data.AnaloguePowerable ap) {
                            if (ap.getMaximumPower() >= emission && ap.getPower() < emission) {
                                ap.setPower(emission);
                                relative.setBlockData(ap, true);
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    public Inventory getInventory() {
        return coreDevice != null ? coreDevice.getInventory() : null;
    }

    @Override
    public ArrayList<Integer> getInSlots() {
        if (coreDevice != null && port instanceof ItemPort itemPort) {
            if (itemPort.isInput()) {
                int[] slots = itemPort.getSlots();
                if (slots != null && slots.length > 0) {
                    ArrayList<Integer> slotList = new ArrayList<>(slots.length);
                    for (int s : slots) slotList.add(s);
                    return slotList;
                }
                return coreDevice.getInSlots();
            }
        }
        return new ArrayList<>();
    }

    @Override
    public ArrayList<Integer> getOutSlots() {
        if (coreDevice != null && port instanceof ItemPort itemPort) {
            if (itemPort.isOutput()) {
                int[] slots = itemPort.getSlots();
                if (slots != null && slots.length > 0) {
                    ArrayList<Integer> slotList = new ArrayList<>(slots.length);
                    for (int s : slots) slotList.add(s);
                    return slotList;
                }
                return coreDevice.getOutSlots();
            }
        }
        return new ArrayList<>();
    }

    @Override
    public ArrayList<Device.LiquidType> getLiquidTypes() {
        if (shuttingDown) return new ArrayList<>();
        if (coreDevice != null && port instanceof LiquidPort liquidPort) {
            Device.LiquidType[] types = liquidPort.getLiquidTypes();
            if (types != null && types.length > 0) {
                return new ArrayList<>(Arrays.asList(types));
            }
            return coreDevice.getLiquidTypes();
        }
        return new ArrayList<>();
    }

    @Override
    public void addLiquidType(Device.LiquidType type, int max) {
        if (coreDevice != null && port instanceof LiquidPort) coreDevice.addLiquidType(type, max);
    }

    @Override
    public void removeLiquidType(Device.LiquidType type) {
        if (coreDevice != null && port instanceof LiquidPort) coreDevice.removeLiquidType(type);
    }

    @Override
    public int getStoredLiquid(Device.LiquidType type) {
        if (shuttingDown) return 0;
        return (coreDevice != null && port instanceof LiquidPort) ? coreDevice.getStoredLiquid(type) : 0;
    }

    @Override
    public void setStoredLiquid(Device.LiquidType type, int amount) {
        if (coreDevice != null && port instanceof LiquidPort) coreDevice.setStoredLiquid(type, amount);
    }

    @Override
    public int getMaxLiquid(Device.LiquidType type) {
        return (coreDevice != null && port instanceof LiquidPort) ? coreDevice.getMaxLiquid(type) : 0;
    }

    @Override
    public void setMaxLiquid(Device.LiquidType type, int amount) {
        if (coreDevice != null && port instanceof LiquidPort) coreDevice.setMaxLiquid(type, amount);
    }

    @Override
    public void addLiquid(Device.LiquidType type, int amount) {
        if (coreDevice != null && port instanceof LiquidPort liquidPort && liquidPort.isInput()) {
            Device.LiquidType[] allowed = liquidPort.getLiquidTypes();
            if (allowed == null || allowed.length == 0 || containsLiquid(allowed, type)) {
                coreDevice.addLiquid(type, amount);
            }
        }
    }

    @Override
    public void removeLiquid(Device.LiquidType type, int amount) {
        if (coreDevice != null && port instanceof LiquidPort liquidPort && liquidPort.isOutput()) {
            Device.LiquidType[] allowed = liquidPort.getLiquidTypes();
            if (allowed == null || allowed.length == 0 || containsLiquid(allowed, type)) {
                coreDevice.removeLiquid(type, amount);
            }
        }
    }

    @Override
    public boolean hasLiquid(Device.LiquidType type, int amount) {
        return coreDevice != null && port instanceof LiquidPort && coreDevice.hasLiquid(type, amount);
    }

    @Override
    public boolean acceptsLiquid(Device.LiquidType type) {
        if (coreDevice != null && port instanceof LiquidPort liquidPort && liquidPort.isInput()) {
            Device.LiquidType[] allowed = liquidPort.getLiquidTypes();
            if (allowed == null || allowed.length == 0 || containsLiquid(allowed, type)) {
                return coreDevice.acceptsLiquid(type);
            }
        }
        return false;
    }

    private boolean containsLiquid(Device.LiquidType[] array, Device.LiquidType type) {
        for (Device.LiquidType t : array) if (t == type) return true;
        return false;
    }

    @Override
    public PowerGrid getGrid() {
        return coreDevice != null ? coreDevice.getGrid() : null;
    }

    @Override
    public boolean onRightClick(PlayerInteractEvent event) {
        return coreDevice != null && port instanceof InteractPort && coreDevice.onRightClick(event);
    }

    @Override
    public void openDeviceInventory(Player player) {
        if (coreDevice != null && port instanceof InteractPort) coreDevice.openDeviceInventory(player);
    }

    @Override
    public boolean isUseUI() {
        return coreDevice != null && port instanceof InteractPort && coreDevice.isUseUI();
    }

    @Override
    public boolean isOpenable() {
        return coreDevice != null && port instanceof InteractPort && coreDevice.isOpenable();
    }

    @Override
    public void secondTick() {
        if (coreDevice == null || coreDevice.isRemoved()) {
            this.removeDevice();
            return;
        }

        Location loc = this.getLocationREADONLY();
        if (!loc.getWorld().isChunkLoaded(loc.getBlockX() >> 4, loc.getBlockZ() >> 4)) {
            return;
        }

        if (++tickCounter >= 20) {
            this.tickCounter = 0;
            BlockFace face = RotationUtil.getFacingSafely(coreDevice);
            Block block = loc.getBlock();

            if (component != null && !component.matches(block.getBlockData(), face)) {
                if (isRequired()) {
                    Behaviors.handleValidationFailure(coreDevice);
                } else {
                    Behaviors.handleOptionalValidationFailure(this);
                }
                return;
            }
        }

        if (port instanceof TickablePort tp) {
            tp.onTick(loc);
        }
    }

    @Override
    public void updateUI() {}

    @Override
    public void action() {}
}