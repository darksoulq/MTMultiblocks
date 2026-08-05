package com.github.darksoulq.mt_multiblocks.port;

public interface ItemPort extends Port {
    boolean isInput();
    boolean isOutput();
    int[] getSlots();
}