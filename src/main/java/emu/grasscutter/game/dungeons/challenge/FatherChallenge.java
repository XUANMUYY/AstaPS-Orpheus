package emu.grasscutter.game.dungeons.challenge;

import emu.grasscutter.game.dungeons.challenge.enums.FatherChallengeProperty;
import emu.grasscutter.game.dungeons.challenge.trigger.InTimeTrigger;
import emu.grasscutter.game.entity.EntityGadget;
import emu.grasscutter.game.entity.EntityMonster;
import emu.grasscutter.game.world.Scene;
import emu.grasscutter.scripts.data.SceneGroup;
import emu.grasscutter.scripts.data.SceneTrigger;
import emu.grasscutter.server.packet.send.PacketChallengeDataNotify;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** A timed challenge whose progress comes from attached child challenge results. */
public final class FatherChallenge extends WorldChallenge {
    private final Map<Integer, Child> children = new ConcurrentHashMap<>();
    private int successGoal;
    private int failGoal;
    private int successPoints;
    private int failPoints;
    private boolean started;

    private record Child(WorldChallenge challenge, int successWeight, int failWeight) {}

    public FatherChallenge(Scene scene, SceneGroup group, int index, int id, int duration,
                           int successGoal, int failGoal) {
        super(scene, group, id, index, List.of(duration, successGoal, failGoal), duration,
                successGoal, List.of(new InTimeTrigger()));
        this.successGoal = successGoal;
        this.failGoal = failGoal;
    }

    public synchronized boolean attach(WorldChallenge child, int successWeight, int failWeight) {
        if (children.containsKey(child.getChallengeIndex()) || successWeight < 0 || failWeight < 0
                || (started && !inProgress())) return false;
        child.setFatherChallenge(this);
        child.setSourceRegionEntityId(getSourceRegionEntityId());
        children.put(child.getChallengeIndex(), new Child(child, successWeight, failWeight));
        if (inProgress()) child.start();
        return true;
    }

    public WorldChallenge activeChild(int index) {
        var child = children.get(index);
        return child != null && child.challenge().inProgress() ? child.challenge() : null;
    }

    public int childCount() {
        return children.size();
    }

    public int successPoints() {
        return successPoints;
    }

    public int failPoints() {
        return failPoints;
    }

    @Override
    public synchronized void start() {
        if (started) return;
        super.start();
        if (inProgress()) {
            started = true;
            for (var child : List.copyOf(children.values())) child.challenge().start();
        }
    }

    synchronized void onChildFinished(WorldChallenge challenge, boolean success) {
        var child = children.get(challenge.getChallengeIndex());
        if (child == null || child.challenge() != challenge || !inProgress()) return;
        children.remove(challenge.getChallengeIndex());
        if (success) successPoints += child.successWeight();
        else failPoints += child.failWeight();
        getScore().set(successPoints);
        getScene().broadcastPacket(new PacketChallengeDataNotify(this, 1, successPoints));
        getScene().broadcastPacket(new PacketChallengeDataNotify(this, 2, failPoints));
        checkOutcome();
    }

    public synchronized boolean modify(FatherChallengeProperty property, int value) {
        if (value < 0 || !inProgress()) return false;
        switch (property) {
            case DURATION -> {
                setTimeLimit(value);
                getScene().broadcastPacket(new PacketChallengeDataNotify(this, 2, getStartedAt() + value));
            }
            case CUR_SUCC -> {
                successPoints = value;
                getScene().broadcastPacket(new PacketChallengeDataNotify(this, 1, value));
            }
            case CUR_FAIL -> {
                failPoints = value;
                getScene().broadcastPacket(new PacketChallengeDataNotify(this, 2, value));
            }
            case SUM_SUCC -> successGoal = value;
            case SUM_FAIL -> failGoal = value;
        }
        getScore().set(successPoints);
        checkOutcome();
        return true;
    }

    private void checkOutcome() {
        // 推断：Lua 的 success/fail 和 CUR_*/SUM_* 分别是积分与结算阈值。
        if (successGoal > 0 && successPoints >= successGoal) done();
        else if (failGoal > 0 && failPoints >= failGoal) fail();
    }

    private void stopChildren() {
        for (var child : List.copyOf(children.values())) {
            if (child.challenge().inProgress()) child.challenge().abortQuiet();
        }
        children.clear();
    }

    @Override public synchronized void done() {
        if (!inProgress()) return;
        stopChildren();
        super.done();
    }

    @Override public synchronized void fail() {
        if (!inProgress()) return;
        stopChildren();
        super.fail();
    }

    @Override public void onCheckTimeOut() {
        super.onCheckTimeOut();
        if (inProgress()) for (var child : List.copyOf(children.values())) child.challenge().onCheckTimeOut();
    }

    @Override public void onMonsterDeath(EntityMonster monster) {
        for (var child : List.copyOf(children.values())) child.challenge().onMonsterDeath(monster);
    }

    @Override public void onGadgetDeath(EntityGadget gadget) {
        for (var child : List.copyOf(children.values())) child.challenge().onGadgetDeath(gadget);
    }

    @Override public void onGadgetDamage(EntityGadget gadget) {
        for (var child : List.copyOf(children.values())) child.challenge().onGadgetDamage(gadget);
    }

    @Override public void onGroupTriggerDeath(SceneTrigger trigger) {
        for (var child : List.copyOf(children.values())) child.challenge().onGroupTriggerDeath(trigger);
    }
}
