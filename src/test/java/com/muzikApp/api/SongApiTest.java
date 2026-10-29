package com.muzikApp.api;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;

import com.muzikApp.entities.Role;
import com.muzikApp.repository.AppUserRepository;

class SongApiTest extends ApiTestSupport {

    @Autowired
    private AppUserRepository users;

    @Test
    void uploadReturnsAbsoluteUrlsAndTheFileIsServed() throws Exception {
        String token = registerAndGetToken("uploader");
        String body = mvc.perform(multipart("/api/songs")
                        .file(new MockMultipartFile("file", "track.mp3", "audio/mpeg", "mp3-bytes".getBytes()))
                        .file(new MockMultipartFile("image", "cover.png", "image/png", "png-bytes".getBytes()))
                        .param("title", "Bohemian").param("artist", "Queen").param("album", "Opera")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.songUrl", startsWith("http://localhost:8042/uploads/songs/")))
                .andExpect(jsonPath("$.imageUrl", startsWith("http://localhost:8042/uploads/images/")))
                .andExpect(jsonPath("$.appUserName").value("Test User"))
                .andReturn().getResponse().getContentAsString();

        String path = read(body, "$.songUrl").replace("http://localhost:8042", "");
        mvc.perform(get(path)) // static files are public: the browser's <audio> tag sends no token
                .andExpect(status().isOk())
                .andExpect(content().bytes("mp3-bytes".getBytes()));
    }

    @Test
    void uploadWithoutTokenIs401() throws Exception {
        mvc.perform(multipart("/api/songs")
                        .file(new MockMultipartFile("file", "a.mp3", "audio/mpeg", "x".getBytes()))
                        .param("title", "t").param("artist", "a").param("album", "b"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void textFileDisguisedAsSongIsRejected() throws Exception {
        mvc.perform(multipart("/api/songs")
                        .file(new MockMultipartFile("file", "notes.mp3", "text/plain", "hello".getBytes()))
                        .param("title", "t").param("artist", "a").param("album", "b")
                        .header("Authorization", bearer(registerAndGetToken("txt"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", startsWith("File must be of type audio/")));
    }

    @Test
    void missingAudioFileIs400() throws Exception {
        mvc.perform(multipart("/api/songs")
                        .param("title", "t").param("artist", "a").param("album", "b")
                        .header("Authorization", bearer(registerAndGetToken("nofile"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void missingTitleIsAValidationError() throws Exception {
        mvc.perform(multipart("/api/songs")
                        .file(new MockMultipartFile("file", "a.mp3", "audio/mpeg", "x".getBytes()))
                        .param("artist", "a").param("album", "b")
                        .header("Authorization", bearer(registerAndGetToken("notitle"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors", hasItem("title: Title is required.")));
    }

    @Test
    void searchFindsByTitleArtistOrAlbumIgnoringCase() throws Exception {
        String token = registerAndGetToken("search");
        String marker = "Zq" + UUID.randomUUID().toString().substring(0, 6);
        uploadSong(token, marker + " Night");
        uploadSong(token, "Something else");

        mvc.perform(get("/api/songs").param("search", marker.toLowerCase()).header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].title").value(marker + " Night"));
    }

    @Test
    void listIsPaginated() throws Exception {
        String token = registerAndGetToken("pages");
        for (int i = 0; i < 3; i++) {
            uploadSong(token, "Paged " + i);
        }

        mvc.perform(get("/api/songs/me").param("size", "2").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.first").value(true))
                .andExpect(jsonPath("$.last").value(false));
    }

    @Test
    void unknownSongIs404() throws Exception {
        mvc.perform(get("/api/songs/999999").header("Authorization", bearer(registerAndGetToken("404"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Song 999999 not found"));
    }

    @Test
    void onlyTheOwnerOrAnAdminMayDeleteASong() throws Exception {
        String owner = registerAndGetToken("owner");
        long songId = uploadSong(owner, "Mine");

        mvc.perform(delete("/api/songs/" + songId).header("Authorization", bearer(registerAndGetToken("stranger"))))
                .andExpect(status().isForbidden());

        String adminEmail = uniqueEmail("admin");
        String adminToken = read(register(adminEmail), "$.accessToken");
        var admin = users.findByEmailIgnoreCase(adminEmail).orElseThrow();
        admin.setRole(Role.ADMIN);
        users.save(admin);

        mvc.perform(delete("/api/songs/" + songId).header("Authorization", bearer(adminToken)))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/songs/" + songId).header("Authorization", bearer(owner)))
                .andExpect(status().isNotFound());
    }

    @Test
    void ownerCanDeleteAndTheFileIsGone() throws Exception {
        String token = registerAndGetToken("cleanup");
        long id = uploadSong(token, "Temporary");
        String url = read(mvc.perform(get("/api/songs/" + id).header("Authorization", bearer(token)))
                .andReturn().getResponse().getContentAsString(), "$.songUrl").replace("http://localhost:8042", "");

        mvc.perform(delete("/api/songs/" + id).header("Authorization", bearer(token)))
                .andExpect(status().isNoContent());
        mvc.perform(get(url)).andExpect(status().isNotFound());
    }
}
