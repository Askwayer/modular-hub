package com.vita.system.sysIpBlock.service.impl;

import com.vita.constant.AuthConstants;
import com.vita.core.RedisCache;
import com.vita.system.sysIpBlock.dto.SysIpBlockCreateDto;
import com.vita.system.sysIpBlock.dto.SysIpBlockUpdateDto;
import com.vita.system.sysIpBlock.entity.SysIpBlock;
import com.vita.system.sysIpBlock.mapper.SysIpBlockMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SysIpBlockServiceImplTest {

    @Mock
    private SysIpBlockMapper sysIpBlockMapper;
    @Mock
    private RedisCache redisCache;

    private SysIpBlockServiceImpl sysIpBlockService;

    @BeforeEach
    void setUp() {
        sysIpBlockService = new SysIpBlockServiceImpl();
        ReflectionTestUtils.setField(sysIpBlockService, "sysIpBlockMapper", sysIpBlockMapper);
        ReflectionTestUtils.setField(sysIpBlockService, "redisCache", redisCache);
    }

    @Test
    void createShouldWritePermanentRedisMarkerForActiveIp() {
        doAnswer(invocation -> {
            SysIpBlock data = invocation.getArgument(0);
            data.setId(1L);
            return 1;
        }).when(sysIpBlockMapper).insert(any(SysIpBlock.class));

        SysIpBlockCreateDto createDto = new SysIpBlockCreateDto();
        createDto.setIp("10.0.0.1");
        createDto.setStatus(Boolean.FALSE);

        Long id = sysIpBlockService.create(createDto);

        assertEquals(1L, id);
        verify(redisCache).setCacheObject(AuthConstants.buildIpBlockKey("10.0.0.1"), 1L);
    }

    @Test
    void updateShouldRefreshRedisMarkerWhenIpChanged() {
        SysIpBlock existed = new SysIpBlock();
        existed.setId(1L);
        existed.setIp("10.0.0.1");
        existed.setStatus(Boolean.FALSE);
        when(sysIpBlockMapper.selectById(1L)).thenReturn(existed);

        SysIpBlockUpdateDto updateDto = new SysIpBlockUpdateDto();
        updateDto.setId(1L);
        updateDto.setIp("10.0.0.2");
        updateDto.setStatus(Boolean.FALSE);

        sysIpBlockService.update(updateDto);

        verify(redisCache).deleteObject(AuthConstants.buildIpBlockKey("10.0.0.1"));
        verify(redisCache).setCacheObject(AuthConstants.buildIpBlockKey("10.0.0.2"), 1L);
    }

    @Test
    void deleteShouldRemoveRedisMarker() {
        SysIpBlock existed = new SysIpBlock();
        existed.setId(1L);
        existed.setIp("10.0.0.1");
        existed.setStatus(Boolean.FALSE);
        when(sysIpBlockMapper.selectById(1L)).thenReturn(existed);

        sysIpBlockService.delete(1L);

        verify(redisCache).deleteObject(AuthConstants.buildIpBlockKey("10.0.0.1"));
    }

    @Test
    void isBlockedShouldFallbackToDatabaseAndWarmCache() {
        when(redisCache.getCacheObject(AuthConstants.buildIpBlockKey("10.0.0.1"))).thenReturn(null);
        SysIpBlock existed = new SysIpBlock();
        existed.setId(2L);
        existed.setIp("10.0.0.1");
        existed.setStatus(Boolean.FALSE);
        when(sysIpBlockMapper.selectOne(any())).thenReturn(existed);

        boolean blocked = sysIpBlockService.isBlocked("10.0.0.1");

        assertTrue(blocked);
        verify(redisCache).setCacheObject(AuthConstants.buildIpBlockKey("10.0.0.1"), 2L);
    }

    @Test
    void loadBlockedIpCacheShouldResetAndReloadAllActiveIps() {
        when(redisCache.keys(AuthConstants.IP_BLOCK_KEY_PREFIX + "*")).thenReturn(Set.of("auth:ip:block:old"));
        SysIpBlock first = new SysIpBlock();
        first.setId(1L);
        first.setIp("10.0.0.1");
        first.setStatus(Boolean.FALSE);
        SysIpBlock second = new SysIpBlock();
        second.setId(2L);
        second.setIp("10.0.0.2");
        second.setStatus(Boolean.FALSE);
        when(sysIpBlockMapper.selectList(any())).thenReturn(List.of(first, second));

        sysIpBlockService.loadBlockedIpCache();

        verify(redisCache).deleteObject(Set.of("auth:ip:block:old"));
        verify(redisCache).setCacheObject(AuthConstants.buildIpBlockKey("10.0.0.1"), 1L);
        verify(redisCache).setCacheObject(AuthConstants.buildIpBlockKey("10.0.0.2"), 2L);
    }
}
