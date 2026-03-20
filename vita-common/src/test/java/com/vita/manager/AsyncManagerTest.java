package com.vita.manager;

import com.vita.manager.task.SystemAsyncTask;
import com.vita.manager.task.SystemAsyncTaskExecutorType;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.Delayed;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class AsyncManagerTest {

    @Test
    void executeShouldRouteImmediateOperLogTaskToDedicatedExecutor() {
        RecordingScheduledExecutorService scheduledExecutorService = new RecordingScheduledExecutorService();
        RecordingExecutor operLogExecutor = new RecordingExecutor();
        AsyncManager asyncManager = new AsyncManager(scheduledExecutorService, operLogExecutor);
        AtomicInteger counter = new AtomicInteger();

        asyncManager.execute(SystemAsyncTask.immediate("oper-log", SystemAsyncTaskExecutorType.OPER_LOG, counter::incrementAndGet));

        assertThat(counter.get()).isEqualTo(1);
        assertThat(operLogExecutor.executeCount).isEqualTo(1);
        assertThat(scheduledExecutorService.executeCount).isZero();
        assertThat(scheduledExecutorService.scheduleCount).isZero();
    }

    @Test
    void executeShouldScheduleDelayedTaskBeforeDispatchingToDedicatedExecutor() {
        RecordingScheduledExecutorService scheduledExecutorService = new RecordingScheduledExecutorService();
        RecordingExecutor operLogExecutor = new RecordingExecutor();
        AsyncManager asyncManager = new AsyncManager(scheduledExecutorService, operLogExecutor);
        AtomicInteger counter = new AtomicInteger();

        asyncManager.execute(SystemAsyncTask.delayed("oper-log", SystemAsyncTaskExecutorType.OPER_LOG, counter::incrementAndGet, 25L));

        assertThat(counter.get()).isEqualTo(1);
        assertThat(scheduledExecutorService.scheduleCount).isEqualTo(1);
        assertThat(scheduledExecutorService.lastDelayMillis).isEqualTo(25L);
        assertThat(operLogExecutor.executeCount).isEqualTo(1);
    }

    @Test
    void executeShouldRouteImmediateScheduledTaskToScheduledExecutor() {
        RecordingScheduledExecutorService scheduledExecutorService = new RecordingScheduledExecutorService();
        RecordingExecutor operLogExecutor = new RecordingExecutor();
        AsyncManager asyncManager = new AsyncManager(scheduledExecutorService, operLogExecutor);
        AtomicInteger counter = new AtomicInteger();

        asyncManager.execute(SystemAsyncTask.immediate("scheduled-task", SystemAsyncTaskExecutorType.SCHEDULED, counter::incrementAndGet));

        assertThat(counter.get()).isEqualTo(1);
        assertThat(scheduledExecutorService.executeCount).isEqualTo(1);
        assertThat(operLogExecutor.executeCount).isZero();
    }

    @Test
    void executeShouldFallbackToCallerThreadWhenExecutorRejects() {
        RecordingScheduledExecutorService scheduledExecutorService = new RecordingScheduledExecutorService();
        RecordingExecutor operLogExecutor = new RecordingExecutor();
        operLogExecutor.reject = true;
        AsyncManager asyncManager = new AsyncManager(scheduledExecutorService, operLogExecutor);
        AtomicInteger counter = new AtomicInteger();

        asyncManager.execute(SystemAsyncTask.immediate("oper-log", SystemAsyncTaskExecutorType.OPER_LOG, counter::incrementAndGet));

        assertThat(counter.get()).isEqualTo(1);
        assertThat(operLogExecutor.executeCount).isZero();
    }

    @Test
    void shutdownShouldCloseScheduledExecutorService() {
        RecordingScheduledExecutorService scheduledExecutorService = new RecordingScheduledExecutorService();
        AsyncManager asyncManager = new AsyncManager(scheduledExecutorService, new RecordingExecutor());

        asyncManager.shutdown();

        assertThat(scheduledExecutorService.shutdownCalled).isTrue();
    }

    private static final class RecordingExecutor implements Executor {

        private int executeCount;

        private boolean reject;

        @Override
        public void execute(Runnable command) {
            if (reject) {
                throw new RejectedExecutionException("queue full");
            }
            executeCount++;
            command.run();
        }
    }

    private static final class RecordingScheduledExecutorService extends AbstractExecutorService implements ScheduledExecutorService {

        private int executeCount;

        private int scheduleCount;

        private long lastDelayMillis = -1L;

        private boolean shutdownCalled;

        private boolean rejectExecute;

        private boolean rejectSchedule;

        @Override
        public void shutdown() {
            this.shutdownCalled = true;
        }

        @Override
        public List<Runnable> shutdownNow() {
            this.shutdownCalled = true;
            return Collections.emptyList();
        }

        @Override
        public boolean isShutdown() {
            return shutdownCalled;
        }

        @Override
        public boolean isTerminated() {
            return shutdownCalled;
        }

        @Override
        public boolean awaitTermination(long timeout, TimeUnit unit) {
            return true;
        }

        @Override
        public void execute(Runnable command) {
            if (rejectExecute) {
                throw new RejectedExecutionException("scheduled execute rejected");
            }
            executeCount++;
            command.run();
        }

        @Override
        public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
            if (rejectSchedule) {
                throw new RejectedExecutionException("scheduled submit rejected");
            }
            scheduleCount++;
            lastDelayMillis = unit.toMillis(delay);
            command.run();
            return new CompletedScheduledFuture<>(null, lastDelayMillis);
        }

        @Override
        public <V> ScheduledFuture<V> schedule(java.util.concurrent.Callable<V> callable, long delay, TimeUnit unit) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
            throw new UnsupportedOperationException();
        }
    }

    private static final class CompletedScheduledFuture<V> implements ScheduledFuture<V> {

        private final V value;

        private final long delayMillis;

        private CompletedScheduledFuture(V value, long delayMillis) {
            this.value = value;
            this.delayMillis = delayMillis;
        }

        @Override
        public long getDelay(TimeUnit unit) {
            return unit.convert(delayMillis, TimeUnit.MILLISECONDS);
        }

        @Override
        public int compareTo(Delayed other) {
            return Long.compare(getDelay(TimeUnit.MILLISECONDS), other.getDelay(TimeUnit.MILLISECONDS));
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            return false;
        }

        @Override
        public boolean isCancelled() {
            return false;
        }

        @Override
        public boolean isDone() {
            return true;
        }

        @Override
        public V get() {
            return value;
        }

        @Override
        public V get(long timeout, TimeUnit unit) {
            return value;
        }
    }
}
