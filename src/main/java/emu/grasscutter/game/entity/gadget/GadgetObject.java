package emu.grasscutter.game.entity.gadget;

import emu.grasscutter.data.GameData;
import emu.grasscutter.data.excels.*;
import emu.grasscutter.game.entity.EntityGadget;
import emu.grasscutter.game.inventory.GameItem;
import emu.grasscutter.game.player.Player;
import emu.grasscutter.game.props.ActionReason;
import emu.grasscutter.net.proto.*;
import emu.grasscutter.scripts.constants.EventType;
import emu.grasscutter.scripts.data.ScriptArgs;
import emu.grasscutter.net.packet.BasePacket;
import emu.grasscutter.net.packet.PacketOpcodes;
import emu.grasscutter.server.packet.send.PacketGadgetInteractRsp;

public class GadgetObject extends GadgetContent implements CreditsOwnInteraction {
    private int itemId;

    public GadgetObject(EntityGadget gadget) {
        super(gadget);
        GatherData gatherData = GameData.getGatherDataMap().get(gadget.getPointType());
        if (gatherData != null) {
            this.itemId = gatherData.getItemId();
        }
    }

    @Override
    public boolean onInteract(Player player, GadgetInteractReqOuterClass.GadgetInteractReq req) {
        // This is a workaround until a proper gadget interaction system can be put in place.
        ItemData itemData = GameData.getItemDataMap().get(this.itemId);
        if (itemData == null) {
            // Shrine of Depths gates consume a key that only the group script knows about.
            if (TempleShrineHelper.onInteract(getGadget(), player)) {
                // The interaction is handled, but deletion is owned by the group Lua script.
                player.onGadgetInteracted(getGadget().getGadgetId());
                return false;
            }
            interactByConfig(player, req);
            return false;
        }

        GameItem item = new GameItem(itemData, 1);
        player.getInventory().addItem(item, ActionReason.Gather);

        var ScriptArgs =
                new ScriptArgs(getGadget().getGroupId(), EventType.EVENT_GATHER, getGadget().getConfigId());
        if (getGadget().getMetaGadget() != null) {
            ScriptArgs.setEventSource(getGadget().getMetaGadget().config_id);
        }
        getGadget().getScene().getScriptManager().callEvent(ScriptArgs);

        getGadget()
                .getScene()
                .broadcastPacket(
                        new PacketGadgetInteractRsp(
                                getGadget(), InteractTypeOuterClass.InteractType.InteractType_INTERACT_GATHER));
        player.onGadgetInteracted(getGadget().getGadgetId());
        return true;
    }

    /**
     * 机关和门这类没有采集物的通用 Gadget：按场景里的 interact_id 查交互表，校验条件、扣道具，
     * 再把机关切到表里的状态。场景脚本靠这次状态变化（EVENT_GADGET_STATE_CHANGE）推进；
     * 不管成败都回 GadgetInteractRsp，客户端不会一直等。
     */
    private void interactByConfig(Player player, GadgetInteractReqOuterClass.GadgetInteractReq req) {
        var gadget = getGadget();
        var data = interactData();
        int state = data != null ? GadgetInteractRules.targetState(data) : -1;
        int retcode = 0;
        if (state >= 0 && state == gadget.getState()) {
            // 已经交互过（机关已在目标状态），不再重复扣道具
            retcode = RetcodeOuterClass.Retcode.RET_GADGET_INTERACT_COND_NOT_MEET_VALUE;
        } else if (data != null) {
            retcode = GadgetInteractRules.check(data, new PlayerContext(player));
            if (retcode == 0 && data.isConsumeItemNum() && !data.getCostItems().isEmpty()
                    && !player.getInventory().payItems(data.getCostItems(), 1, ActionReason.None)) {
                retcode = RetcodeOuterClass.Retcode.RET_ITEM_COUNT_NOT_ENOUGH_VALUE;
            }
        }

        var rsp = GadgetInteractRspOuterClass.GadgetInteractRsp.newBuilder()
                .setRetcode(retcode)
                .setGadgetEntityId(gadget.getId())
                .setGadgetId(gadget.getGadgetId())
                .setInteractType(InteractTypeOuterClass.InteractType.InteractType_INTERACT_NONE)
                .setOpType(req.getOpType());
        var packet = new BasePacket(PacketOpcodes.GadgetInteractRsp);
        packet.setData(rsp.build());
        player.sendPacket(packet);
        if (retcode != 0) return;

        player.onGadgetInteracted(gadget.getGadgetId());
        if (state >= 0) {
            gadget.updateState(state);
        }
    }

    private GadgetInteractData interactData() {
        var meta = getGadget().getMetaGadget();
        if (meta == null || meta.interact_id <= 0) return null;
        return GameData.getGadgetInteractDataMap().get(meta.interact_id);
    }

    private record PlayerContext(Player player) implements GadgetInteractRules.Context {
        @Override
        public int playerLevel() {
            return player.getLevel();
        }

        @Override
        public int offeringLevel(int offeringId) {
            return OfferingHelper.getLevel(player, offeringId);
        }

        @Override
        public long itemCount(int itemId) {
            return player.getInventory().getItemCountById(itemId);
        }
    }

    @Override
    public void onBuildProto(SceneGadgetInfoOuterClass.SceneGadgetInfo.Builder gadgetInfo) {
        // 客户端按 interact_id 显示交互界面（消耗、说明），官方下发场景机关时带着它
        var meta = getGadget().getMetaGadget();
        if (meta != null && meta.interact_id > 0) {
            gadgetInfo.setInteractId(meta.interact_id);
        }
    }
}
