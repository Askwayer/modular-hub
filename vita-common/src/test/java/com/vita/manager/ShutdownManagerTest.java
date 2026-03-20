package com.vita.manager;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ShutdownManagerTest {

    @Test
    void destroyShouldShutdownAsyncManager() {
        RecordingAsyncManager asyncManager = new RecordingAsyncManager();
        ShutdownManager shutdownManager = new ShutdownManager(asyncManager);

        shutdownManager.destroy();

        assertThat(asyncManager.shutdownCount).isEqualTo(1);
    }

    private static final class RecordingAsyncManager extends AsyncManager {

        private int shutdownCount;

        private RecordingAsyncManager() {
            super(null, null);
        }

        @Override
        public void shutdown() {
            shutdownCount++;
        }
    }
}
