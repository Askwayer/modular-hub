package com.vita.log.service;

import com.vita.log.model.OperLogRecord;
import com.vita.manager.AsyncManager;
import com.vita.manager.task.SystemAsyncTask;
import com.vita.manager.task.SystemAsyncTaskExecutorType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Iterator;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class OperLogAsyncExecutorTest {

    @Test
    void submitShouldCreateSnapshotAndDelegateTaskToAsyncManager() {
        CapturingPersistence persistence = new CapturingPersistence();
        RecordingAsyncManager asyncManager = new RecordingAsyncManager();
        OperLogAsyncExecutor executor = new OperLogAsyncExecutor(asyncManager, new StaticObjectProvider<>(persistence));
        OperLogRecord record = buildRecord();

        executor.submit("req-success", record);
        record.setModule("changed-after-submit");

        assertThat(asyncManager.executeCount).isEqualTo(1);
        assertThat(asyncManager.lastTask).isNotNull();
        assertThat(asyncManager.lastTask.getTaskName()).isEqualTo("system-oper-log");
        assertThat(asyncManager.lastTask.getExecutorType()).isEqualTo(SystemAsyncTaskExecutorType.OPER_LOG);

        asyncManager.lastTask.getTask().run();

        assertThat(persistence.lastRecord).isNotNull();
        assertThat(persistence.lastRecord).isNotSameAs(record);
        assertThat(persistence.lastRecord.getModule()).isEqualTo("系统用户");
    }

    @Test
    void submitShouldIgnoreNullRecord() {
        RecordingAsyncManager asyncManager = new RecordingAsyncManager();
        OperLogAsyncExecutor executor = new OperLogAsyncExecutor(asyncManager, new StaticObjectProvider<>(null));

        executor.submit("req-null", null);

        assertThat(asyncManager.executeCount).isZero();
    }

    private OperLogRecord buildRecord() {
        OperLogRecord record = new OperLogRecord();
        record.setModule("系统用户");
        record.setBusinessType("CREATE");
        record.setRequestMethod("POST");
        record.setOperUrl("http://localhost:8080/system/sysUser/add");
        record.setOperIp("127.0.0.1");
        record.setOperName("tester");
        record.setOperUserId(1L);
        record.setClassName("com.vita.controller.system.SysUserController");
        record.setMethodName("create");
        record.setRequestParam("{\"username\":\"tester\"}");
        record.setStatus((byte) 1);
        record.setCostTime(10L);
        record.setOperTime(LocalDateTime.now());
        return record;
    }

    private static final class CapturingPersistence implements OperLogPersistence {

        private OperLogRecord lastRecord;

        @Override
        public void persist(OperLogRecord record) {
            this.lastRecord = record;
        }
    }

    private static final class StaticObjectProvider<T> implements ObjectProvider<T> {

        private final T value;

        private StaticObjectProvider(T value) {
            this.value = value;
        }

        @Override
        public T getObject(Object... args) {
            return value;
        }

        @Override
        public T getIfAvailable() {
            return value;
        }

        @Override
        public T getIfUnique() {
            return value;
        }

        @Override
        public T getObject() {
            return value;
        }

        @Override
        public Iterator<T> iterator() {
            return Collections.singletonList(value).iterator();
        }

        @Override
        public Stream<T> stream() {
            return Stream.of(value);
        }

        @Override
        public Stream<T> orderedStream() {
            return Stream.of(value);
        }
    }

    private static final class RecordingAsyncManager extends AsyncManager {

        private SystemAsyncTask lastTask;

        private int executeCount;

        private RecordingAsyncManager() {
            super(null, null);
        }

        @Override
        public void execute(SystemAsyncTask task) {
            this.lastTask = task;
            this.executeCount++;
        }
    }
}
