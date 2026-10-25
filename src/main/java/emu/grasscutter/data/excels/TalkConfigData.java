package emu.grasscutter.data.excels;

import com.google.gson.annotations.SerializedName;
import emu.grasscutter.data.*;
import emu.grasscutter.game.quest.enums.LogicType;
import emu.grasscutter.game.talk.TalkExec;
import java.util.List;
import lombok.*;

@ResourceType(name = "TalkExcelConfigData.json")
@EqualsAndHashCode(callSuper = false)
@Data
public final class TalkConfigData extends GameResource {
    @SerializedName(
            value = "id",
            alternate = {"_id"})
    private int id;

    /** 开始条件；类型保留原文字符串，服务端不认识的类型在判定时按「无法判断」处理。 */
    @SerializedName(
            value = "beginCond",
            alternate = {"_beginCond"})
    private List<TalkCondParam> beginCond;

    @SerializedName(
            value = "beginCondComb",
            alternate = {"_beginCondComb"})
    private LogicType beginCondComb;

    @SerializedName(
            value = "finishExec",
            alternate = {"_finishExec"})
    private List<TalkExecParam> finishExec;

    @SerializedName(
            value = "questId",
            alternate = {"_questId"})
    private int questId;

    @SerializedName(
            value = "npcId",
            alternate = {"_npcId"})
    private List<Integer> npcId;

    @Override
    public void onLoad() {
        this.finishExec =
                this.finishExec == null
                        ? List.of()
                        : this.finishExec.stream().filter(x -> x.getType() != null).toList();
        this.beginCond =
                this.beginCond == null
                        ? List.of()
                        : this.beginCond.stream()
                                .filter(x -> x.getType() != null && !x.getType().isEmpty())
                                .toList();

        if (this.questId <= 0) {
            var id = String.valueOf(this.getId());
            this.questId = Integer.parseInt(id.length() < 5 ? "0" : id.substring(0, id.length() - 2));
        }
    }

    @Data
    public static class TalkExecParam {
        @SerializedName(
                value = "type",
                alternate = {"_type"})
        private TalkExec type;

        @SerializedName(
                value = "param",
                alternate = {"_param"})
        private String[] param;

        /** 第 index 个参数按整数读，空串或缺失读成 0。 */
        public int intParam(int index) {
            return parseIntParam(this.param, index);
        }
    }

    static int parseIntParam(String[] param, int index) {
        if (param == null || index >= param.length) return 0;
        var value = param[index];
        if (value == null || value.isBlank()) return 0;
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    @Data
    public static class TalkCondParam {
        @SerializedName(
                value = "type",
                alternate = {"_type"})
        private String type;

        @SerializedName(
                value = "param",
                alternate = {"_param"})
        private String[] param;

        /** 第 index 个参数按整数读，空串或缺失读成 0。 */
        public int intParam(int index) {
            return parseIntParam(this.param, index);
        }
    }
}
