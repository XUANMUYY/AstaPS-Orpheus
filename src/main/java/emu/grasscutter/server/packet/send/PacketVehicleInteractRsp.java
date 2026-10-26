package emu.grasscutter.server.packet.send;


import emu.grasscutter.data.GameData;
import emu.grasscutter.game.ability.NatsaurusVehicleHelper;
import emu.grasscutter.game.entity.*;
import emu.grasscutter.game.player.Player;
import emu.grasscutter.game.props.FightProperty;
import emu.grasscutter.game.quest.enums.QuestContent;
import emu.grasscutter.game.quest.QuestVehicleFeatureTags;
import emu.grasscutter.net.packet.*;
import emu.grasscutter.net.proto.VehicleInteractRspOuterClass.VehicleInteractRsp;
import emu.grasscutter.net.proto.VehicleInteractTypeOuterClass.VehicleInteractType;
import emu.grasscutter.net.proto.VehicleMemberOuterClass.VehicleMember;
import it.unimi.dsi.fastutil.ints.Int2FloatMap;
import java.util.Locale;
public class PacketVehicleInteractRsp extends BasePacket {

    public PacketVehicleInteractRsp(Player player, int entityId, VehicleInteractType interactType) {
        super(PacketOpcodes.VehicleInteractRsp);
        VehicleInteractRsp.Builder proto = VehicleInteractRsp.newBuilder();

        GameEntity vehicle = player.getScene().getEntityById(entityId);
        if (vehicle == null) {
            this.setData(VehicleInteractRsp.newBuilder().build());
            return;
        }
        // Only a fallback. A vehicle whose gadget config carries combat properties already has its HP
        // out of fillFightProps, and the Natlan Saurian mounts are flagged isInvincible there, so they
        // come out with CUR_HP at infinity. Overwriting both with a flat 1000 is what made a ridden
        // Saurian die to a single hit.
        if (vehicle instanceof EntityVehicle mount && !NatsaurusVehicleHelper.isNatsaurusMount(mount)) {
            initializeMissingDurability(vehicle.getFightProperties());
        }
        if (vehicle instanceof EntityVehicle) {
            proto.setEntityId(vehicle.getId());

            VehicleMember vehicleMember =
                    VehicleMember.newBuilder()
                            .setUid(player.getUid())
                            .setAvatarGuid(player.getTeamManager().getCurrentCharacterGuid())
                            .build();

            proto.setInteractType(interactType);
            proto.setMember(vehicleMember);

            switch (interactType) {
                case VehicleInteractType_VEHICLE_INTERACT_IN -> {
                    ((EntityVehicle) vehicle).getVehicleMembers().add(vehicleMember);
                    player
                            .getQuestManager()
                            .queueEvent(
                                    QuestContent.QUEST_CONTENT_ENTER_VEHICLE,
                                    ((EntityVehicle) vehicle).getGadgetId());
                    QuestVehicleFeatureTags.queue(player, ((EntityVehicle) vehicle).getGadgetId(),
                            QuestContent.QUEST_CONTENT_ENTER_FEATURE_TAG_VEHICLE);

                    // For phlogiston. The gadget row may be absent, and a quarter of the rows
                    // that exist carry no jsonName - either way this falls back to type 1 rather
                    // than throwing into the packet send.
                    var gadgetData = GameData.getGadgetDataMap().get(((EntityVehicle) vehicle).getGadgetId());
                    var jsonName =
                            gadgetData == null || gadgetData.getJsonName() == null
                                    ? ""
                                    : gadgetData.getJsonName().toLowerCase(Locale.ROOT);
                    var vehicleType = jsonName.contains("skiff") ? 2 : jsonName.contains("sorush") ? 3 :
                            jsonName.contains("natsaurus") ? 4 : 1;
                    proto.setGadgetId(((EntityVehicle) vehicle).getGadgetId());
                    proto.setVehicleType(vehicleType);
                    proto.setVehiclePos(player.getPosition().toProto());
                    proto.setVehicleRot(player.getRotation().toProto());
                }
                case VehicleInteractType_VEHICLE_INTERACT_OUT -> {
                    EntityVehicle mount = (EntityVehicle) vehicle;
                    if (mount.getVehicleMembers().removeIf(member -> member.getUid() == player.getUid())) {
                        player.getQuestManager().queueEvent(
                                QuestContent.QUEST_CONTENT_LEAVE_VEHICLE, mount.getGadgetId());
                        QuestVehicleFeatureTags.queue(player, mount.getGadgetId(),
                                QuestContent.QUEST_CONTENT_LEAVE_FEATURE_TAG_VEHICLE);
                    }
                }
                default -> {}
            }
        }
        this.setData(proto.build());
    }

    static void initializeMissingDurability(Int2FloatMap properties) {
        int maxId = FightProperty.FIGHT_PROP_MAX_HP.getId();
        int currentId = FightProperty.FIGHT_PROP_CUR_HP.getId();
        properties.putIfAbsent(maxId, 1000f);
        properties.putIfAbsent(currentId, properties.get(maxId));
    }

    public PacketVehicleInteractRsp(
            EntityVehicle vehicle, VehicleMember vehicleMember, VehicleInteractType interactType) {
        super(PacketOpcodes.VehicleInteractRsp);
        VehicleInteractRsp.Builder proto = VehicleInteractRsp.newBuilder();

        if (vehicle != null) {
            proto.setEntityId(vehicle.getId());
            proto.setInteractType(interactType);
            proto.setMember(vehicleMember);

            switch (interactType) {
                case VehicleInteractType_VEHICLE_INTERACT_IN -> {
                    vehicle.getVehicleMembers().add(vehicleMember);
                }
                case VehicleInteractType_VEHICLE_INTERACT_OUT -> {
                    vehicle.getVehicleMembers().remove(vehicleMember);
                }
                default -> {}
            }
        }
        this.setData(proto.build());
    }
}
