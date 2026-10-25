package emu.grasscutter.game.quest.content;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.Gson;
import emu.grasscutter.data.GameData;
import emu.grasscutter.data.excels.FeatureTagGroupData;
import emu.grasscutter.data.excels.avatar.VehicleData;
import emu.grasscutter.data.excels.quest.QuestData;
import emu.grasscutter.game.quest.QuestVehicleFeatureTags;
import emu.grasscutter.net.proto.PlayerCookReqOuterClass.PlayerCookReq;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

final class PlayerActionQuestContentTest {
    private static QuestData.QuestContentCondition condition(int[] args, String foods) {
        var condition = new QuestData.QuestContentCondition();
        condition.setParam(args);
        condition.setParamStr(foods);
        return condition;
    }

    @Test
    void featureTagConditionsRequireExactTag() {
        var wanted = condition(new int[] {30011, 0}, "");
        assertTrue(new ContentEnterFeatureTagVehicle().execute(null, wanted, "", 30011));
        assertTrue(new ContentLeaveFeatureTagVehicle().execute(null, wanted, "", 30011));
        assertFalse(new ContentEnterFeatureTagVehicle().execute(null, wanted, "", 30012));
        assertFalse(new ContentLeaveFeatureTagVehicle().execute(null, wanted, ""));
        assertFalse(new ContentEnterFeatureTagVehicle().execute(null, condition(new int[0], ""), "", 30011));
    }

    @Test
    void vehicleTagsComeFromVehicleGroupAndOmitZeroAndDuplicates() throws Exception {
        var gson = new Gson();
        var vehicles = gson.fromJson(Files.readString(Path.of("resources/ExcelBinOutput/VehicleExcelConfigData.json")), VehicleData[].class);
        var groups = gson.fromJson(Files.readString(Path.of("resources/ExcelBinOutput/FeatureTagGroupExcelConfigData.json")), FeatureTagGroupData[].class);
        var vehicle = Arrays.stream(vehicles).filter(v -> v.getId() == 45003010).findFirst().orElseThrow();
        var group = Arrays.stream(groups).filter(g -> g.getId() == vehicle.getFeatureTagGroupID()).findFirst().orElseThrow();
        GameData.getVehicleDataMap().put(vehicle.getId(), vehicle);
        GameData.getFeatureTagGroupDataMap().put(group.getId(), group);
        try {
            assertArrayEquals(new int[] {30010, 30011, 30102, 30103},
                    Arrays.stream(QuestVehicleFeatureTags.tagsForGadget(vehicle.getId())).sorted().toArray());
            var repeated = gson.fromJson("{\"groupID\":30000011,\"tagIDs\":[30011,30011,0,30012]}",
                    FeatureTagGroupData.class);
            GameData.getFeatureTagGroupDataMap().put(group.getId(), repeated);
            assertArrayEquals(new int[] {30011, 30012}, QuestVehicleFeatureTags.tagsForGadget(vehicle.getId()));
            assertArrayEquals(new int[0], QuestVehicleFeatureTags.tagsForGadget(999));
        } finally {
            GameData.getVehicleDataMap().remove(vehicle.getId());
            GameData.getFeatureTagGroupDataMap().remove(group.getId());
        }
    }

    @Test
    void cookingMatchesGroupConfigAndOptionalFoodList() {
        var req = PlayerCookReq.newBuilder().setEBPHDCJFJEF(133601030).setMJAKDGHIIFB(30001).setRecipeId(1001).build();
        var content = new ContentFinishAnyFoodCook();
        var chosen = condition(new int[] {133601030, 30001}, "108880,108881");
        assertTrue(content.execute(null, chosen, "", req.getEBPHDCJFJEF(), req.getMJAKDGHIIFB(), 108881));
        assertFalse(content.execute(null, chosen, "", req.getEBPHDCJFJEF(), req.getMJAKDGHIIFB(), 108874));
        assertFalse(content.execute(null, chosen, "", 133601031, 30001, 108881));
        assertFalse(content.execute(null, chosen, "", 133601030, 30002, 108881));
        assertTrue(content.execute(null, condition(new int[] {133601030, 30001}, ""), "", 133601030, 30001, 108880));
        assertFalse(content.execute(null, chosen, "", 133601030, 30001));
        assertFalse(content.execute(null, condition(new int[] {133601030, 30001}, "108881,bad"),
                "", 133601030, 30001, 108881));
    }

    @Test
    void trainRenameAndWidgetMatchOnlyRequestedIds() {
        var train = new ContentCallPlayerTrainInRange();
        var trainCondition = condition(new int[] {3, 35}, "");
        assertTrue(train.execute(null, trainCondition, "", 3, 35));
        assertFalse(train.execute(null, trainCondition, "", 3, 36));
        assertFalse(train.execute(null, trainCondition, "", 2001, 35));
        assertFalse(train.execute(null, trainCondition, "", 3));

        var rename = new ContentAvatarRenameComplete();
        var renameCondition = condition(new int[] {1, 0}, "");
        assertTrue(rename.execute(null, renameCondition, "", 1));
        assertFalse(rename.execute(null, renameCondition, "", 2));
        assertFalse(rename.execute(null, renameCondition, ""));

        var widget = new ContentSpecificWidgetAttached();
        var widgetCondition = condition(new int[] {220133, 0}, "");
        assertTrue(widget.execute(null, widgetCondition, "", 220133));
        assertFalse(widget.execute(null, widgetCondition, "", 220134));
        assertFalse(widget.execute(null, widgetCondition, ""));
    }
}
