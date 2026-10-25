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
    /** Delay from ScriptLib.InitTimeAxis — official lua units are seconds. */
    private final int delay;
    private final boolean loop;
    private boolean paused;
    private long pauseStartedMillis;
    private volatile long pausedMillis;
    private boolean cancelled;

    public synchronized boolean pause() {
        if (cancelled) return false;
        if (!paused) {
            paused = true;
            pauseStartedMillis = System.currentTimeMillis();
        }
        return true;
    }

    public synchronized boolean resume() {
        if (cancelled) return false;
        if (paused) {
            pausedMillis += System.currentTimeMillis() - pauseStartedMillis;
            paused = false;
            notifyAll();
        }
        return true;
    }

    synchronized boolean awaitUnpaused(long scheduledMillis, long offsetAtSchedule) {
        while (!cancelled) {
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
    public void start() {
        // java.util.Timer expects milliseconds; lua InitTimeAxis passes seconds.
        long delayMs = Math.max(0, (long) this.delay) * 1000L;
        if (this.loop) {
            this.timer.scheduleAtFixedRate(new Task(), delayMs, delayMs);
        } else {
            this.timer.schedule(new Task(), delayMs);
        }
    }

    /** Terminates a repeating task. */
    public void stop() {
        synchronized (this) {
            cancelled = true;
            notifyAll();
        }
        this.timer.cancel();
    }

    final class Task extends TimerTask {
        private final long offsetAtSchedule = pausedMillis;

        @Override
        public void run() {
            if (!awaitUnpaused(scheduledExecutionTime(), offsetAtSchedule)) return;
            if (cancelled) return;
            // Invoke script event.
            SceneTimeAxis.this.handle.callEvent(
                    new ScriptArgs(SceneTimeAxis.this.groupId, EventType.EVENT_TIME_AXIS_PASS)
                            .setEventSource(SceneTimeAxis.this.identifier));
        }
    }
}
