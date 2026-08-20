package com.github.darksoulq.mt_multiblocks.model;

import com.mojang.math.Transformation;
import io.papermc.paper.adventure.PaperAdventure;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Display;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.Bukkit;
import org.bukkit.block.data.BlockData;
import org.bukkit.craftbukkit.block.data.CraftBlockData;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.io.Serializable;

public class ModelPart implements Serializable {
    private final String id;
    private final Class<? extends Display> displayClass;

    private float translateX = 0, translateY = 0, translateZ = 0;
    private float scaleX = 1, scaleY = 1, scaleZ = 1;
    private float leftRotX = 0, leftRotY = 0, leftRotZ = 0, leftRotW = 1;
    private float rightRotX = 0, rightRotY = 0, rightRotZ = 0, rightRotW = 1;

    private String blockDataString;
    private transient BlockState nmsBlockStateCache;

    private transient org.bukkit.inventory.ItemStack bukkitItemStack;
    private transient ItemStack nmsItemStackCache;

    private String textJson;
    private transient net.kyori.adventure.text.Component textComponent;
    private transient Component nmsTextComponentCache;

    private Display.BillboardConstraints billboard = Display.BillboardConstraints.FIXED;
    private ItemDisplayContext itemTransform = ItemDisplayContext.FIXED;

    private ModelPart(String id, Class<? extends Display> displayClass) {
        this.id = id;
        this.displayClass = displayClass;
    }

    public static ModelPart block(String id, BlockData data) {
        ModelPart part = new ModelPart(id, Display.BlockDisplay.class);
        part.blockDataString = data.getAsString();
        return part;
    }

    public static ModelPart item(String id, org.bukkit.inventory.ItemStack stack) {
        ModelPart part = new ModelPart(id, Display.ItemDisplay.class);
        part.bukkitItemStack = stack;
        return part;
    }

    public static ModelPart text(String id, net.kyori.adventure.text.Component text) {
        ModelPart part = new ModelPart(id, Display.TextDisplay.class);
        part.textComponent = text;
        part.textJson = GsonComponentSerializer.gson().serialize(text);
        return part;
    }

    public ModelPart translate(double x, double y, double z) {
        this.translateX = (float) x; this.translateY = (float) y; this.translateZ = (float) z;
        return this;
    }

    public ModelPart scale(double x, double y, double z) {
        this.scaleX = (float) x; this.scaleY = (float) y; this.scaleZ = (float) z;
        return this;
    }

    public ModelPart leftRot(Quaternionf rot) {
        this.leftRotX = rot.x; this.leftRotY = rot.y; this.leftRotZ = rot.z; this.leftRotW = rot.w;
        return this;
    }

    public ModelPart rightRot(Quaternionf rot) {
        this.rightRotX = rot.x; this.rightRotY = rot.y; this.rightRotZ = rot.z; this.rightRotW = rot.w;
        return this;
    }

    public ModelPart rotateX(float deg) {
        Quaternionf q = new Quaternionf(leftRotX, leftRotY, leftRotZ, leftRotW);
        q.rotateX((float) Math.toRadians(deg));
        this.leftRotX = q.x; this.leftRotY = q.y; this.leftRotZ = q.z; this.leftRotW = q.w;
        return this;
    }

    public ModelPart rotateY(float deg) {
        Quaternionf q = new Quaternionf(leftRotX, leftRotY, leftRotZ, leftRotW);
        q.rotateY((float) Math.toRadians(deg));
        this.leftRotX = q.x; this.leftRotY = q.y; this.leftRotZ = q.z; this.leftRotW = q.w;
        return this;
    }

    public ModelPart rotateZ(float deg) {
        Quaternionf q = new Quaternionf(leftRotX, leftRotY, leftRotZ, leftRotW);
        q.rotateZ((float) Math.toRadians(deg));
        this.leftRotX = q.x; this.leftRotY = q.y; this.leftRotZ = q.z; this.leftRotW = q.w;
        return this;
    }

    public ModelPart billboard(org.bukkit.entity.Display.Billboard billboard) {
        this.billboard = convertBillboardToNMS(billboard);
        return this;
    }

