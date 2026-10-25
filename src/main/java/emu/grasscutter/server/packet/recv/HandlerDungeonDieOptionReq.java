package emu.grasscutter.server.packet.recv;

import emu.grasscutter.net.packet.*;
import emu.grasscutter.net.proto.DungeonDieOptionReqOuterClass.DungeonDieOptionReq;
import emu.grasscutter.net.proto.PlayerDieOptionOuterClass.PlayerDieOption;
import emu.grasscutter.server.game.GameSession;

@Opcodes(PacketOpcodes.DungeonDieOptionReq)
public class HandlerDungeonDieOptionReq extends PacketHandler {
    enum Action { EXIT, RESTART, REVIVE, IGNORE }

    static Action resolveAction(PlayerDieOption option, boolean quitImmediately) {
        if (quitImmediately) return Action.EXIT;
        // 推断：CANCEL 退出当前副本；7.1 枚举只给出选项名称，没有附带行为说明。
        return switch (option) {
            case PlayerDieOption_DIE_OPT_CANCEL -> Action.EXIT;
            case PlayerDieOption_DIE_OPT_REPLAY -> Action.RESTART;
            case PlayerDieOption_DIE_OPT_REVIVE -> Action.REVIVE;
            default -> Action.IGNORE;
        };
    }

    @Override
    public void handle(GameSession session, byte[] header, byte[] payload) throws Exception {
        DungeonDieOptionReq req = DungeonDieOptionReq.parseFrom(payload);
        var player = session.getPlayer();
        var scene = player.getScene();
        var manager = scene != null ? scene.getDungeonManager() : null;
        if (manager != null) {
            switch (resolveAction(req.getDieOption(), req.getIsQuitImmediately())) {
                case EXIT -> player.getServer().getDungeonSystem().exitDungeon(player);
                case RESTART -> {
                    player.getServer().getDungeonSystem().restartDungeon(player);
                    var restartedScene = player.getScene();
                    if (restartedScene != null && restartedScene.getDungeonManager() != null
                            && restartedScene.getDungeonManager() != manager
                            && !player.getTeamManager().getActiveTeam().isEmpty()) {
                        // 推断：死亡后重开至少要恢复一名可操作角色。
                        player.getTeamManager().checkCurrentAvatarIsAlive(null);
                    }
                }
                case REVIVE -> manager.tryRevivePlayer(player);
                case IGNORE -> { }
            }
        }
        player.sendPacket(new BasePacket(PacketOpcodes.DungeonDieOptionRsp));
    }
}
