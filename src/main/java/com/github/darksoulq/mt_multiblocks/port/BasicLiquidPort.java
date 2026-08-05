package com.github.darksoulq.mt_multiblocks.port;

import com.MT.xxxtrigger50xxx.Devices.Device;

import java.io.Serial;

public class BasicLiquidPort implements LiquidPort {
    @Serial
    private static final long serialVersionUID = 1L;
    private final boolean isInput;
    private final boolean isOutput;
    private final Device.LiquidType[] liquidTypes;

    public BasicLiquidPort(boolean isInput, boolean isOutput) {
        this(isInput, isOutput, null);
    }

    public BasicLiquidPort(boolean isInput, boolean isOutput, Device.LiquidType[] liquidTypes) {
        this.isInput = isInput;
        this.isOutput = isOutput;
        this.liquidTypes = liquidTypes;
    }

    @Override
    public boolean isInput() {
        return isInput;
    }

    @Override
    public boolean isOutput() {
        return isOutput;
    }

    @Override
    public Device.LiquidType[] getLiquidTypes() {
        return liquidTypes;
    }
}