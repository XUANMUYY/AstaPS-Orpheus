package emu.grasscutter.game.talk;

import emu.grasscutter.Grasscutter;
import emu.grasscutter.data.excels.TalkConfigData;
import emu.grasscutter.data.excels.TalkConfigData.TalkExecParam;
import emu.grasscutter.game.entity.GameEntity;
import emu.grasscutter.game.player.Player;
import emu.grasscutter.server.game.*;
import it.unimi.dsi.fastutil.ints.*;

public final class TalkSystem extends BaseGameSystem {
    private final Int2ObjectMap<TalkExecHandler> execHandlers = new Int2ObjectOpenHashMap<>();

    public TalkSystem(GameServer server) {
        super(server);

        this.registerHandlers(this.execHandlers, TalkExecHandler.class);
    }

    /**
     * Registers all handlers with the required conditions.
     *
     * @param map The map to save handlers to.
     * @param clazz The class which handlers should derive from.
     */
    public <T> void registerHandlers(Int2ObjectMap<T> map, Class<T> clazz) {
        var handlerClasses = Grasscutter.reflector.getSubTypesOf(clazz);
        for (var obj : handlerClasses) {
            this.registerTalkHandler(map, obj);
        }
    }

    /**
     * Registers the talk handler of the specified class.
     *
     * @param map The map to save the handler to.
     * @param handlerClass The class of the handler.
     */
    public <T> void registerTalkHandler(Int2ObjectMap<T> map, Class<? extends T> handlerClass) {
        try {
            var value = 0;
            if (handlerClass.isAnnotationPresent(TalkValueExec.class)) {
                TalkValueExec opcode = handlerClass.getAnnotation(TalkValueExec.class);
                value = opcode.value().getValue();
            } else {
                return;
            }

            if (value <= 0) return;
            map.put(value, handlerClass.getDeclaredConstructor().newInstance());
        } catch (Exception exception) {
            Grasscutter.getLogger().debug("Unable to register talk handler.", exception);
        }
    }

    /**
     * Notifies the associated handler of a talk being triggered.
     *
     * @param player The player which triggered the talk.
     * @param talkData The data associated with the talk.
     * @param execParam The talk parameter.
     */
    public boolean triggerExec(Player player, TalkConfigData talkData, TalkExecParam execParam) {
        return this.triggerExec(player, talkData, execParam, null);
    }

    /**
     * Notifies the associated handler of a talk being triggered.
     *
     * @param talkEntity 对话对象在服务端的实体，可能为 null。
     */
    public boolean triggerExec(
            Player player, TalkConfigData talkData, TalkExecParam execParam, GameEntity talkEntity) {
        var handler = this.execHandlers.get(execParam.getType().getValue());
        if (handler == null) {
            Grasscutter.getLogger()
                    .debug(
                            "Could not execute talk handlers for {} ({}).",
                            talkData.getId(),
                            execParam.getType().getValue());
            return true;
        }

        // Execute the handler.
        try {
            return handler.executeChecked(player, talkData, execParam, talkEntity);
        } catch (RuntimeException e) {
            // 执行失败交给 TalkManager 阻止对话完成事件。
            Grasscutter.getLogger()
                    .warn("Talk exec {} of talk {} failed.", execParam.getType(), talkData.getId(), e);
            return false;
        }
    }
}
