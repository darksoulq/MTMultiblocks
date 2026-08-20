package com.github.darksoulq.mt_multiblocks.model;

import com.mojang.math.Transformation;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.world.entity.Display;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.List;

public class ActiveModelPart {
    private final ActiveModel parentModel;
    private final ModelPart partDef;
    private final Display displayEntity;
    private final Quaternionf rightRotCache;

    public ActiveModelPart(ActiveModel parentModel, ModelPart partDef, Display displayEntity) {
        this.parentModel = parentModel;
        this.partDef = partDef;
        this.displayEntity = displayEntity;

        this.rightRotCache = new Quaternionf(partDef.getRightRotX(), partDef.getRightRotY(), partDef.getRightRotZ(), partDef.getRightRotW());
        this.displayEntity.setTransformationInterpolationDelay(0);
    }

    public Display getEntity() {
        return displayEntity;
    }

    public void applyTransform(float animTranslateX, float animTranslateY, float animTranslateZ, float animRotX, float animRotY, float animRotZ, float animScaleX, float animScaleY, float animScaleZ, int duration) {
        displayEntity.setTransformationInterpolationDuration(duration);

        Vector3f translation = new Vector3f(partDef.getTranslateX() + animTranslateX, partDef.getTranslateY() + animTranslateY, partDef.getTranslateZ() + animTranslateZ);
        Vector3f scale = new Vector3f(partDef.getScaleX() * animScaleX, partDef.getScaleY() * animScaleY, partDef.getScaleZ() * animScaleZ);
        Quaternionf leftRot = new Quaternionf(partDef.getLeftRotX(), partDef.getLeftRotY(), partDef.getLeftRotZ(), partDef.getLeftRotW());

        float radX = animRotX * 0.017453292F;
        float radY = animRotY * 0.017453292F;
        float radZ = animRotZ * 0.017453292F;

        if (radX != 0.0F) leftRot.rotateX(radX);
        if (radY != 0.0F) leftRot.rotateY(radY);
        if (radZ != 0.0F) leftRot.rotateZ(radZ);

        displayEntity.setTransformation(new Transformation(translation, leftRot, scale, rightRotCache));
    }

    public void resetTransform() {
        displayEntity.setTransformationInterpolationDuration(10);
        partDef.applyTo(displayEntity);
    }

    public void appendSpawnPackets(List<Packet<? super ClientGamePacketListener>> packets) {
        packets.add(new ClientboundAddEntityPacket(
            displayEntity.getId(), displayEntity.getUUID(),
            displayEntity.getX(), displayEntity.getY(), displayEntity.getZ(),
            displayEntity.getXRot(), displayEntity.getYRot(),
            displayEntity.getType(), 0, Vec3.ZERO, displayEntity.getYHeadRot()
        ));

        var data = displayEntity.getEntityData().getNonDefaultValues();
        if (data != null) {
            packets.add(new ClientboundSetEntityDataPacket(displayEntity.getId(), data));
        }
    }

    public void appendDataPackets(List<Packet<? super ClientGamePacketListener>> packets) {
        var data = displayEntity.getEntityData().packDirty();
        if (data != null && !data.isEmpty()) {
            packets.add(new ClientboundSetEntityDataPacket(displayEntity.getId(), data));
        }
    }

    public void appendDestroyPackets(List<Packet<? super ClientGamePacketListener>> packets) {
        packets.add(new ClientboundRemoveEntitiesPacket(displayEntity.getId()));
    }
}