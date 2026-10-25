package emu.grasscutter.scripts;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonParser;
import emu.grasscutter.scripts.lib.PointArrayAvatarMovement;
import org.junit.jupiter.api.Test;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;

final class PointArrayAvatarMovementTest {
    private static final String ARRAYS = """
            [{"pointArrayId":42,"platformPointList":[
              {"pointId":1,"position":{"x":1,"y":2,"z":3}},
              {"pointId":2,"position":{"x":4,"y":5,"z":6}}]}]
            """;

    @Test
    void resolvesLastRoutePointForForwardAndReverseTravel() {
        var arrays = JsonParser.parseString(ARRAYS).getAsJsonArray();
        var forward = new LuaTable();
        forward.set(1, LuaValue.valueOf(1));
        forward.set(2, LuaValue.valueOf(2));
        var reverse = new LuaTable();
        reverse.set(1, LuaValue.valueOf(2));
        reverse.set(2, LuaValue.valueOf(1));
        assertEquals(4f, PointArrayAvatarMovement.destination(arrays, 42, forward).getX());
        assertEquals(1f, PointArrayAvatarMovement.destination(arrays, 42, reverse).getX());
    }

    @Test
    void unknownArrayOrPointDoesNotProduceAMovement() {
        var arrays = JsonParser.parseString(ARRAYS).getAsJsonArray();
        var route = new LuaTable();
        route.set(1, LuaValue.valueOf(99));
        assertNull(PointArrayAvatarMovement.destination(arrays, 42, route));
        route.set(1, LuaValue.valueOf(2));
        assertNull(PointArrayAvatarMovement.destination(arrays, 7, route));
    }
}
