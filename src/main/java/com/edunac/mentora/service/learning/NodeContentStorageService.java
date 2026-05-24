package com.edunac.mentora.service.learning;

import com.edunac.mentora.domain.learning.ContentType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class NodeContentStorageService {

    public static final String PUBLIC_PREFIX = "/uploads/node-contents/";

    private static final Set<String> VIDEO_EXTENSIONS = Set.of("mp4", "webm", "mov", "avi", "mkv");
    private static final Set<String> FILE_EXTENSIONS = Set.of(
            "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx",
            "zip", "rar", "txt", "png", "jpg", "jpeg", "gif"
    );

    private final Path baseDir;

    public NodeContentStorageService(
            @Value("${mentora.upload.node-contents-dir:uploads/node-contents}") String baseDirPath
    ) {
        this.baseDir = Paths.get(baseDirPath).toAbsolutePath().normalize();
    }

    public String store(MultipartFile file, ContentType type) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vui lòng chọn tệp để tải lên");
        }

        String extension = extractExtension(file.getOriginalFilename());
        validateExtension(extension, type);

        String subFolder = type == ContentType.VIDEO ? "videos" : "files";
        Path targetDir = baseDir.resolve(subFolder);
        String storedName = UUID.randomUUID() + "." + extension;
        Path targetFile = targetDir.resolve(storedName);

        try {
            Files.createDirectories(targetDir);
            file.transferTo(targetFile);
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Không thể lưu tệp tải lên");
        }

        return PUBLIC_PREFIX + subFolder + "/" + storedName;
    }

    public void deleteIfManaged(String contentUrl) {
        if (contentUrl == null || !contentUrl.startsWith(PUBLIC_PREFIX)) {
            return;
        }
        String relative = contentUrl.substring(PUBLIC_PREFIX.length());
        Path filePath = baseDir.resolve(relative).normalize();
        if (!filePath.startsWith(baseDir)) {
            return;
        }
        try {
            Files.deleteIfExists(filePath);
        } catch (IOException ignored) {
            // Best-effort cleanup
        }
    }

    public Path getBaseDir() {
        return baseDir;
    }

    private void validateExtension(String extension, ContentType type) {
        Set<String> allowed = type == ContentType.VIDEO ? VIDEO_EXTENSIONS : FILE_EXTENSIONS;
        if (!allowed.contains(extension)) {
            String allowedList = String.join(", ", allowed);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Định dạng không hỗ trợ. Cho phép: " + allowedList);
        }
    }

    private String extractExtension(String originalFilename) {
        if (originalFilename == null || !originalFilename.contains(".")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tên tệp không hợp lệ");
        }
        return originalFilename.substring(originalFilename.lastIndexOf('.') + 1)
                .toLowerCase(Locale.ROOT);
    }
}
