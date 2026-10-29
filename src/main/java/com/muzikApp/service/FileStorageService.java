package com.muzikApp.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.muzikApp.config.AppProperties;
import com.muzikApp.exception.BadRequestException;

/**
 * Stores uploads under a random name. The client's file name is never used as a path
 * (no "../../etc/passwd"), only its extension, and only from an allow-list.
 */
@Service
public class FileStorageService {

    private static final Logger LOG = LoggerFactory.getLogger(FileStorageService.class);
    static final Set<String> AUDIO_EXTENSIONS = Set.of("mp3", "wav", "ogg", "m4a", "flac");
    static final Set<String> IMAGE_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp");

    private final Path songDir;
    private final Path imageDir;

    public FileStorageService(AppProperties properties) {
        this.songDir = Path.of(properties.storage().songDir()).toAbsolutePath().normalize();
        this.imageDir = Path.of(properties.storage().imageDir()).toAbsolutePath().normalize();
    }

    /** @return the public URL path, e.g. /uploads/songs/3f2c....mp3 */
    public String storeSong(MultipartFile file) {
        return store(Upload.of(file), songDir, "audio/", AUDIO_EXTENSIONS, "/uploads/songs/");
    }

    public String storeImage(MultipartFile file) {
        return store(Upload.of(file), imageDir, "image/", IMAGE_EXTENSIONS, "/uploads/images/");
    }

    /** Same checks as an upload, for files the app generates itself (demo data). */
    public String storeSong(String fileName, String contentType, byte[] content) {
        return store(Upload.of(fileName, contentType, content), songDir, "audio/", AUDIO_EXTENSIONS, "/uploads/songs/");
    }

    public String storeImage(String fileName, String contentType, byte[] content) {
        return store(Upload.of(fileName, contentType, content), imageDir, "image/", IMAGE_EXTENSIONS,
                "/uploads/images/");
    }

    /** Best effort: a file that cannot be deleted must not make the whole request fail. */
    public void delete(String urlPath) {
        if (urlPath == null) {
            return;
        }
        Path dir = urlPath.startsWith("/uploads/songs/") ? songDir : imageDir;
        Path file = dir.resolve(urlPath.substring(urlPath.lastIndexOf('/') + 1)).normalize();
        try {
            if (file.startsWith(dir)) {
                Files.deleteIfExists(file);
            }
        } catch (IOException e) {
            LOG.warn("Could not delete {}: {}", file, e.getMessage());
        }
    }

    /** What store() needs from a file, whether it came from an HTTP upload or was generated. */
    private record Upload(String originalName, String contentType, boolean empty, InputStreamSource content) {

        interface InputStreamSource {
            InputStream open() throws IOException;
        }

        static Upload of(MultipartFile file) {
            return file == null ? new Upload(null, null, true, null)
                    : new Upload(file.getOriginalFilename(), file.getContentType(), file.isEmpty(), file::getInputStream);
        }

        static Upload of(String name, String contentType, byte[] bytes) {
            return new Upload(name, contentType, bytes.length == 0, () -> new ByteArrayInputStream(bytes));
        }
    }

    private String store(Upload file, Path dir, String typePrefix, Set<String> extensions, String urlPrefix) {
        if (file.empty()) {
            throw new BadRequestException("File is empty");
        }
        String contentType = file.contentType();
        if (contentType == null || !contentType.toLowerCase(Locale.ROOT).startsWith(typePrefix)) {
            throw new BadRequestException("File must be of type " + typePrefix + "*, was " + contentType);
        }
        String extension = extension(file.originalName());
        if (!extensions.contains(extension)) {
            throw new BadRequestException("File extension must be one of " + extensions.stream().sorted().toList());
        }
        String name = UUID.randomUUID() + "." + extension;
        Path target = dir.resolve(name).normalize();
        try (InputStream in = file.content().open()) {
            Files.createDirectories(dir);
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not store file", e);
        }
        return urlPrefix + name;
    }

    private static String extension(String originalName) {
        if (originalName == null || originalName.lastIndexOf('.') < 0) {
            return "";
        }
        return originalName.substring(originalName.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    }
}
