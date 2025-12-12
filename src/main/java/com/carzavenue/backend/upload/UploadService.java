package com.carzavenue.backend.upload;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class UploadService {
    private final Path storagePath;
    private final String baseUrl;

    public UploadService(@Value("${app.upload.storage-path}") String storagePath,
                         @Value("${app.upload.base-url}") String baseUrl) throws IOException {
        this.storagePath = Paths.get(storagePath);
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        Files.createDirectories(this.storagePath);
    }

    public List<String> storeImages(MultipartFile[] files) throws IOException {
        List<String> urls = new ArrayList<>();
        for (MultipartFile file : files) {
            if (file.isEmpty()) continue;
            String filename = UUID.randomUUID() + "-" + file.getOriginalFilename();
            Path destination = storagePath.resolve(filename);
            Files.copy(file.getInputStream(), destination);
            urls.add(baseUrl + "/" + filename);
        }
        return urls;
    }
}
