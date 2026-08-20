package com.github.darksoulq.mt_multiblocks;

import com.github.darksoulq.mt_multiblocks.choice.ComponentChoice;
import com.github.darksoulq.mt_multiblocks.port.Port;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;

import java.io.Serial;

public final class OptionalComponent implements StructureComponent {
    @Serial
    private static final long serialVersionUID = 1L;

    private final ComponentChoice choice;
    private final Port port;

    public OptionalComponent(ComponentChoice choice, Port port) {
        this.choice = choice;
        this.port = port;
    }

    @Override
    public Port getPort() {
        return port;
    }

    @Override
    public boolean isRequired() {
        return false;
    }

    @Override
    public boolean matches(BlockData blockData, BlockFace facing) {
        return choice.matches(blockData, facing);
    }

    @Override
    public BlockData getDisplayData(BlockFace facing) {
        return choice.getDisplayData(facing);
    }

    @Override
    public BlockState getNMSDisplayData(BlockFace facing) {
        return choice.getNMSDisplayData(facing);
    }
}