package com.vita.aichat.chat.support;

import com.vita.aichat.chat.model.result.AiChatStreamBatchSnapshot;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AiStreamBatchAccumulatorTest {

    @Test
    void shouldFlushWhenCharThresholdReached() {
        AiStreamBatchAccumulator accumulator = new AiStreamBatchAccumulator(1000L, 5, 0L);

        accumulator.append("abc", null);
        assertFalse(accumulator.shouldFlush(100L));

        accumulator.append("de", null);
        assertTrue(accumulator.shouldFlush(100L));

        AiChatStreamBatchSnapshot snapshot = accumulator.drain(100L);
        assertEquals("abcde", snapshot.getContentDelta());
        assertEquals("", snapshot.getReasoningDelta());
        assertEquals(5, snapshot.getDeltaChars());
        assertFalse(accumulator.hasPending());
    }

    @Test
    void shouldFlushWhenTimeWindowReached() {
        AiStreamBatchAccumulator accumulator = new AiStreamBatchAccumulator(1000L, 20, 0L);

        accumulator.append("abc", "xyz");
        assertFalse(accumulator.shouldFlush(999L));
        assertTrue(accumulator.shouldFlush(1000L));

        AiChatStreamBatchSnapshot snapshot = accumulator.drain(1000L);
        assertEquals("abc", snapshot.getContentDelta());
        assertEquals("xyz", snapshot.getReasoningDelta());
        assertEquals(6, snapshot.getDeltaChars());
    }
}
