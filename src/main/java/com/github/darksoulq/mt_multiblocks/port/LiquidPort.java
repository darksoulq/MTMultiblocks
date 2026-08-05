package com.github.darksoulq.mt_multiblocks.port;

import com.MT.xxxtrigger50xxx.Devices.Device;

public interface LiquidPort extends Port {
    boolean isInput();
    boolean isOutput();
    Device.LiquidType[] getLiquidTypes();
}