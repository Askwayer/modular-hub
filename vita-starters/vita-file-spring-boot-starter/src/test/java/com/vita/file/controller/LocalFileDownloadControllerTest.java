package com.vita.file.controller;

import com.vita.file.property.FileProperty;
import com.vita.file.property.FileSignatureProperty;
import com.vita.file.property.LocalStorageProperty;
import com.vita.file.support.FileSignatureSupport;
import com.vita.file.support.LocalFileUrlSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class LocalFileDownloadControllerTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldDownloadNestedPrivateFileWithValidSignature() throws Exception {
        FileProperty fileProperty = new FileProperty();
        fileProperty.setPublicPrefix("public");
        fileProperty.setPrivatePrefix("private");

        LocalStorageProperty local = new LocalStorageProperty();
        local.setBasePath(tempDir.toString());
        local.setPublicDomain("files.example.com/public");
        local.setPrivateDomain("files.example.com/private");
        fileProperty.setLocal(local);

        FileSignatureProperty signature = new FileSignatureProperty();
        signature.setSecret("unit-test-secret");
        fileProperty.setSignature(signature);

        Path file = tempDir.resolve("private/a/b/c.txt");
        Files.createDirectories(file.getParent());
        Files.write(file, "nested-content".getBytes());

        FileSignatureSupport fileSignatureSupport = new FileSignatureSupport();
        LocalFileDownloadController controller = new LocalFileDownloadController(fileProperty,
                fileSignatureSupport, new LocalFileUrlSupport(fileProperty));

        String uri = "/file/download/files/private/a/b/c.txt";
        String tokenWithTime = fileSignatureSupport.generateToken(fileProperty, uri);
        String[] tokenParts = tokenWithTime.split("&time=");

        MockHttpServletRequest request = new MockHttpServletRequest("GET", uri);
        MockHttpServletResponse response = new MockHttpServletResponse();
        controller.download("files", tokenParts[0], tokenParts[1], request, response);

        assertEquals(200, response.getStatus());
        assertArrayEquals("nested-content".getBytes(), response.getContentAsByteArray());
    }
}
