package com.github.darksoulq.mt_multiblocks.port;

import java.io.Serial;

public class BasicItemPort implements ItemPort {
    @Serial
    private static final long serialVersionUID = 1L;
    private final boolean isInput;
    private final boolean isOutput;
    private final int[] slots;

    public BasicItemPort(boolean isInput, boolean isOutput) {
        this(isInput, isOutput, null);
    }

    public BasicItemPort(boolean isInput, boolean isOutput, int[] slots) {
        this.isInput = isInput;
        this.isOutput = isOutput;
        this.slots = slots;
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
    public int[] getSlots() {
        return slots;
    }
}