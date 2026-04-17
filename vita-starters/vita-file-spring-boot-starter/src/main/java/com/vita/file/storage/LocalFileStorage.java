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
import com.vita.file.support.LocalFileUrlSupport;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 本地磁盘文件存储实现。
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "vita.file.active", havingValue = "local")
public class LocalFileStorage implements FileStorage {

    private static final String PART_DIR_PREFIX = "part" + File.separator;

    private final FileProperty fileProperty;
    private final FileObjectKeyGenerator keyGenerator;
    private final FileSignatureSupport signatureSupport;
    private final LocalFileUrlSupport localFileUrlSupport;

    public LocalFileStorage(FileProperty fileProperty, FileObjectKeyGenerator keyGenerator,
                            FileSignatureSupport signatureSupport, LocalFileUrlSupport localFileUrlSupport) {
        this.fileProperty = fileProperty;
        this.keyGenerator = keyGenerator;
        this.signatureSupport = signatureSupport;
        this.localFileUrlSupport = localFileUrlSupport;
    }

    @Override
    public String getType() {
        return "local";
    }

    @Override
    public void upload(MultipartFile file, FileUploadCommand command) {
        File targetFile = localFileUrlSupport.resolveFile(command.getObjectKey());
        localFileUrlSupport.ensureParentDirExists(targetFile);
        try {
            file.transferTo(targetFile);
        } catch (IOException e) {
            log.warn("本地文件上传失败, objectKey={}", command.getObjectKey(), e);
            throw new ServiceException(GlobalErrorCode.FILE_UPLOAD_FAILED);
        }
    }

    @Override
    public void uploadByStream(InputStream inputStream, FileUploadCommand command) {
        File targetFile = localFileUrlSupport.resolveFile(command.getObjectKey());
        localFileUrlSupport.ensureParentDirExists(targetFile);
        try {
            Files.copy(inputStream, targetFile.toPath());
        } catch (IOException e) {
            log.warn("本地流上传失败, objectKey={}", command.getObjectKey(), e);
            throw new ServiceException(GlobalErrorCode.FILE_UPLOAD_FAILED);
        }
    }

    @Override
    public String initMultipartUpload(FileUploadCommand command) {
        return command.getUploadId();
    }

    @Override
    public void uploadPart(MultipartFile file, FileUploadCommand command) {
        String partPath = getPartRelativePath(command.getUploadId(), command.getPartNum());
        String basePath = fileProperty.getLocal().getBasePath();
        String bucketName = getBucketName(command.getIsPublic());
        File partFile = new File(basePath + File.separator + bucketName + File.separator + partPath);
        localFileUrlSupport.ensureParentDirExists(partFile);
        try {
            file.transferTo(partFile);
        } catch (IOException e) {
            log.warn("本地分片上传失败, partPath={}", partPath, e);
            throw new ServiceException(GlobalErrorCode.FILE_UPLOAD_FAILED);
        }
    }

    @Override
    public void completeMultipartUpload(FileUploadCommand command) {
        File targetFile = localFileUrlSupport.resolveFile(command.getObjectKey());
        File partDir = getPartDir(command.getUploadId());
        if (!partDir.exists()) {
            throw new ServiceException(GlobalErrorCode.FILE_PART_NOT_FOUND);
        }
        mergePartsByJava(partDir, targetFile);
        // 清理分片临时目录
        deletePartDir(partDir);
    }

    @Override
    public void abortMultipartUpload(String uploadId, String objectKey) {
        deletePartDir(getPartDir(uploadId));
    }

    @Override
    public FileAccessResult generateAccessUrl(FileAccessCommand command) {
        String baseUrl = fileProperty.getLocal().getBasePath();
        if (Boolean.TRUE.equals(command.getIsPublic())) {
            return FileAccessResult.builder()
                    .accessUrl(baseUrl + "/" + getBucketName(true) + "/" + command.getObjectKey())
                    .signed(false)
                    .build();
        }
        // 私有文件生成签名 URL
        try {
            String uri = "/" + getBucketName(false) + "/" + command.getObjectKey();
            String token = signatureSupport.generateToken(fileProperty, uri);
            return FileAccessResult.builder()
                    .accessUrl(baseUrl + uri + "?token=" + token)
                    .signed(true)
                    .build();
        } catch (IllegalArgumentException e) {
            throw new ServiceException(GlobalErrorCode.FILE_SIGN_SECRET_NOT_CONFIGURED);
        }
    }

    @Override
    public List<String> listParts(FileUploadCommand command) {
        File partDir = getPartDir(command.getUploadId());
        if (!partDir.exists()) {
            return new ArrayList<>();
        }
        String[] files = partDir.list();
        if (files == null) {
            return new ArrayList<>();
        }
        return Arrays.asList(files);
    }

    @Override
    public String getBucketName(boolean isPublic) {
        // 本地存储使用统一目录，不区分 bucket
        return "files";
    }

    @Override
    public String getHostname(boolean isPublic) {
        return fileProperty.getLocal().getBasePath();
    }

    private String getPartRelativePath(String uploadId, int partNum) {
        return PART_DIR_PREFIX + uploadId + File.separator + "part." + partNum;
    }

    private File getPartDir(String uploadId) {
        String basePath = fileProperty.getLocal().getBasePath();
        String bucketName = getBucketName(true);
        return new File(basePath + File.separator + bucketName + File.separator
                + PART_DIR_PREFIX + uploadId);
    }

    private void mergePartsByJava(File partDir, File targetFile) {
        File[] partFiles = partDir.listFiles(f -> f.getName().startsWith("part."));
        if (partFiles == null || partFiles.length == 0) {
            throw new ServiceException(GlobalErrorCode.FILE_PART_NOT_FOUND);
        }
        Arrays.sort(partFiles, Comparator.comparingInt(f -> {
            String num = f.getName().replace("part.", "");
            try {
                return Integer.parseInt(num);
            } catch (NumberFormatException e) {
                return Integer.MAX_VALUE;
            }
        }));

        try (RandomAccessFile writeFile = new RandomAccessFile(targetFile, "rw")) {
            long position = 0;
            byte[] buffer = new byte[8192];
            for (File partFile : partFiles) {
                try (RandomAccessFile readFile = new RandomAccessFile(partFile, "r")) {
                    int bytesRead;
                    while ((bytesRead = readFile.read(buffer)) != -1) {
                        writeFile.seek(position);
                        writeFile.write(buffer, 0, bytesRead);
                        position += bytesRead;
                    }
                }
            }
        } catch (IOException e) {
            log.warn("本地文件合并失败", e);
            throw new ServiceException(GlobalErrorCode.FILE_MERGE_FAILED);
        }
    }

    private void deletePartDir(File partDir) {
        if (partDir.exists()) {
            try {
                Files.walk(partDir.toPath())
                        .sorted(Comparator.reverseOrder())
                        .forEach(path -> {
                            try {
                                Files.deleteIfExists(path);
                            } catch (IOException e) {
                                log.warn("删除分片文件失败: {}", path, e);
                            }
                        });
            } catch (IOException e) {
                log.warn("删除分片目录失败: {}", partDir, e);
            }
        }
    }
}