    public ModelPart itemTransform(org.bukkit.entity.ItemDisplay.ItemDisplayTransform transform) {
        this.itemTransform = convertTransformToNMS(transform);
        return this;
    }

    public String getId() { return id; }
    public Class<? extends Display> getDisplayClass() { return displayClass; }
    public float getTranslateX() { return translateX; }
    public float getTranslateY() { return translateY; }
    public float getTranslateZ() { return translateZ; }
    public float getScaleX() { return scaleX; }
    public float getScaleY() { return scaleY; }
    public float getScaleZ() { return scaleZ; }
    public float getLeftRotX() { return leftRotX; }
    public float getLeftRotY() { return leftRotY; }
    public float getLeftRotZ() { return leftRotZ; }
    public float getLeftRotW() { return leftRotW; }
    public float getRightRotX() { return rightRotX; }
    public float getRightRotY() { return rightRotY; }
    public float getRightRotZ() { return rightRotZ; }
    public float getRightRotW() { return rightRotW; }

    private void initNMSCache() {
        if (blockDataString != null && nmsBlockStateCache == null) {
            nmsBlockStateCache = ((CraftBlockData) Bukkit.createBlockData(blockDataString)).getState();
        }
        if (bukkitItemStack != null && nmsItemStackCache == null) {
            nmsItemStackCache = CraftItemStack.asNMSCopy(bukkitItemStack);
        }

        if (textJson != null && textComponent == null) {
            textComponent = GsonComponentSerializer.gson().deserialize(textJson);
        }
        if (textComponent != null && nmsTextComponentCache == null) {
            nmsTextComponentCache = PaperAdventure.asVanilla(textComponent);
        }
    }

    public void applyTo(Display display) {
        initNMSCache();

        display.setTransformation(new Transformation(
            new Vector3f(translateX, translateY, translateZ),
            new Quaternionf(leftRotX, leftRotY, leftRotZ, leftRotW),
            new Vector3f(scaleX, scaleY, scaleZ),
            new Quaternionf(rightRotX, rightRotY, rightRotZ, rightRotW)
        ));
        display.setBillboardConstraints(billboard);

        switch (display) {
            case Display.BlockDisplay bd when nmsBlockStateCache != null -> bd.setBlockState(nmsBlockStateCache);
            case Display.ItemDisplay id when nmsItemStackCache != null -> {
                id.setItemStack(nmsItemStackCache);
                id.setItemTransform(itemTransform);
            }
            case Display.TextDisplay td when nmsTextComponentCache != null -> td.setText(nmsTextComponentCache);
            default -> {
            }
        }
    }

    private Display.BillboardConstraints convertBillboardToNMS(org.bukkit.entity.Display.Billboard billboard) {
        return switch (billboard) {
            case VERTICAL -> Display.BillboardConstraints.VERTICAL;
            case HORIZONTAL -> Display.BillboardConstraints.HORIZONTAL;
            case CENTER -> Display.BillboardConstraints.CENTER;
            case null, default -> Display.BillboardConstraints.FIXED;
        };
    }

    private ItemDisplayContext convertTransformToNMS(org.bukkit.entity.ItemDisplay.ItemDisplayTransform transform) {
        return switch (transform) {
            case THIRDPERSON_LEFTHAND -> ItemDisplayContext.THIRD_PERSON_LEFT_HAND;
            case THIRDPERSON_RIGHTHAND -> ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;
            case FIRSTPERSON_LEFTHAND -> ItemDisplayContext.FIRST_PERSON_LEFT_HAND;
            case FIRSTPERSON_RIGHTHAND -> ItemDisplayContext.FIRST_PERSON_RIGHT_HAND;
            case HEAD -> ItemDisplayContext.HEAD;
            case GUI -> ItemDisplayContext.GUI;
            case GROUND -> ItemDisplayContext.GROUND;
            case FIXED -> ItemDisplayContext.FIXED;
            case null, default -> ItemDisplayContext.NONE;
        };
    }
}