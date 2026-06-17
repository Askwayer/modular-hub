package com.vita.manager;

import com.vita.manager.task.SystemAsyncTask;
import com.vita.manager.task.SystemAsyncTaskExecutorType;
import org.junit.jupiter.api.Test;

import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;

class AsyncManagerTest {

    @Test
    void executeShouldRouteByExecutorType() {
        AtomicReference<String> routedExecutor = new AtomicReference<>();
        Executor operExecutor = command -> routedExecutor.set("oper");
        Executor loginExecutor = command -> routedExecutor.set("login");
        Executor geoExecutor = command -> routedExecutor.set("geo");
        ScheduledExecutorService scheduledExecutorService = mock(ScheduledExecutorService.class);
        AsyncManager asyncManager = new AsyncManager(scheduledExecutorService, operExecutor, loginExecutor, geoExecutor);

        asyncManager.execute(SystemAsyncTask.immediate("oper", SystemAsyncTaskExecutorType.OPER_AUDIT, () -> {
        }));
        assertEquals("oper", routedExecutor.get());

        asyncManager.execute(SystemAsyncTask.immediate("login", SystemAsyncTaskExecutorType.LOGIN_AUDIT, () -> {
        }));
        assertEquals("login", routedExecutor.get());

        asyncManager.execute(SystemAsyncTask.immediate("geo", SystemAsyncTaskExecutorType.GEO_ENRICH, () -> {
        }));
        assertEquals("geo", routedExecutor.get());
    }

    @Test
    void executeShouldDropRejectedTaskInsteadOfRunningCallerThread() {
        AtomicBoolean executed = new AtomicBoolean(false);
        Executor rejectedExecutor = command -> {
            throw new RejectedExecutionException("full");
        };
        ScheduledExecutorService scheduledExecutorService = mock(ScheduledExecutorService.class);
        AsyncManager asyncManager = new AsyncManager(scheduledExecutorService, rejectedExecutor, rejectedExecutor, rejectedExecutor);

        asyncManager.execute(SystemAsyncTask.immediate("drop", SystemAsyncTaskExecutorType.OPER_AUDIT, () -> executed.set(true)));

        assertFalse(executed.get());
    }
}
