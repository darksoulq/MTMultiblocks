package com.github.darksoulq.mt_multiblocks.model;

import com.MT.xxxtrigger50xxx.Devices.Device;

import java.util.ArrayList;
import java.util.List;

public class AnimationController {

    private static class PartState {
        float translateX, translateY, translateZ, rotX, rotY, rotZ, scaleX = 1, scaleY = 1, scaleZ = 1;

        PartState() {}

        void setFrom(Keyframe kf) {
            this.translateX = kf.translateX; this.translateY = kf.translateY; this.translateZ = kf.translateZ;
            this.rotX = kf.rotX; this.rotY = kf.rotY; this.rotZ = kf.rotZ;
            this.scaleX = kf.scaleX; this.scaleY = kf.scaleY; this.scaleZ = kf.scaleZ;
        }
    }

    private static class PartAnimator {
        final ActiveModelPart part;
        final List<Keyframe> timeline;
        int keyframeIndex = 0;
        int ticksElapsed = 0;
        final PartState startState = new PartState();
        final PartState currentState = new PartState();

        PartAnimator(ActiveModelPart part, List<Keyframe> timeline) {
            this.part = part;
            this.timeline = timeline;
        }
    }

    private final ActiveModel model;
    private ModelAnimation currentAnimation;
    private boolean looping;
    private boolean isPlaying;

    private PartAnimator[] activeAnimators = new PartAnimator[0];
    private final List<ActiveModelPart> updatedPartsCache = new ArrayList<>();

    public AnimationController(ActiveModel model) {
        this.model = model;
    }

    public static AnimationController get(Device device) {
        ActiveModel model = ModelTracker.getModel(device);
        return model != null ? model.getAnimationController() : null;
    }

    public void play(ModelAnimation animation, boolean loop) {
        if (isPlaying && this.currentAnimation == animation) return;

        this.currentAnimation = animation;
        this.looping = loop;
        this.isPlaying = true;

        if (currentAnimation == null || currentAnimation.getPartIds().isEmpty()) {
            this.isPlaying = false;
            this.activeAnimators = new PartAnimator[0];
            return;
        }
        List<PartAnimator> animators = new ArrayList<>(currentAnimation.getPartIds().size());
        for (String partId : currentAnimation.getPartIds()) {
            ActiveModelPart part = model.getPart(partId);
            if (part == null) continue;

            List<Keyframe> timeline = currentAnimation.getTimeline(partId);
            if (timeline == null || timeline.isEmpty()) continue;

            animators.add(new PartAnimator(part, timeline));
        }
        this.activeAnimators = animators.toArray(new PartAnimator[0]);
    }

    public void stop() {
        if (!isPlaying) return;
        this.isPlaying = false;
        this.currentAnimation = null;
        this.activeAnimators = new PartAnimator[0];

        updatedPartsCache.clear();
        for (ActiveModelPart part : model.getAllParts()) {
            part.resetTransform();
            updatedPartsCache.add(part);
        }
        if (!updatedPartsCache.isEmpty()) {
            model.broadcastDataUpdate(updatedPartsCache);
        }
    }

    public boolean isPlaying() {
        return isPlaying;
    }

    public boolean isPlaying(ModelAnimation animation) {
        return isPlaying && this.currentAnimation == animation;
    }

    public void tick() {
        if (!isPlaying || currentAnimation == null) return;

        boolean anyActive = false;
        updatedPartsCache.clear();

        for (PartAnimator anim : activeAnimators) {
            if (anim.keyframeIndex >= anim.timeline.size()) {
                if (looping) {
                    anim.keyframeIndex = 0;
                    anim.ticksElapsed = 0;
                } else {
                    continue;
                }
            }

            anyActive = true;
            Keyframe target = anim.timeline.get(anim.keyframeIndex);

            if (anim.ticksElapsed == 0 && target.easing == Easing.LINEAR) {
                anim.part.applyTransform(target.translateX, target.translateY, target.translateZ, target.rotX, target.rotY, target.rotZ, target.scaleX, target.scaleY, target.scaleZ, target.durationTicks);
                updatedPartsCache.add(anim.part);
            } else if (target.easing != Easing.LINEAR) {
                float nextT = Math.min(1.0f, (float) (anim.ticksElapsed + 2) / target.durationTicks);
                float eased = target.easing.apply(nextT);

                PartState start = anim.startState;
                PartState current = anim.currentState;

                current.translateX = start.translateX + (target.translateX - start.translateX) * eased;
                current.translateY = start.translateY + (target.translateY - start.translateY) * eased;
                current.translateZ = start.translateZ + (target.translateZ - start.translateZ) * eased;
                current.rotX = start.rotX + (target.rotX - start.rotX) * eased;
                current.rotY = start.rotY + (target.rotY - start.rotY) * eased;
                current.rotZ = start.rotZ + (target.rotZ - start.rotZ) * eased;
                current.scaleX = start.scaleX + (target.scaleX - start.scaleX) * eased;
                current.scaleY = start.scaleY + (target.scaleY - start.scaleY) * eased;
                current.scaleZ = start.scaleZ + (target.scaleZ - start.scaleZ) * eased;

                anim.part.applyTransform(current.translateX, current.translateY, current.translateZ, current.rotX, current.rotY, current.rotZ, current.scaleX, current.scaleY, current.scaleZ, 2);

                updatedPartsCache.add(anim.part);
            }

            anim.ticksElapsed++;
            if (anim.ticksElapsed >= target.durationTicks) {
                anim.startState.setFrom(target);
                anim.currentState.setFrom(target);
                anim.ticksElapsed = 0;
                anim.keyframeIndex++;
            }
        }

        if (!updatedPartsCache.isEmpty()) {
            model.broadcastDataUpdate(updatedPartsCache);
        }

        if (!anyActive && !looping) {
            isPlaying = false;
            currentAnimation = null;
        }
    }
}