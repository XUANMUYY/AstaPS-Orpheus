package emu.grasscutter.scripts;

import emu.grasscutter.scripts.constants.EventType;
import emu.grasscutter.scripts.data.ScriptArgs;
import java.util.*;
import lombok.*;

@Getter
@RequiredArgsConstructor
public final class SceneTimeAxis {
    private final Timer timer = new Timer();

    private final SceneScriptManager handle;
    private final int groupId;

    private final String identifier;
    /** Time points from ScriptLib.InitTimeAxis — official lua units are seconds. */
    private final int[] delays;
    private final boolean loop;
    private boolean stopped;

    /** Schedules the task to run. */
    public synchronized void start() {
        if (stopped || delays.length == 0) return;
        // 推断：Lua time points are cumulative; java.util.Timer expects milliseconds.
        this.timer.schedule(new Task(0), Math.max(0, (long) delays[0]) * 1000L);
    }

    /** Terminates a repeating task. */
    public synchronized void stop() {
        this.stopped = true;
        this.timer.cancel();
    }

    final class Task extends TimerTask {
        private final int index;

        Task(int index) {
            this.index = index;
        }

        @Override
        public void run() {
            synchronized (SceneTimeAxis.this) {
                if (stopped) return;
            }
            SceneTimeAxis.this.handle.callEvent(
                    new ScriptArgs(SceneTimeAxis.this.groupId, EventType.EVENT_TIME_AXIS_PASS)
                            .setParam1(index + 1)
                            .setEventSource(SceneTimeAxis.this.identifier));
            int next = index + 1;
            if (next == delays.length) {
                if (!loop) return;
                next = 0;
            }
            long seconds = next == 0 ? delays[0] : (long) delays[next] - delays[index];
            synchronized (SceneTimeAxis.this) {
                if (!stopped) timer.schedule(new Task(next), Math.max(next == 0 ? 1 : 0, seconds) * 1000L);
            }
        }
    }
}
