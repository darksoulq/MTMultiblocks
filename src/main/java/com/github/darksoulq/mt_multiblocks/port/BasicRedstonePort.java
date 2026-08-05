package com.github.darksoulq.mt_multiblocks.port;

import java.io.Serial;

public class BasicRedstonePort implements RedstonePort {
    @Serial
    private static final long serialVersionUID = 1L;
    private final boolean canReceive;
    private final boolean canEmit;

    public BasicRedstonePort(boolean canReceive, boolean canEmit) {
        this.canReceive = canReceive;
        this.canEmit = canEmit;
    }

    @Override
    public boolean canReceive() {
        return canReceive;
    }

    @Override
    public boolean canEmit() {
        return canEmit;
    }
}