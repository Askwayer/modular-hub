package com.vita.file.service;

import cn.hutool.core.text.CharSequenceUtil;
import com.alibaba.fastjson2.JSONObject;
import com.vita.core.RedisCache;
import com.vita.core.exception.GlobalErrorCode;
import com.vita.core.exception.ServiceException;
import com.vita.file.dto.FileUploadRequestDto;
import com.vita.file.enums.FileVisibilityEnum;
import com.vita.file.property.FileProperty;
import com.vita.file.storage.FileStorage;
import com.vita.file.support.FileAccessCommand;
import com.vita.file.support.FileAccessResult;
import com.vita.file.support.FileObjectKeyGenerator;
import com.vita.file.support.FileUploadCommand;
import com.vita.file.vo.FileAccessUrlVo;
import com.vita.file.vo.FileUploadVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 文件应用服务：uploadId 管理、后缀校验、分片编排、URL 组装。
 */
@Slf4j
@Service
public class FileApplicationService {

    private static final String UPLOAD_ID_KEY_PREFIX = "vita:file:upload-id:";
    private static final String UPLOAD_PART_NUM_KEY_PREFIX = "vita:file:upload-part-num:";
    private static final long UPLOAD_EXPIRE_SECONDS = 7 * 86400L;

    private final FileProperty fileProperty;
    private final FileStorage fileStorage;
    private final RedisCache redisCache;
    private final FileObjectKeyGenerator keyGenerator;

    public FileApplicationService(FileProperty fileProperty, FileStorage fileStorage,
                                  RedisCache redisCache, FileObjectKeyGenerator keyGenerator) {
        this.fileProperty = fileProperty;
        this.fileStorage = fileStorage;
        this.redisCache = redisCache;
        this.keyGenerator = keyGenerator;
    }

    /**
     * 获取 uploadId。
     */
    public String getUploadId(FileUploadRequestDto dto) {
        validatePrefixDir(dto.getPrefixDir());
        validateSuffix(dto.getSuffix());

        String uploadId;
        if (dto.getIsPart() != null && dto.getIsPart() == 1) {
            // 分片上传：调用存储层初始化
            String objectKey = keyGenerator.generate(fileProperty,
                    FileVisibilityEnum.isPublic(dto.getIsPublic()), dto.getSuffix(), dto.getPrefixDir());
            FileUploadCommand command = FileUploadCommand.builder()
                    .objectKey(objectKey)
                    .isPublic(FileVisibilityEnum.isPublic(dto.getIsPublic()))
                    .build();
            uploadId = fileStorage.initMultipartUpload(command);
            if (CharSequenceUtil.isBlank(uploadId)) {
                throw new ServiceException(GlobalErrorCode.FILE_UPLOAD_FAILED);
            }
        } else {
            uploadId = UUID.randomUUID().toString().replace("-", "");
        }

        // 写入 Redis 缓存
        FileUploadRequestDto cachedDto = new FileUploadRequestDto();
        cachedDto.setIsPublic(dto.getIsPublic());
        cachedDto.setIsPart(dto.getIsPart());
        cachedDto.setSuffix(dto.getSuffix());
        cachedDto.setFileSize(dto.getFileSize());
        cachedDto.setPrefixDir(dto.getPrefixDir());
        cachedDto.setOriginName(dto.getOriginName());
        cachedDto.setUploadId(uploadId);

        String redisKey = UPLOAD_ID_KEY_PREFIX + uploadId;
        redisCache.setCacheObject(redisKey, JSONObject.toJSONString(cachedDto), UPLOAD_EXPIRE_SECONDS, TimeUnit.SECONDS);

        return uploadId;
    }

    /**
     * 校验 uploadId 有效性。
     */
    public boolean validateUploadId(String uploadId) {
        String redisKey = UPLOAD_ID_KEY_PREFIX + uploadId;
        String json = redisCache.getCacheObject(redisKey);
        return CharSequenceUtil.isNotBlank(json);
    }

