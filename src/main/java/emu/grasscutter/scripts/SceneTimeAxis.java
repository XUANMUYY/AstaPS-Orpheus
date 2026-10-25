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
    // PauseTimeAxis / ContinueTimeAxis：暂停期间的时长累计到 pausedMillis，到点的时间点顺延。
    private boolean paused;
    private long pauseStartedMillis;
    private volatile long pausedMillis;

    public synchronized boolean pause() {
        if (stopped) return false;
        if (!paused) {
            paused = true;
            pauseStartedMillis = System.currentTimeMillis();
        }
        return true;
    }

    public synchronized boolean resume() {
        if (stopped) return false;
        if (paused) {
            pausedMillis += System.currentTimeMillis() - pauseStartedMillis;
            paused = false;
            notifyAll();
        }
        return true;
    }

    synchronized boolean awaitUnpaused(long scheduledMillis, long offsetAtSchedule) {
        while (!stopped) {
            if (paused) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return false;
                }
                continue;
            }
            long remaining = scheduledMillis + pausedMillis - offsetAtSchedule - System.currentTimeMillis();
            if (remaining <= 0) return true;
            try {
                wait(remaining);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return false;
    }

    /** Schedules the task to run. */
    public synchronized void start() {
        if (stopped || delays.length == 0) return;
        // 推断：Lua time points are cumulative; java.util.Timer expects milliseconds.
        this.timer.schedule(new Task(0), Math.max(0, (long) delays[0]) * 1000L);
    }

    /** Terminates a repeating task. */
    public synchronized void stop() {
        this.stopped = true;
        notifyAll();
        this.timer.cancel();
    }

    final class Task extends TimerTask {
        private final int index;
        private final long offsetAtSchedule = pausedMillis;

        Task(int index) {
            this.index = index;
        }

        @Override
        public void run() {
            if (!awaitUnpaused(scheduledExecutionTime(), offsetAtSchedule)) return;
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
