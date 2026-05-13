package com.vita.controller.publicapi;

import com.vita.hot.service.HotRefreshExecutionMode;
import com.vita.hot.service.HotRefreshExecutionResult;
import com.vita.hot.service.IHotSourceService;
import com.vita.hot.vo.HotSourceSnapshotVo;
import com.vita.hot.vo.HotSourceStatusVo;
import com.vita.hot.vo.HotSourceSummaryVo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class HotRefreshPublicControllerTest {

    private MockMvc mockMvc;

    private StubHotSourceService hotSourceService;

    @BeforeEach
    void setUp() {
        hotSourceService = new StubHotSourceService();
        mockMvc = MockMvcBuilders.standaloneSetup(new HotRefreshPublicController(hotSourceService)).build();
    }

    @Test
    void refreshAllShouldReturnRemoteExecutionMode() throws Exception {
        hotSourceService.result = HotRefreshExecutionResult.remote(List.of(buildStatus("weibo")));

        mockMvc.perform(post("/public/hot/refreshAll"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.content.executionMode").value(HotRefreshExecutionMode.REMOTE.name()))
                .andExpect(jsonPath("$.content.statusList.length()").value(1))
                .andExpect(jsonPath("$.content.statusList[0].sourceCode").value("weibo"));
    }

    @Test
    void refreshAllShouldReturnCacheFallbackExecutionMode() throws Exception {
        hotSourceService.result = HotRefreshExecutionResult.cacheFallback(List.of(buildStatus("zhihu")));

        mockMvc.perform(post("/public/hot/refreshAll"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.content.executionMode").value(HotRefreshExecutionMode.CACHE_FALLBACK.name()))
                .andExpect(jsonPath("$.content.statusList.length()").value(1))
                .andExpect(jsonPath("$.content.statusList[0].sourceCode").value("zhihu"));
    }

    @Test
    void refreshAllShouldReturnSkippedExecutionMode() throws Exception {
        hotSourceService.result = HotRefreshExecutionResult.skipped();

        mockMvc.perform(post("/public/hot/refreshAll"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.content.executionMode").value(HotRefreshExecutionMode.SKIPPED.name()))
                .andExpect(jsonPath("$.content.statusList.length()").value(0));
    }

    private HotSourceStatusVo buildStatus(String sourceCode) {
        HotSourceStatusVo statusVo = new HotSourceStatusVo();
        statusVo.setSourceCode(sourceCode);
        statusVo.setSourceName(sourceCode);
        return statusVo;
    }

    private static final class StubHotSourceService implements IHotSourceService {

        private HotRefreshExecutionResult result = HotRefreshExecutionResult.skipped();

        @Override
        public List<HotSourceSummaryVo> listCachedSources() {
            throw new UnsupportedOperationException();
        }

        @Override
        public HotSourceSnapshotVo getCachedSource(String sourceCode) {
            throw new UnsupportedOperationException();
        }

        @Override
        public HotSourceSnapshotVo refreshSource(String sourceCode) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<HotSourceStatusVo> refreshAllEnabledSources() {
            throw new UnsupportedOperationException();
        }

        @Override
        public HotRefreshExecutionResult refreshAllEnabledSourcesWithFallback() {
            return result;
        }

        @Override
        public List<HotSourceStatusVo> listCacheStatus() {
            throw new UnsupportedOperationException();
        }
    }
}
