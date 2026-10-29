package com.muzikApp.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import com.muzikApp.config.AppProperties;
import com.muzikApp.exception.BadRequestException;

class FileStorageServiceTest {

    @TempDir
    Path tmp;

    private FileStorageService storage() {
        return new FileStorageService(new AppProperties("http://localhost", "http://localhost:4200",
                new AppProperties.Jwt("x".repeat(32), Duration.ofMinutes(1), Duration.ofMinutes(1)),
                new AppProperties.Storage(tmp.resolve("songs").toString(), tmp.resolve("images").toString())));
    }

    @Test
    void storesSongUnderARandomNameKeepingTheExtension() throws Exception {
        String url = storage().storeSong(new MockMultipartFile("file", "My Song.MP3", "audio/mpeg", "abc".getBytes()));

        assertThat(url).startsWith("/uploads/songs/").endsWith(".mp3").doesNotContain("My Song");
        Path stored = tmp.resolve("songs").resolve(url.substring("/uploads/songs/".length()));
        assertThat(Files.readString(stored)).isEqualTo("abc");
    }

    @Test
    void clientFileNameCannotEscapeTheUploadFolder() {
        String url = storage().storeSong(
                new MockMultipartFile("file", "../../../etc/evil.mp3", "audio/mpeg", "x".getBytes()));

        assertThat(url).doesNotContain("..").doesNotContain("evil");
        assertThat(tmp.resolve("songs").toFile().list()).hasSize(1);
    }

    @Test
    void rejectsWrongContentType() {
        assertThatThrownBy(() -> storage().storeSong(
                new MockMultipartFile("file", "song.mp3", "text/plain", "x".getBytes())))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("audio/");
    }

    @Test
    void rejectsExtensionNotOnTheAllowList() {
        assertThatThrownBy(() -> storage().storeSong(
                new MockMultipartFile("file", "virus.exe", "audio/mpeg", "x".getBytes())))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("extension");
    }

    @Test
    void rejectsEmptyFile() {
        assertThatThrownBy(() -> storage().storeImage(
                new MockMultipartFile("image", "cover.png", "image/png", new byte[0])))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void deleteRemovesTheFileButNothingOutsideTheFolder() throws Exception {
        FileStorageService storage = storage();
        String url = storage.storeImage(new MockMultipartFile("image", "c.png", "image/png", "png".getBytes()));
        Path outside = Files.writeString(tmp.resolve("keep.txt"), "keep");

        storage.delete(url);
        storage.delete("/uploads/images/../keep.txt");

        assertThat(tmp.resolve("images").toFile().list()).isEmpty();
        assertThat(outside).exists();
    }
}
