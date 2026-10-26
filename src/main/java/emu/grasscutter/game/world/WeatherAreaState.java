package emu.grasscutter.game.world;

import emu.grasscutter.game.props.ClimateType;

public final class WeatherAreaState {
    private WeatherAreaState() {}

    public static int nextArea(int current, int area, int state) {
        return state != 0 ? area : (current == area ? 1 : current);
    }

    public static void apply(Scene scene, int weatherAreaId, int state) {
        if (scene == null) return;
        if (state != 0) {
            scene.getPlayers().forEach(p -> p.setWeather(weatherAreaId, ClimateType.CLIMATE_NONE));
        } else {
            scene.getPlayers().forEach(p -> {
                if (nextArea(p.getWeatherId(), weatherAreaId, state) != p.getWeatherId()) {
                    p.setWeather(1, ClimateType.CLIMATE_SUNNY);
                }
            });
        }
    }
}
