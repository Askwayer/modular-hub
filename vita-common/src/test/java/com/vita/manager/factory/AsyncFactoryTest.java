package com.vita.manager.factory;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.vita.interceptor.RequestTraceInterceptor;
import com.vita.log.model.OperLogRecord;
import com.vita.log.service.OperLogPersistence;
import com.vita.manager.task.SystemAsyncTask;
import com.vita.manager.task.SystemAsyncTaskExecutorType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class AsyncFactoryTest {

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    void recordOperShouldCreateOperLogTaskDefinition() {
        SystemAsyncTask task = AsyncFactory.recordOper("req-success", buildRecord(), new CapturingPersistence());

        assertThat(task.getTaskName()).isEqualTo("system-oper-log");
        assertThat(task.getExecutorType()).isEqualTo(SystemAsyncTaskExecutorType.OPER_LOG);
        assertThat(task.hasDelay()).isFalse();
    }

    @Test
    void recordOperShouldWriteAccessLogPersistRecordAndClearMdc() {
        CapturingPersistence persistence = new CapturingPersistence();
        Logger accessLogger = (Logger) LoggerFactory.getLogger("sys-access");
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        accessLogger.addAppender(appender);

        SystemAsyncTask task = AsyncFactory.recordOper("req-success", buildRecord(), persistence);
        task.getTask().run();

        assertThat(persistence.lastRecord).isNotNull();
        assertThat(persistence.lastRecord.getModule()).isEqualTo("系统用户");
        assertThat(appender.list).hasSize(1);
        assertThat(appender.list.get(0).getFormattedMessage()).contains("requestId=req-success");
        assertThat(MDC.get(RequestTraceInterceptor.REQUEST_ID_KEY)).isNull();

        accessLogger.detachAppender(appender);
        appender.stop();
    }

    @Test
    void recordOperShouldSwallowTaskExceptionAndLogError() {
        Logger logger = (Logger) LoggerFactory.getLogger(AsyncFactory.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        SystemAsyncTask task = AsyncFactory.recordOper("req-error", buildRecord(), new FailingPersistence());
        task.getTask().run();

        assertThat(appender.list).hasSize(1);
        assertThat(appender.list.get(0).getFormattedMessage()).contains("异步保存系统操作日志失败");
        assertThat(MDC.get(RequestTraceInterceptor.REQUEST_ID_KEY)).isNull();

        logger.detachAppender(appender);
        appender.stop();
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

    private static final class FailingPersistence implements OperLogPersistence {

        @Override
        public void persist(OperLogRecord record) {
            throw new IllegalStateException("boom");
        }
    }
}
