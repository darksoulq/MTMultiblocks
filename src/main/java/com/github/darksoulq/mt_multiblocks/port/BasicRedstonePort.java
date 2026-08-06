package com.github.darksoulq.mt_multiblocks.port;

import com.MT.xxxtrigger50xxx.Devices.Device;
import org.jetbrains.annotations.Range;

import java.io.Serial;

public class BasicRedstonePort implements RedstonePort {
    @Serial
    private static final long serialVersionUID = 1L;
    private final boolean canReceive;
    private final boolean canEmit;

    @Range(from = 0, to = 15)
    private int emissionLevel;

    public BasicRedstonePort(boolean canReceive, boolean canEmit) {
        this(canReceive, canEmit, 0);
    }

    public BasicRedstonePort(boolean canReceive, boolean canEmit, @Range(from = 0, to = 15) int emissionLevel) {
        this.canReceive = canReceive;
        this.canEmit = canEmit;
        this.setEmissionLevel(emissionLevel);
    }

    @Override
    public boolean canReceive() {
        return canReceive;
    }

    @Override
    public boolean canEmit() {
        return canEmit;
    }

    @Override
    public int getEmissionLevel(Device coreDevice) {
        return emissionLevel;
    }

    public void setEmissionLevel(@Range(from = 0, to = 15) int emissionLevel) {
        this.emissionLevel = emissionLevel;
    }

    @Override
    public void onRedstoneReceive(Device coreDevice, int powerLevel) {
    }
}