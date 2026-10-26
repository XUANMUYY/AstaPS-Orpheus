package emu.grasscutter.server.game;

/** A single music-game attempt belongs to one game connection, and can settle only once. */
public class MusicGamePlayTracker {
    public enum SettleResult {
        NOT_STARTED,
        ID_MISMATCH,
        OK
    }

    private int musicBasicId;
    private long ugcGuid;
    private long startedAtMillis;
    private boolean started;

    public synchronized void start(int musicBasicId, long ugcGuid) {
        this.musicBasicId = musicBasicId;
        this.ugcGuid = ugcGuid;
        this.startedAtMillis = System.currentTimeMillis();
        this.started = true;
    }

    public synchronized SettleResult settle(int musicBasicId, long ugcGuid) {
        if (!started) {
            return SettleResult.NOT_STARTED;
        }
        if (this.musicBasicId != musicBasicId || this.ugcGuid != ugcGuid) {
            return SettleResult.ID_MISMATCH;
        }

        // MusicGameBasicConfigData has no track duration; do not guess a minimum play time.
        started = false;
        return SettleResult.OK;
    }

    long getStartedAtMillis() {
        return startedAtMillis;
    }
}