    /**
     * 上传单个文件。
     */
    public FileUploadVo uploadSingleFile(MultipartFile file, String uploadId) {
        FileUploadRequestDto dto = getAndValidateUploadId(uploadId);
        if (dto.getIsPart() != null && dto.getIsPart() == 1) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(), "该 uploadId 为分片上传使用");
        }
        validateFileSuffix(dto.getSuffix());
        validateFileSize(file.getSize());

        String objectKey = keyGenerator.generate(fileProperty,
                FileVisibilityEnum.isPublic(dto.getIsPublic()), dto.getSuffix(), dto.getPrefixDir());

        FileUploadCommand command = FileUploadCommand.builder()
                .objectKey(objectKey)
                .isPublic(FileVisibilityEnum.isPublic(dto.getIsPublic()))
                .uploadId(uploadId)
                .build();

        fileStorage.upload(file, command);

        // 删除 Redis 缓存
        deleteUploadIdCache(uploadId);

        return buildUploadVo(dto, objectKey, uploadId);
    }

    /**
     * 上传分片。
     */
    public FileUploadVo uploadPartFile(MultipartFile file, String uploadId, Integer partNum, Integer totalParts) {
        FileUploadRequestDto dto = getAndValidateUploadId(uploadId);
        if (partNum == null || totalParts == null) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST);
        }
        validateFileSize(file.getSize());

        FileUploadCommand command = FileUploadCommand.builder()
                .objectKey(keyGenerator.generate(fileProperty,
                        FileVisibilityEnum.isPublic(dto.getIsPublic()), dto.getSuffix(), dto.getPrefixDir()))
                .isPublic(FileVisibilityEnum.isPublic(dto.getIsPublic()))
                .uploadId(uploadId)
                .partNum(partNum)
                .totalParts(totalParts)
                .partSize(file.getSize())
                .build();

        fileStorage.uploadPart(file, command);

        // 缓存已上传片数
        String partNumKey = UPLOAD_PART_NUM_KEY_PREFIX + uploadId;
        redisCache.setCacheObject(partNumKey, partNum, UPLOAD_EXPIRE_SECONDS, TimeUnit.SECONDS);

        return FileUploadVo.builder()
                .uploadId(uploadId)
                .uri(command.getObjectKey())
                .bucketName(fileStorage.getBucketName(command.getIsPublic()))
                .hostName(fileStorage.getHostname(command.getIsPublic()))
                .build();
    }

    /**
     * 完成分片上传。
     */
    public FileUploadVo completeUpload(String uploadId) {
        FileUploadRequestDto dto = getAndValidateUploadId(uploadId);

        String objectKey = keyGenerator.generate(fileProperty,
                FileVisibilityEnum.isPublic(dto.getIsPublic()), dto.getSuffix(), dto.getPrefixDir());

        FileUploadCommand command = FileUploadCommand.builder()
                .objectKey(objectKey)
                .isPublic(FileVisibilityEnum.isPublic(dto.getIsPublic()))
                .uploadId(uploadId)
                .build();

        fileStorage.completeMultipartUpload(command);

        // 删除 Redis 缓存
        deleteUploadIdCache(uploadId);
        redisCache.deleteObject(UPLOAD_PART_NUM_KEY_PREFIX + uploadId);

        return buildUploadVo(dto, objectKey, uploadId);
    }

    /**
     * 取消上传。
     */
    public void abortUpload(String uploadId) {
        FileUploadRequestDto dto = getAndValidateUploadId(uploadId);
        String objectKey = keyGenerator.generate(fileProperty,
                FileVisibilityEnum.isPublic(dto.getIsPublic()), dto.getSuffix(), dto.getPrefixDir());
        fileStorage.abortMultipartUpload(uploadId, objectKey);
        deleteUploadIdCache(uploadId);
    }

    /**
     * 获取文件访问 URL。
     */
    public FileAccessUrlVo getAccessUrl(String objectKey) {
        boolean isPublic = isPublicByKeyName(objectKey);
        FileAccessCommand command = FileAccessCommand.builder()
                .objectKey(objectKey)
                .isPublic(isPublic)
                .build();

        FileAccessResult result = fileStorage.generateAccessUrl(command);
        return FileAccessUrlVo.builder()
                .objectKey(objectKey)
                .accessUrl(result.getAccessUrl())
                .isPublic(isPublic)
                .build();
    }

    /**
     * 批量获取文件访问 URL。
     */
    public List<FileAccessUrlVo> batchGetAccessUrls(List<String> objectKeys) {
        return objectKeys.stream()
                .map(this::getAccessUrl)
                .collect(Collectors.toList());
    }

    /**
     * 获取分片列表。
     */
    public List<String> listParts(String uploadId) {
        FileUploadRequestDto dto = getAndValidateUploadId(uploadId);
        String objectKey = keyGenerator.generate(fileProperty,
                FileVisibilityEnum.isPublic(dto.getIsPublic()), dto.getSuffix(), dto.getPrefixDir());
        FileUploadCommand command = FileUploadCommand.builder()
                .objectKey(objectKey)
                .isPublic(FileVisibilityEnum.isPublic(dto.getIsPublic()))
                .uploadId(uploadId)
                .build();
        return fileStorage.listParts(command);
    }

    /**
     * Base64 图片上传。
     */
    public FileUploadVo uploadBase64(String prefixDir, String imageBase64) {
        imageBase64 = parseBase64(imageBase64);
        byte[] bytes = Base64.getDecoder().decode(imageBase64.getBytes());
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes)) {
            String suffix = "png";
            String objectKey = keyGenerator.generate(fileProperty, true, suffix, prefixDir);
            FileUploadCommand command = FileUploadCommand.builder()
                    .objectKey(objectKey)
                    .isPublic(true)
                    .build();
            fileStorage.uploadByStream(inputStream, command);

            return FileUploadVo.builder()
                    .uri(objectKey)
                    .fileUrl(fileStorage.getHostname(true) + "/" + fileStorage.getBucketName(true) + "/" + objectKey)
                    .build();
        } catch (Exception e) {
            log.warn("Base64 图片上传失败", e);
            throw new ServiceException(GlobalErrorCode.FILE_UPLOAD_FAILED);
        }
    }

    /**
     * 获取 Redis 中的 uploadId 元数据。
     */
    private FileUploadRequestDto getAndValidateUploadId(String uploadId) {
        String redisKey = UPLOAD_ID_KEY_PREFIX + uploadId;
        String json = redisCache.getCacheObject(redisKey);
        if (CharSequenceUtil.isBlank(json)) {
            throw new ServiceException(GlobalErrorCode.FILE_UPLOAD_EXPIRED);
        }
        return JSONObject.parseObject(json, FileUploadRequestDto.class);
    }

    private void deleteUploadIdCache(String uploadId) {
        redisCache.deleteObject(UPLOAD_ID_KEY_PREFIX + uploadId);
    }

    private FileUploadVo buildUploadVo(FileUploadRequestDto dto, String objectKey, String uploadId) {
        boolean isPublic = FileVisibilityEnum.isPublic(dto.getIsPublic());
        String bucketName = fileStorage.getBucketName(isPublic);
        String hostname = fileStorage.getHostname(isPublic);

        String fileUrl = hostname + "/" + bucketName + "/" + objectKey;
        if (CharSequenceUtil.isNotBlank(dto.getOriginName())) {
            fileUrl += "?originName=" + dto.getOriginName();
        }

        String signUrl = "";
        if (!isPublic) {
            FileAccessCommand command = FileAccessCommand.builder()
                    .objectKey(objectKey)
                    .isPublic(false)
                    .build();
            FileAccessResult result = fileStorage.generateAccessUrl(command);
            signUrl = result.getAccessUrl();
        }

        return FileUploadVo.builder()
                .bucketName(bucketName)
                .hostName(hostname)
                .uri(objectKey)
                .fileUrl(fileUrl)
                .signUrl(signUrl)
                .uploadId(uploadId)
                .build();
    }

    private void validateFileSuffix(String suffix) {
        List<String> allowed = fileProperty.getAllowedExtensions();
        if (allowed != null && !allowed.isEmpty()) {
            if (!allowed.contains(suffix.toLowerCase())) {
                throw new ServiceException(GlobalErrorCode.FILE_FORMAT_NOT_SUPPORTED);
            }
        }
    }

    private void validateSuffix(String suffix) {
        if (CharSequenceUtil.isBlank(suffix)) {
            throw new ServiceException(GlobalErrorCode.FILE_PART_SUFFIX_REQUIRED);
        }
    }

    private void validatePrefixDir(String prefixDir) {
        if (CharSequenceUtil.isBlank(prefixDir)) {
            return;
        }
        if (prefixDir.contains("/")) {
            throw new ServiceException(GlobalErrorCode.FILE_PREFIX_DIR_NOT_SUPPORT_MULTI_LEVEL);
        }
        if (prefixDir.length() > 30) {
            throw new ServiceException(GlobalErrorCode.FILE_PREFIX_DIR_TOO_LONG);
        }
    }

    private void validateFileSize(long fileSize) {
        Long maxFileSize = fileProperty.getMaxFileSize();
        if (maxFileSize != null && fileSize > maxFileSize) {
            throw new ServiceException(GlobalErrorCode.FILE_SIZE_EXCEEDS_LIMIT);
        }
    }

    private boolean isPublicByKeyName(String objectKey) {
        String publicPrefix = fileProperty.getPublicPrefix();
        return objectKey.startsWith(publicPrefix + "/");
    }

    private String parseBase64(String imageBase64) {
        if (CharSequenceUtil.isBlank(imageBase64)) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(), "Base64 图片不能为空");
        }
        int baseIndex = imageBase64.indexOf(";base64,");
        if (baseIndex >= 0) {
            return imageBase64.substring(baseIndex + 8);
        }
        return imageBase64;
    }
}
