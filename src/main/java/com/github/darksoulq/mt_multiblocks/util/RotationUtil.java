package com.github.darksoulq.mt_multiblocks.util;

import com.MT.xxxtrigger50xxx.Devices.Device;
import com.github.darksoulq.mt_multiblocks.MTMultiblock;
import org.bukkit.Axis;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.Orientable;
import org.bukkit.block.data.Rotatable;
import org.bukkit.util.Vector;

public class RotationUtil {

    public static BlockFace getFacingSafely(Device device) {
        if (device == null) return BlockFace.NORTH;
        try {
            if (device instanceof MTMultiblock mt) {
                return mt.getFacing();
            }
            if (device.getLocationREADONLY() != null) {
                Block block = device.getLocationREADONLY().getBlock();
                if (block != null) {
                    BlockData data = block.getBlockData();
                    if (data instanceof Directional dir) return dir.getFacing();
                    if (data instanceof Rotatable rot) return rot.getRotation();
                }
            }
        } catch (Exception ignored) {}
        return BlockFace.NORTH;
    }

    public static float getYawFromFace(BlockFace face) {
        return switch (face) {
            case EAST -> -90f;
            case SOUTH -> 0f;
            case WEST -> 90f;
            default -> 180f;
        };
    }

    public static byte getFaceIndex(BlockFace face) {
        return switch (face) {
            case NORTH -> 0;
            case EAST -> 1;
            case SOUTH -> 2;
            case WEST -> 3;
            default -> -1;
        };
    }

    public static Vector rotateVector(Vector v, BlockFace coreFacing) {
        double x = v.getX();
        double y = v.getY();
        double z = v.getZ();

        return switch (coreFacing) {
            case WEST -> new Vector(-z, y, x);
            case NORTH -> new Vector(-x, y, -z);
            case EAST -> new Vector(z, y, -x);
            default -> new Vector(x, y, z);
        };
    }

    public static BlockData rotateBlockData(BlockData data, BlockFace coreFacing) {
        if (coreFacing == BlockFace.SOUTH) return data.clone();
        BlockData cloned = data.clone();
        if (cloned instanceof Directional dir) {
            dir.setFacing(rotateFace(dir.getFacing(), coreFacing));
        }
        if (cloned instanceof Rotatable rot) {
            rot.setRotation(rotateFace(rot.getRotation(), coreFacing));
        }
        if (cloned instanceof Orientable ori) {
            if (coreFacing == BlockFace.EAST || coreFacing == BlockFace.WEST) {
                if (ori.getAxis() == Axis.X) ori.setAxis(Axis.Z);
                else if (ori.getAxis() == Axis.Z) ori.setAxis(Axis.X);
            }
        }
        return cloned;
    }

    private static BlockFace rotateFace(BlockFace original, BlockFace coreFacing) {
        if (original == BlockFace.UP || original == BlockFace.DOWN) return original;
        int rotations = 0;
        switch (coreFacing) {
            case WEST: rotations = 1; break;
            case NORTH: rotations = 2; break;
            case EAST: rotations = 3; break;
            case SOUTH: default: return original;
        }
        BlockFace current = original;
        for (int i = 0; i < rotations; i++) {
            switch (current) {
                case NORTH: current = BlockFace.EAST; break;
                case EAST: current = BlockFace.SOUTH; break;
                case SOUTH: current = BlockFace.WEST; break;
                case WEST: current = BlockFace.NORTH; break;
                case NORTH_EAST: current = BlockFace.SOUTH_EAST; break;
                case SOUTH_EAST: current = BlockFace.SOUTH_WEST; break;
                case SOUTH_WEST: current = BlockFace.NORTH_WEST; break;
                case NORTH_WEST: current = BlockFace.NORTH_EAST; break;
                default: break;
            }
        }
        return current;
    }
}