package emu.grasscutter.game.talk.exec;

import emu.grasscutter.data.excels.TalkConfigData;
import emu.grasscutter.data.excels.TalkConfigData.TalkExecParam;
import emu.grasscutter.game.player.Player;
import emu.grasscutter.game.talk.*;

@TalkValueExec(TalkExec.TALK_EXEC_CHANGE_WEAPON_MODE)
public final class ExecChangeWeaponMode extends TalkExecHandler {
    /** 参数 [武器号, 形态]：供 QUEST_COND_WEAPON_IS_ARKHE_OUSIA_OR_PNEUMA 判定。 */
    @Override
    public void execute(Player player, TalkConfigData talkData, TalkExecParam execParam) {
        int weaponId = execParam.intParam(0);
        if (weaponId <= 0) return;
        player.getPlayerProgress().setWeaponMode(weaponId, execParam.intParam(1));
        player.save();
    }
}
