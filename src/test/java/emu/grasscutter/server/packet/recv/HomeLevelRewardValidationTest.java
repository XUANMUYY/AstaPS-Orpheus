package emu.grasscutter.server.packet.recv;

import static org.junit.jupiter.api.Assertions.assertEquals;

import emu.grasscutter.net.proto.RetcodeOuterClass.Retcode;
import org.junit.jupiter.api.Test;

final class HomeLevelRewardValidationTest {
    @Test
    void onlyExistingReachedAndUnclaimedLevelCanPayOut() {
        assertEquals(Retcode.RET_HOME_INVALID_CLIENT_PARAM_VALUE,
                HandlerGetHomeLevelUpRewardReq.rewardRetcode(20, 2, false, false));
        assertEquals(Retcode.RET_GENERAL_REWARD_NO_QUALIFICATION_VALUE,
                HandlerGetHomeLevelUpRewardReq.rewardRetcode(3, 2, true, false));
        assertEquals(Retcode.RET_REWARD_HAS_TAKEN_VALUE,
                HandlerGetHomeLevelUpRewardReq.rewardRetcode(2, 2, true, true));
        assertEquals(Retcode.RET_SUCC_VALUE,
                HandlerGetHomeLevelUpRewardReq.rewardRetcode(2, 2, true, false));
    }
}
