package com.vita.file.storage;

import cn.hutool.core.text.CharSequenceUtil;
import com.vita.core.exception.GlobalErrorCode;
import com.vita.core.exception.ServiceException;
import com.vita.file.property.FileProperty;
import com.vita.file.support.FileAccessCommand;
import com.vita.file.support.FileAccessResult;
import com.vita.file.support.FileObjectKeyGenerator;
import com.vita.file.support.FileSignatureSupport;
import com.vita.file.support.FileUploadCommand;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.ObjectWriteResponse;
import io.minio.PutObjectArgs;
import io.minio.http.Method;
import io.minio.messages.ListPartsResult;
import io.minio.messages.Part;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * MinIO 文件存储实现。
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "vita.file.active", havingValue = "minio")
public class MinioFileStorage implements FileStorage {

    private final VitaMinioClient minioClient;
    private final FileProperty fileProperty;

    public MinioFileStorage(FileProperty fileProperty, FileObjectKeyGenerator keyGenerator,
                            FileSignatureSupport signatureSupport) {
        this.fileProperty = fileProperty;
        MinioClient client = MinioClient.builder()
                .endpoint(fileProperty.getMinio().getEndpoint())
                .credentials(fileProperty.getMinio().getAccessKey(), fileProperty.getMinio().getSecretKey())
                .build();
        this.minioClient = new VitaMinioClient(client);
    }

    @Override
    public String getType() {
        return "minio";
    }

    @Override
    public void upload(MultipartFile file, FileUploadCommand command) {
        try (InputStream inputStream = file.getInputStream()) {
            PutObjectArgs args = PutObjectArgs.builder()
                    .bucket(getBucketName(command.getIsPublic()))
                    .object(command.getObjectKey())
                    .stream(inputStream, file.getSize(), -1)
                    .build();
            minioClient.putObject(args);
        } catch (Exception e) {
            log.warn("MinIO 上传失败, objectKey={}", command.getObjectKey(), e);
            throw new ServiceException(GlobalErrorCode.FILE_UPLOAD_FAILED);
        }
    }

    @Override
    public void uploadByStream(InputStream inputStream, FileUploadCommand command) {
        try {
            PutObjectArgs args = PutObjectArgs.builder()
                    .bucket(getBucketName(command.getIsPublic()))
                    .object(command.getObjectKey())
                    .stream(inputStream, inputStream.available(), -1)
                    .build();
            minioClient.putObject(args);
        } catch (Exception e) {
            log.warn("MinIO 流上传失败, objectKey={}", command.getObjectKey(), e);
            throw new ServiceException(GlobalErrorCode.FILE_UPLOAD_FAILED);
        }
    }

    @Override
    public String initMultipartUpload(FileUploadCommand command) {
        String uploadId = minioClient.initMultiPartUpload(
                getBucketName(command.getIsPublic()), command.getObjectKey());
        if (uploadId == null) {
            throw new ServiceException(GlobalErrorCode.FILE_UPLOAD_FAILED);
        }
        return uploadId;
    }

    @Override
    public void uploadPart(MultipartFile file, FileUploadCommand command) {
        try (InputStream inputStream = file.getInputStream()) {
            minioClient.uploadPart(inputStream,
                    getBucketName(command.getIsPublic()), command.getObjectKey(),
                    command.getUploadId(), command.getPartNum());
        } catch (Exception e) {
            log.warn("MinIO 上传分片失败, partNum={}", command.getPartNum(), e);
            throw new ServiceException(GlobalErrorCode.FILE_UPLOAD_FAILED);
        }
    }

    @Override
    public void completeMultipartUpload(FileUploadCommand command) {
        List<Part> parts = listPartsInternal(command);
        if (parts.isEmpty()) {
            throw new ServiceException(GlobalErrorCode.FILE_PART_NOT_FOUND);
        }
        Part[] partArray = parts.toArray(new Part[0]);
        ObjectWriteResponse response = minioClient.completeMultipartUpload(
                getBucketName(command.getIsPublic()), command.getObjectKey(),
                command.getUploadId(), partArray);
        if (response == null) {
            throw new ServiceException(GlobalErrorCode.FILE_MERGE_FAILED);
        }
    }

    @Override
    public void abortMultipartUpload(String uploadId, String objectKey) {
        minioClient.abortMultipartUpload(getBucketName(true), uploadId, objectKey);
    }

    @Override
    public FileAccessResult generateAccessUrl(FileAccessCommand command) {
        try {
            String bucket = getBucketName(command.getIsPublic());
            int expireSeconds = fileProperty.getSignExpireSeconds() != null
                    ? fileProperty.getSignExpireSeconds() : 300;
            String url = minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(bucket)
                    .object(command.getObjectKey())
                    .extraQueryParams(command.getExtraParams())
                    .expiry(expireSeconds, TimeUnit.SECONDS)
                    .build());
            return FileAccessResult.builder()
                    .accessUrl(url)
                    .signed(!Boolean.TRUE.equals(command.getIsPublic()))
                    .build();
        } catch (Exception e) {
            log.warn("MinIO 生成签名 URL 失败, objectKey={}", command.getObjectKey(), e);
            return FileAccessResult.builder()
                    .accessUrl("")
                    .signed(false)
                    .build();
        }
    }

    @Override
    public List<String> listParts(FileUploadCommand command) {
        List<Part> parts = listPartsInternal(command);
        List<String> result = new ArrayList<>();
        for (Part part : parts) {
            result.add("part." + part.partNumber() + " size=" + part.partSize());
        }
        return result;
    }

    @Override
    public String getBucketName(boolean isPublic) {
        return fileProperty.getMinio().getBucket();
    }

    @Override
    public String getHostname(boolean isPublic) {
        String publicBaseUrl = fileProperty.getMinio().getPublicBaseUrl();
        if (Boolean.TRUE.equals(isPublic) && CharSequenceUtil.isNotBlank(publicBaseUrl)) {
            return publicBaseUrl;
        }
        return fileProperty.getMinio().getEndpoint();
    }

    private List<Part> listPartsInternal(FileUploadCommand command) {
        List<Part> allParts = new ArrayList<>();
        int partNumberMarker = 0;
        boolean isTruncated;
        do {
            io.minio.ListPartsResponse response = minioClient.listParts(
                    getBucketName(command.getIsPublic()), command.getObjectKey(),
                    null, partNumberMarker, command.getUploadId());
            if (response == null) {
                throw new ServiceException(GlobalErrorCode.FILE_PART_NOT_FOUND);
            }
            ListPartsResult result = response.result();
            allParts.addAll(result.partList());
            partNumberMarker = result.nextPartNumberMarker();
            isTruncated = result.isTruncated();
        } while (isTruncated);
        return allParts;
    }
}
