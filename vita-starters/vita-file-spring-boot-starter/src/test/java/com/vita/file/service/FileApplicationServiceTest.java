package com.vita.file.service;

import com.vita.core.RedisCache;
import com.vita.core.exception.ServiceException;
import com.vita.file.dto.FileUploadRequestDto;
import com.vita.file.property.FileProperty;
import com.vita.file.property.LocalStorageProperty;
import com.vita.file.property.MinioStorageProperty;
import com.vita.file.storage.FileStorage;
import com.vita.file.support.FileObjectKeyGenerator;
import com.vita.file.support.FileUploadCommand;
import com.vita.file.vo.FileUploadVo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class FileApplicationServiceTest {

    private final FileStorage fileStorage = mock(FileStorage.class);
    private final RedisCache redisCache = mock(RedisCache.class);
    private final FileObjectKeyGenerator keyGenerator = mock(FileObjectKeyGenerator.class);

    private FileApplicationService fileApplicationService;

    @BeforeEach
    void setUp() {
        FileProperty fileProperty = new FileProperty();
        fileProperty.setPublicPrefix("public");
        fileProperty.setPrivatePrefix("private");
        fileProperty.setMaxFileSize(1024L * 1024);

        LocalStorageProperty local = new LocalStorageProperty();
        local.setBasePath("/tmp/vita-test");
        local.setPublicDomain("files.example.com/public");
        local.setPrivateDomain("files.example.com/private");
        fileProperty.setLocal(local);

        MinioStorageProperty minio = new MinioStorageProperty();
        minio.setEndpoint("http://127.0.0.1:9000");
        minio.setAccessKey("test");
        minio.setSecretKey("test");
        minio.setBucket("vita-file");
        minio.setPublicDomain("oss.example.com/public");
        minio.setPrivateDomain("oss.example.com/private");
        fileProperty.setMinio(minio);

        Map<String, Object> cache = new ConcurrentHashMap<>();
        doAnswer(invocation -> {
            cache.put(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(redisCache).setCacheObject(anyString(), any(), anyLong(), any(TimeUnit.class));
        when(redisCache.getCacheObject(anyString())).thenAnswer(invocation -> cache.get(invocation.getArgument(0)));
        doAnswer(invocation -> {
            cache.remove(invocation.getArgument(0));
            return true;
        }).when(redisCache).deleteObject(anyString());

        when(fileStorage.getBucketName(true)).thenReturn("files");
        when(fileStorage.getBucketName(false)).thenReturn("files");
        when(fileStorage.getHostname(true)).thenReturn("https://files.example.com/public");
        when(fileStorage.getHostname(false)).thenReturn("https://files.example.com/private");
        when(fileStorage.buildDirectUrl(any())).thenAnswer(invocation ->
                "https://files.example.com/direct/" + invocation.<com.vita.file.support.FileAccessCommand>getArgument(0).getObjectKey());
        when(fileStorage.generateAccessUrl(any())).thenAnswer(invocation -> com.vita.file.support.FileAccessResult.builder()
                .accessUrl("https://files.example.com/signed/" + invocation.<com.vita.file.support.FileAccessCommand>getArgument(0).getObjectKey())
                .signed(true)
                .build());

        fileApplicationService = new FileApplicationService(fileProperty, fileStorage, redisCache, keyGenerator);
    }

    @Test
    void shouldReuseObjectKeyAcrossUploadSteps() {
        FileUploadRequestDto dto = new FileUploadRequestDto();
        dto.setIsPublic(1);
        dto.setIsPart(0);
        dto.setSuffix("png");
        dto.setFileSize(128L);
        dto.setPrefixDir("avatar");
        dto.setOriginName("test.png");

        when(keyGenerator.generate(any(), anyBoolean(), anyString(), anyString()))
                .thenReturn("public/avatar/2026/05/25/fixed.png");

        String uploadId = fileApplicationService.getUploadId(dto);

        MultipartFile file = new MockMultipartFile("file", "test.png", "image/png", new byte[128]);
        FileUploadVo result = fileApplicationService.uploadSingleFile(file, uploadId);

        ArgumentCaptor<FileUploadCommand> commandCaptor = ArgumentCaptor.forClass(FileUploadCommand.class);
        verify(fileStorage).upload(any(MultipartFile.class), commandCaptor.capture());
        assertEquals("public/avatar/2026/05/25/fixed.png", commandCaptor.getValue().getObjectKey());
        assertEquals("public/avatar/2026/05/25/fixed.png", result.getUri());
        assertTrue(result.getFileUrl().contains("public/avatar/2026/05/25/fixed.png"));
        verify(keyGenerator, times(1)).generate(any(), anyBoolean(), anyString(), anyString());
    }

    @Test
    void shouldRejectUnsupportedSuffix() {
        FileUploadRequestDto dto = new FileUploadRequestDto();
        dto.setIsPublic(1);
        dto.setIsPart(0);
        dto.setSuffix("exe");
        dto.setFileSize(128L);

        ServiceException exception = assertThrows(ServiceException.class, () -> fileApplicationService.getUploadId(dto));
        assertEquals(2202, exception.getCode());
    }

    @Test
    void shouldValidateMultipartTotalSizeBeforeComplete() {
        FileUploadRequestDto dto = new FileUploadRequestDto();
        dto.setIsPublic(2);
        dto.setIsPart(1);
        dto.setSuffix("png");
        dto.setFileSize(256L);

        when(keyGenerator.generate(any(), anyBoolean(), anyString(), anyString()))
                .thenReturn("private/2026/05/25/fixed.png");
        when(fileStorage.initMultipartUpload(any())).thenReturn("upload-1");
        when(fileStorage.getUploadedPartTotalSize(any())).thenReturn(200L);

        String uploadId = fileApplicationService.getUploadId(dto);

        ServiceException exception = assertThrows(ServiceException.class,
                () -> fileApplicationService.completeUpload(uploadId));

        assertEquals(400, exception.getCode());
        verify(fileStorage, never()).completeMultipartUpload(any());
    }
}
