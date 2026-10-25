package emu.grasscutter.game.quest;

import emu.grasscutter.Grasscutter;
import emu.grasscutter.game.ability.NatsaurusVehicleHelper;
import emu.grasscutter.game.entity.EntityVehicle;
import emu.grasscutter.game.player.Player;
import emu.grasscutter.game.world.Position;
import emu.grasscutter.game.world.Scene;
import emu.grasscutter.net.proto.VehicleInteractTypeOuterClass.VehicleInteractType;
import emu.grasscutter.net.proto.VehicleMemberOuterClass.VehicleMember;
import emu.grasscutter.net.proto.VisionTypeOuterClass.VisionType;
import emu.grasscutter.server.packet.send.PacketVehicleInteractRsp;
import java.util.ArrayList;

public final class QuestVehicleSupport {
    private QuestVehicleSupport() {}

    public record VehicleLocation(int sceneId, Position position, Position rotation) {}

    public static VehicleLocation parseLocation(String value) {
        if (value == null) return null;
        String text = value.trim();
        if (text.startsWith("[") && text.endsWith("]")) text = text.substring(1, text.length() - 1).trim();
        else if (text.startsWith("[") || text.endsWith("]")) return null;

        String[] parts = text.split(":", -1);
        if (parts.length != 2) return null;
        String[] ids = parts[0].split("_", -1);
        String[] coordinates = parts[1].split(",", -1);
        if (ids.length != 2 || coordinates.length != 6) return null;
        try {
            int sceneId = Integer.parseInt(ids[0].trim());
            int configId = Integer.parseInt(ids[1].trim());
            if (sceneId <= 0 || configId <= 0) return null;
            float[] values = new float[6];
            for (int i = 0; i < values.length; i++) {
                values[i] = Float.parseFloat(coordinates[i].trim());
                if (!Float.isFinite(values[i])) return null;
            }
            return new VehicleLocation(sceneId,
                    new Position(values[0], values[1], values[2]),
                    new Position(values[3], values[4], values[5]));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static void enter(Player player, int gadgetId, int sceneId, Position pos, Position rot) {
        Scene scene = player.getScene();
        if (scene == null || scene.getId() != sceneId) {
            Grasscutter.getLogger().debug("Quest vehicle {} skipped: player {} is not in scene {}",
                    gadgetId, player.getUid(), sceneId);
            return;
        }

        exit(player, 0);
        removeOwned(scene, player, gadgetId);
        EntityVehicle vehicle = new EntityVehicle(scene, player, gadgetId, 0, pos, rot);
        scene.addEntity(vehicle);
        player.getStaminaManager().handleVehicleInteractReq(player.getSession(), vehicle.getId(),
                VehicleInteractType.VehicleInteractType_VEHICLE_INTERACT_IN);
        player.sendPacket(new PacketVehicleInteractRsp(player, vehicle.getId(),
                VehicleInteractType.VehicleInteractType_VEHICLE_INTERACT_IN));
    }

    public static void exit(Player player, int gadgetId) {
        Scene scene = player.getScene();
        if (scene == null) return;
        for (var entity : new ArrayList<>(scene.getEntities().values())) {
            if (!(entity instanceof EntityVehicle vehicle)
                    || (gadgetId != 0 && vehicle.getGadgetId() != gadgetId)) continue;
            for (VehicleMember member : new ArrayList<>(vehicle.getVehicleMembers())) {
                if (member.getUid() == player.getUid())
                    NatsaurusVehicleHelper.dismount(player, vehicle, member);
            }
        }
    }

    public static void clear(Player player, int sceneId, int gadgetId) {
        Scene scene = player.getScene();
        if (scene == null || scene.getId() != sceneId) return;
        removeOwned(scene, player, gadgetId);
    }

    private static void removeOwned(Scene scene, Player owner, int gadgetId) {
        for (var entity : new ArrayList<>(scene.getEntities().values())) {
            if (!(entity instanceof EntityVehicle vehicle) || !owner.equals(vehicle.getOwner())
                    || vehicle.getGadgetId() != gadgetId) continue;
            for (VehicleMember member : new ArrayList<>(vehicle.getVehicleMembers())) {
                Player rider = scene.getPlayers().stream()
                        .filter(candidate -> candidate.getUid() == member.getUid())
                        .findFirst().orElse(null);
                if (rider == null && owner.getUid() == member.getUid()) rider = owner;
                if (rider != null) NatsaurusVehicleHelper.dismount(rider, vehicle, member);
                else vehicle.getVehicleMembers().remove(member);
            }
            scene.removeEntity(vehicle, VisionType.VisionType_VISION_REMOVE);
        }
    }
}
