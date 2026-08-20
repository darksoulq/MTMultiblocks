package com.github.darksoulq.mt_multiblocks.model;

import com.github.darksoulq.mt_multiblocks.util.RotationUtil;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntityTypes;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ActiveModel {
    private final MultiblockModel blueprintModel;
    private final Map<String, ActiveModelPart> activeParts = new HashMap<>();
    private final Set<Player> viewers = new HashSet<>();
    private final AnimationController animationController;
    private final BoundingBox boundingBox;
    private final Location coreLocation;

    public ActiveModel(Location coreLoc, BlockFace facing, MultiblockModel blueprintModel) {
        this.blueprintModel = blueprintModel;
        this.coreLocation = coreLoc.clone();
        this.animationController = new AnimationController(this);

        Vector min = new Vector(coreLoc.getX(), coreLoc.getY(), coreLoc.getZ());
        Vector max = min.clone();

        ServerLevel nmsLevel = ((CraftWorld) coreLoc.getWorld()).getHandle();

        for (ModelPart part : blueprintModel.getParts()) {
            Location partLoc = coreLoc.clone();
            partLoc.setYaw(RotationUtil.getYawFromFace(facing));

            Display display;
            if (part.getDisplayClass() == Display.ItemDisplay.class) {
                display = new Display.ItemDisplay(EntityTypes.ITEM_DISPLAY, nmsLevel);
            } else if (part.getDisplayClass() == Display.TextDisplay.class) {
                display = new Display.TextDisplay(EntityTypes.TEXT_DISPLAY, nmsLevel);
            } else {
                display = new Display.BlockDisplay(EntityTypes.BLOCK_DISPLAY, nmsLevel);
            }

            display.setPos(partLoc.getX(), partLoc.getY(), partLoc.getZ());
            display.setYRot(partLoc.getYaw());
            display.setXRot(partLoc.getPitch());

            part.applyTo(display);

            activeParts.put(part.getId(), new ActiveModelPart(this, part, display));

            Vector trans = new Vector(part.getTranslateX(), part.getTranslateY(), part.getTranslateZ());
            trans = RotationUtil.rotateVector(trans, facing);
            Vector pLoc = coreLoc.toVector().add(trans);

            double r = Math.max(part.getScaleX(), Math.max(part.getScaleY(), part.getScaleZ()));
            min.setX(Math.min(min.getX(), pLoc.getX() - r));
            min.setY(Math.min(min.getY(), pLoc.getY() - r));
            min.setZ(Math.min(min.getZ(), pLoc.getZ() - r));

            max.setX(Math.max(max.getX(), pLoc.getX() + r));
            max.setY(Math.max(max.getY(), pLoc.getY() + r));
            max.setZ(Math.max(max.getZ(), pLoc.getZ() + r));
        }

        this.boundingBox = BoundingBox.of(min, max);
    }

    public MultiblockModel getBlueprintModel() { return blueprintModel; }
    public AnimationController getAnimationController() { return animationController; }
    public ActiveModelPart getPart(String id) { return activeParts.get(id); }
    public Collection<ActiveModelPart> getAllParts() { return activeParts.values(); }
    public Set<Player> getViewers() { return viewers; }
    public BoundingBox getBoundingBox() { return boundingBox; }
    public Location getCoreLocation() { return coreLocation; }

    public void spawnFor(Player p) {
        if (viewers.add(p)) {
            List<Packet<? super ClientGamePacketListener>> packets = new ArrayList<>();
            for (ActiveModelPart part : activeParts.values()) {
                part.appendSpawnPackets(packets);
            }
            if (!packets.isEmpty()) {
                ((CraftPlayer) p).getHandle().connection.send(new ClientboundBundlePacket(packets));
            }
        }
    }

    public void destroyFor(Player p) {
        if (viewers.remove(p)) {
            List<Packet<? super ClientGamePacketListener>> packets = new ArrayList<>();
            for (ActiveModelPart part : activeParts.values()) {
                part.appendDestroyPackets(packets);
            }
            if (!packets.isEmpty()) {
                ((CraftPlayer) p).getHandle().connection.send(new ClientboundBundlePacket(packets));
            }
        }
    }

    public void destroyAll() {
        for (Player p : new HashSet<>(viewers)) {
            destroyFor(p);
        }
        activeParts.clear();
    }

    public void broadcastDataUpdate(List<ActiveModelPart> updatedParts) {
        if (viewers.isEmpty()) return;
        List<Packet<? super ClientGamePacketListener>> packets = new ArrayList<>();
        for (ActiveModelPart part : updatedParts) {
            part.appendDataPackets(packets);
        }
        if (packets.isEmpty()) return;

        ClientboundBundlePacket bundle = new ClientboundBundlePacket(packets);
        for (Player p : viewers) {
            ((CraftPlayer) p).getHandle().connection.send(bundle);
        }
    }
}