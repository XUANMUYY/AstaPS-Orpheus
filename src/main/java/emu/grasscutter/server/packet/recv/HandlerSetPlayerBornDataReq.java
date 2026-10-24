package emu.grasscutter.server.packet.recv;

import static emu.grasscutter.config.Configuration.*;

import emu.grasscutter.*;
import emu.grasscutter.data.GameData;
import emu.grasscutter.game.avatar.Avatar;
import emu.grasscutter.game.mail.Mail;
import emu.grasscutter.game.player.Player;
import emu.grasscutter.net.packet.*;
import emu.grasscutter.net.proto.SetPlayerBornDataReqOuterClass.SetPlayerBornDataReq;
import emu.grasscutter.server.born.BornIntroGate;
import emu.grasscutter.server.game.GameSession;
import emu.grasscutter.server.game.GameSession.SessionState;
import emu.grasscutter.server.packet.send.*;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

@Opcodes(PacketOpcodes.SetPlayerBornDataReq)
public class HandlerSetPlayerBornDataReq extends PacketHandler {

    /** Pure data packets naming the chosen Traveler; nothing here enters a scene. */
    static final List<Function<Player, BasePacket>> BORN_IDENTITY_PACKETS =
            List.of(PacketPlayerDataNotify::new, PacketAvatarDataNotify::new);

    @Override
    public void handle(GameSession session, byte[] header, byte[] payload) throws Exception {
        SetPlayerBornDataReq req = SetPlayerBornDataReq.parseFrom(payload);

        int avatarId = req.getAvatarId();
        int startingSkillDepot;
        if (avatarId == GameConstants.MAIN_CHARACTER_MALE) {
            startingSkillDepot = 504;
        } else if (avatarId == GameConstants.MAIN_CHARACTER_FEMALE) {
            startingSkillDepot = 704;
        } else {
            session.send(new PacketSetPlayerBornDataRsp(-1));
            return;
        }

        if (!GameData.getAvatarDataMap().containsKey(avatarId)) {
            Grasscutter.getLogger()
                    .error("No avatar data found! Please check your ExcelBinOutput folder.");
            session.send(new PacketSetPlayerBornDataRsp(-1));
            session.close();
            return;
        }

        Player player = session.getPlayer();
        player.setNickname(req.getNickName());

        if (player.getAvatars().getAvatarCount() == 0) {
            Avatar mainCharacter = new Avatar(avatarId);

            if (!GAME_OPTIONS.questing.enabled) {
                mainCharacter.setSkillDepotData(
                        GameData.getAvatarSkillDepotDataMap().get(startingSkillDepot));
            }

            player.addAvatar(mainCharacter, false);
            player.setMainCharacterId(avatarId);
            player.setHeadImage(avatarId);
            var team = player.getTeamManager().getCurrentSinglePlayerTeamInfo().getAvatars();
            team.clear();
            team.add(mainCharacter.getAvatarId());
            player.save();
        } else {
            return;
        }

        // 7.1 accepts the chosen Traveler first, then runs a second native intro client-side. Do
        // not create the World yet: sending scene-entry here cuts that intro off and eventually
        // makes the client reconnect.
        // The native intro's subtitles resolve the Traveler's twin from the local main character.
        // onLogin (and its AvatarDataNotify) is deferred until after that intro, so without these
        // the client assumes the male Traveler and a Lumine player reads "my sister".
        BORN_IDENTITY_PACKETS.forEach(packet -> session.send(packet.apply(player)));

        int configuredRsp = GAME_OPTIONS.newAccountIntro.setPlayerBornDataRsp;
        if (configuredRsp > 0 && configuredRsp != PacketSetPlayerBornDataRsp.CMD_ID) {
            session.send(new BasePacket(configuredRsp));
        } else {
            session.send(new PacketSetPlayerBornDataRsp());
        }
        session.send(new PacketPlayerNicknameNotify(req.getNickName()));

        // Normal packets (including the pause-cycle signal below) arrive after 26105, so leave the
        // character-picking router state before returning from this handler.
        session.setState(SessionState.ACTIVE);
        BornIntroGate.arm(session);

        Grasscutter.getLogger()
                .info(
                        "[intro] character creation finished: {} picked avatar {}; waiting for native intro handoff.",
                        req.getNickName(),
                        avatarId);

        if (!GAME_INFO.joinOptions.sendWelcomeMail) {
            return;
        }
        var welcomeMail = GAME_INFO.joinOptions.welcomeMail;
        Mail mail = new Mail();
        mail.mailContent.title = welcomeMail.title;
        mail.mailContent.sender = welcomeMail.sender;
        mail.mailContent.content =
                welcomeMail.content
                        + "\n<type=\"browser\" text=\"GitHub\" href=\"https://github.com/Grasscutters/Grasscutter\"/>";
        mail.itemList.addAll(Arrays.asList(welcomeMail.items));
        mail.importance = 1;
        player.sendMail(mail);
    }
}
