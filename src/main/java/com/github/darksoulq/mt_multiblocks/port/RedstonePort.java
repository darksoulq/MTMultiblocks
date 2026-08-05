package com.github.darksoulq.mt_multiblocks.port;

public interface RedstonePort extends Port {
    boolean canReceive();
    boolean canEmit();
}