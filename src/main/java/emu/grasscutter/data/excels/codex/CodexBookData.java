package emu.grasscutter.data.excels.codex;

import com.google.gson.annotations.SerializedName;
import emu.grasscutter.data.*;

/** 书籍图鉴：拿到对应的书（任务道具）就解锁。 */
@ResourceType(name = {"BooksCodexExcelConfigData.json"})
public class CodexBookData extends GameResource {
    @SerializedName(value = "id", alternate = "Id")
    private int Id;
    private int materialId;
    private int sortOrder;

    @Override
    public int getId() {
        return Id;
    }

    public int getMaterialId() {
        return materialId;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    @Override
    public void onLoad() {
        GameData.getCodexBookDataIdMap().put(this.getMaterialId(), this);
    }
}
