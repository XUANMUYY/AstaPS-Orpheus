package emu.grasscutter.data.excels.codex;

import com.google.gson.annotations.SerializedName;
import emu.grasscutter.data.GameData;
import emu.grasscutter.data.GameResource;
import emu.grasscutter.data.ResourceType;
import lombok.Getter;

@ResourceType(name = "BooksCodexExcelConfigData.json")
@Getter
public class CodexBookData extends GameResource {
    @SerializedName(value = "id", alternate = "Id")
    private int id;
    private int materialId;

    @Override
    public void onLoad() {
        GameData.getCodexBookDataMap().put(id, this);
    }
}
