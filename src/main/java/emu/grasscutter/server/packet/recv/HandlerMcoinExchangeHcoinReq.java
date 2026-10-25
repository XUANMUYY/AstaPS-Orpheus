package emu.grasscutter.server.packet.recv;

import emu.grasscutter.net.packet.*;
import emu.grasscutter.net.proto.*;
import emu.grasscutter.server.game.GameSession;
import emu.grasscutter.server.packet.send.PacketMcoinExchangeHcoinRsp;

@Opcodes(PacketOpcodes.McoinExchangeHcoinReq)
public class HandlerMcoinExchangeHcoinReq extends PacketHandler {

    @Override
    public void handle(GameSession session, byte[] header, byte[] payload) throws Exception {
        McoinExchangeHcoinReqOuterClass.McoinExchangeHcoinReq exchangeReq =
                McoinExchangeHcoinReqOuterClass.McoinExchangeHcoinReq.parseFrom(payload);

        var player = session.getPlayer();
        int cost = exchangeReq.getMcoinCost();
        int retcode = check(player.getCrystals(), player.getPrimogems(), cost);
        if (retcode != RetcodeOuterClass.Retcode.RET_SUCC_VALUE) {
            session.send(new PacketMcoinExchangeHcoinRsp(retcode));
            return;
        }

        // 创世结晶与原石 1:1 兑换，得到的原石数以扣掉的结晶数为准
        player.setCrystals(player.getCrystals() - cost);
        player.setPrimogems(player.getPrimogems() + cost);
        player.save();
        session.send(new PacketMcoinExchangeHcoinRsp(RetcodeOuterClass.Retcode.RET_SUCC_VALUE));
    }

    /** 兑换前校验：数量必须为正、结晶要够、原石不能溢出；不再因为请求里两个数不等就跳过检查。 */
    static int check(int crystals, int primogems, int cost) {
        if (cost <= 0) return RetcodeOuterClass.Retcode.RET_SVR_ERROR_VALUE;
        if (crystals < cost) return RetcodeOuterClass.Retcode.RET_MCOIN_NOT_ENOUGH_VALUE;
        if ((long) primogems + cost > Integer.MAX_VALUE) {
            return RetcodeOuterClass.Retcode.RET_HCOIN_EXCEED_LIMIT_VALUE;
        }
        return RetcodeOuterClass.Retcode.RET_SUCC_VALUE;
    }
}
