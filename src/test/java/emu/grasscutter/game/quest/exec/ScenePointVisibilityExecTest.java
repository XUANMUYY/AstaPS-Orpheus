package emu.grasscutter.game.quest.exec;

import static org.junit.jupiter.api.Assertions.*;

import emu.grasscutter.game.player.Player;
import emu.grasscutter.game.quest.GameMainQuest;
import emu.grasscutter.game.quest.GameQuest;
import emu.grasscutter.game.quest.enums.QuestExec;
import emu.grasscutter.net.packet.BasePacket;
import emu.grasscutter.net.proto.GetScenePointRspOuterClass.GetScenePointRsp;
import emu.grasscutter.net.proto.ScenePointUnlockNotifyOuterClass.ScenePointUnlockNotify;
import emu.grasscutter.server.packet.send.PacketGetScenePointRsp;
import emu.grasscutter.server.packet.send.PacketScenePointUnlockNotify;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

final class ScenePointVisibilityExecTest {
    private static final class RecordingPlayer extends Player {
        int saves;
        boolean online;
        final List<BasePacket> packets = new ArrayList<>();

        @Override
        public void save() {
            saves++;
        }

        @Override
        public boolean isOnline() {
            return online;
        }

        @Override
        public void sendPacket(BasePacket packet) {
            packets.add(packet);
        }
    }

    private static GameQuest questFor(Player player) {
        var main = new GameMainQuest();
        main.setOwner(player);
        var quest = new GameQuest();
        quest.setMainQuest(main);
        return quest;
    }

    @Test
    void handlersPersistMutuallyExclusiveSceneStatesAndNotifyWhenOnline() throws Exception {
        var player = new RecordingPlayer();
        var quest = questFor(player);
        var hide = new ExecHideScenePoint();
        var unhide = new ExecUnhideScenePoint();

        assertEquals(QuestExec.QUEST_EXEC_HIDE_SCENE_POINT,
                QuestExec.getContentTriggerByName("QUEST_EXEC_HIDE_SCENE_POINT"));
        assertEquals(QuestExec.QUEST_EXEC_UNHIDE_SCENE_POINT,
                QuestExec.getContentTriggerByName("QUEST_EXEC_UNHIDE_SCENE_POINT"));

        assertTrue(hide.execute(quest, null, " 10 ", "4, 9,20"));
        assertEquals(Set.of(4, 9, 20), player.getHiddenScenePoints(10));
        assertTrue(player.getUnhiddenScenePoints(10).isEmpty());
        assertEquals(1, player.saves);
        assertTrue(player.packets.isEmpty());

        player.online = true;
        assertTrue(unhide.execute(quest, null, "10", " 9, 20 "));
        assertEquals(Set.of(4), player.getHiddenScenePoints(10));
        assertEquals(Set.of(9, 20), player.getUnhiddenScenePoints(10));
        assertEquals(2, player.saves);
        var notification = ScenePointUnlockNotify.parseFrom(player.packets.get(0).getData());
        assertEquals(10, notification.getSceneId());
        assertEquals(List.of(9, 20), notification.getHBKHEKLMEKNList());
        assertTrue(notification.getHidePointListList().isEmpty());
        assertTrue(notification.getUnhidePointListList().isEmpty());
        assertTrue(notification.getPointListList().isEmpty());

        assertTrue(hide.execute(quest, null, "10", "20"));
        assertEquals(Set.of(4, 20), player.getHiddenScenePoints(10));
        assertEquals(Set.of(9), player.getUnhiddenScenePoints(10));
        assertEquals(3, player.saves);
        var hiddenNotification = ScenePointUnlockNotify.parseFrom(player.packets.get(1).getData());
        assertEquals(List.of(20), hiddenNotification.getHidePointListList());
        assertTrue(hiddenNotification.getHBKHEKLMEKNList().isEmpty());
        assertTrue(hiddenNotification.getUnhidePointListList().isEmpty());
        assertTrue(hiddenNotification.getPointListList().isEmpty());
    }

    @Test
    void malformedParametersNeverChangeState() {
        var player = new RecordingPlayer();
        var quest = questFor(player);
        var hide = new ExecHideScenePoint();
        var unhide = new ExecUnhideScenePoint();
        for (String[] params : List.of(
                new String[] {}, new String[] {"3"}, new String[] {"", "1"},
                new String[] {"0", "1"}, new String[] {"x", "1"},
                new String[] {"3", ""}, new String[] {"3", "1,,2"},
                new String[] {"3", "1, "}, new String[] {"3", "-2"},
                new String[] {"3", "1,x"})) {
            assertFalse(hide.execute(quest, null, params));
            assertFalse(unhide.execute(quest, null, params));
        }
        assertEquals(0, player.saves);
        assertTrue(player.getHiddenScenePoints(3).isEmpty());
        assertTrue(player.getUnhiddenScenePoints(3).isEmpty());
    }

    @Test
    void snapshotUsesVisibilityFieldsWithoutConflatingLockedPoints() throws Exception {
        var player = new RecordingPlayer();
        player.getHiddenScenePoints(3).add(1218);
        player.getUnhiddenScenePoints(3).add(831);
        player.getForceLockedScenePoints(3).add(7);

        var response = GetScenePointRsp.parseFrom(new PacketGetScenePointRsp(player, 3).getData());
        assertEquals(Set.of(1218), Set.copyOf(response.getHidePointListList()));
        assertEquals(Set.of(831), Set.copyOf(response.getHBKHEKLMEKNList()));
        assertFalse(response.getHidePointListList().contains(7));
        assertTrue(response.getUnhidePointListList().isEmpty());

        var otherScene = GetScenePointRsp.parseFrom(new PacketGetScenePointRsp(player, 4).getData());
        assertTrue(otherScene.getHidePointListList().isEmpty());
        assertTrue(otherScene.getHBKHEKLMEKNList().isEmpty());
    }

    @Test
    void legacyNullFieldsAndWireNumbers() throws Exception {
        var player = new RecordingPlayer();
        for (String fieldName : List.of("hiddenScenePoints", "unhiddenScenePoints")) {
            Field field = Player.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(player, null);
        }
        assertTrue(player.getHiddenScenePoints(3).isEmpty());
        assertTrue(player.getUnhiddenScenePoints(3).isEmpty());

        assertEquals(8, ScenePointUnlockNotify.HBKHEKLMEKN_FIELD_NUMBER);
        assertEquals(6, ScenePointUnlockNotify.HIDE_POINT_LIST_FIELD_NUMBER);
        assertEquals(4, ScenePointUnlockNotify.UNHIDE_POINT_LIST_FIELD_NUMBER);
        assertEquals(14, GetScenePointRsp.HBKHEKLMEKN_FIELD_NUMBER);
        assertEquals(4, GetScenePointRsp.HIDE_POINT_LIST_FIELD_NUMBER);
        assertEquals(3, GetScenePointRsp.UNHIDE_POINT_LIST_FIELD_NUMBER);

        var unhide = ScenePointUnlockNotify.parseFrom(
                PacketScenePointUnlockNotify.unhide(3, List.of(831)).getData());
        var hide = ScenePointUnlockNotify.parseFrom(
                PacketScenePointUnlockNotify.hide(3, List.of(1218)).getData());
        assertEquals(List.of(831), unhide.getHBKHEKLMEKNList());
        assertEquals(List.of(1218), hide.getHidePointListList());
    }
}
