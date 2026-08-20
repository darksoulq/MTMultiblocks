package com.github.darksoulq.mt_multiblocks.model;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

public class VisibilityCuller {
    public static VisibilityState getVisibility(Player p, ActiveModel model) {
        Location eyeLoc = p.getEyeLocation();
        if (!eyeLoc.getWorld().equals(model.getCoreLocation().getWorld())) return VisibilityState.HIDDEN;

        BoundingBox box = model.getBoundingBox();
        Vector center = box.getCenter();
        double distSq = center.distanceSquared(eyeLoc.toVector());

        if (distSq > 2025) return VisibilityState.CULLED_DISTANCE;

        Vector dir = eyeLoc.getDirection();
        Vector toBox = center.clone().subtract(eyeLoc.toVector());
        double dist = toBox.length();

        if (dist > 0) {
            toBox.normalize();
            double dot = dir.dot(toBox);
            double boxRadius = box.getMax().distance(box.getMin()) / 2.0;
            double threshold = 0.707 - (boxRadius / dist);

            if (dot < threshold && dist > boxRadius) {
                return VisibilityState.CULLED_FRUSTUM;
            }
        }

        return VisibilityState.VISIBLE;
    }
}