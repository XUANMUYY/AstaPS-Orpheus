package emu.grasscutter.scripts;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

final class SceneTimeAxisPauseTest {
    @Test
    void resumeKeepsTheRemainingDelay() throws Exception {
        var axis = new SceneTimeAxis(null, 1, "oven", new int[] {1}, false);
        try {
            assertTrue(axis.pause());
            long deadline = System.currentTimeMillis() + 180;
            var result = CompletableFuture.supplyAsync(() -> axis.awaitUnpaused(deadline, 0));
            Thread.sleep(220);
            assertFalse(result.isDone());
            assertTrue(axis.resume());
            Thread.sleep(60);
            assertFalse(result.isDone());
            assertTrue(result.get(1, TimeUnit.SECONDS));
        } finally {
            axis.stop();
        }
    }

    @Test
    void stopWakesAPausedTaskWithoutFiringIt() throws Exception {
        var axis = new SceneTimeAxis(null, 1, "oven", new int[] {1}, false);
        assertTrue(axis.pause());
        var result = CompletableFuture.supplyAsync(
                () -> axis.awaitUnpaused(System.currentTimeMillis(), 0));
        Thread.sleep(40);
        assertFalse(result.isDone());
        axis.stop();
        assertFalse(result.get(1, TimeUnit.SECONDS));
        assertFalse(axis.resume());
    }
}
