package com.github.darksoulq.mt_multiblocks.core;

import com.MT.xxxtrigger50xxx.Devices.Device;
import com.MT.xxxtrigger50xxx.Guide.ItemMenu;
import org.bukkit.inventory.ItemStack;

import java.util.function.Supplier;

public class AddonDeviceTemplate {

    private final String id;
    private String name;
    private String description = "";
    private String modelData = "none";
    private Class<? extends Device> deviceClass;
    private ItemMenu.Category category = ItemMenu.Category.LOGISTICS;
    private Supplier<Device> prototypeSupplier;

    public AddonDeviceTemplate(String id) {
        this.id = id;
        this.name = id;
    }

    public AddonDeviceTemplate name(String name) {
        this.name = name;
        return this;
    }

    public AddonDeviceTemplate description(String description) {
        this.description = description;
        return this;
    }

    public AddonDeviceTemplate modelData(String modelData) {
        this.modelData = modelData;
        return this;
    }

    public AddonDeviceTemplate deviceClass(Class<? extends Device> deviceClass) {
        this.deviceClass = deviceClass;
        return this;
    }

    public AddonDeviceTemplate category(ItemMenu.Category category) {
        this.category = category;
        return this;
    }

    public AddonDeviceTemplate prototype(Supplier<Device> prototypeSupplier) {
        this.prototypeSupplier = prototypeSupplier;
        return this;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getModelData() {
        return modelData;
    }

    public Class<? extends Device> getDeviceClass() {
        return deviceClass;
    }

    public ItemMenu.Category getCategory() {
        return category;
    }

    public Supplier<Device> getPrototypeSupplier() {
        return prototypeSupplier;
    }

    public ItemStack getPrototypeStack() {
        if (prototypeSupplier == null) return null;
        Device proto = prototypeSupplier.get();
        return proto != null ? proto.getDeviceStack() : null;
    }
}