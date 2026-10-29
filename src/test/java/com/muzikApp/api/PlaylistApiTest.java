package com.muzikApp.api;

import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import com.jayway.jsonpath.JsonPath;

class PlaylistApiTest extends ApiTestSupport {

    private long createPlaylist(String token, String name, boolean isPublic) throws Exception {
        String body = mvc.perform(post("/api/playlists").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\",\"isPublic\":" + isPublic + "}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    /** Regression: with "boolean isPublic" + Lombok the JSON field was called "public". */
    @Test
    void jsonFieldIsCalledIsPublic() throws Exception {
        mvc.perform(post("/api/playlists").header("Authorization", bearer(registerAndGetToken("json")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Road trip\",\"isPublic\":true}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.isPublic").value(true))
                .andExpect(jsonPath("$.public").doesNotExist());
    }

    @Test
    void playlistIsPrivateByDefault() throws Exception {
        mvc.perform(post("/api/playlists").header("Authorization", bearer(registerAndGetToken("default")))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Quiet\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.isPublic").value(false));
    }

    @Test
    void nameIsRequiredAndAtMost100Characters() throws Exception {
        String token = registerAndGetToken("names");
        mvc.perform(post("/api/playlists").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/playlists").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"" + "x".repeat(101) + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0]").value("name: Playlist name must not exceed 100 characters."));
    }

    @Test
    void someoneElsesPrivatePlaylistLooksLikeItDoesNotExist() throws Exception {
        long id = createPlaylist(registerAndGetToken("alice"), "Secret mix", false);

        mvc.perform(get("/api/playlists/" + id).header("Authorization", bearer(registerAndGetToken("bob"))))
                .andExpect(status().isNotFound());
    }

    @Test
    void publicPlaylistCanBeReadButNotChangedByOthers() throws Exception {
        String alice = registerAndGetToken("alice");
        long id = createPlaylist(alice, "Shared mix", true);
        String bob = registerAndGetToken("bob");

        mvc.perform(get("/api/playlists/" + id).header("Authorization", bearer(bob)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Shared mix"));
        mvc.perform(put("/api/playlists/" + id).header("Authorization", bearer(bob))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Hijacked\"}"))
                .andExpect(status().isNotFound());
        mvc.perform(delete("/api/playlists/" + id).header("Authorization", bearer(bob)))
                .andExpect(status().isNotFound());
        mvc.perform(post("/api/playlists/" + id + "/songs/" + uploadSong(bob, "Bob song"))
                        .header("Authorization", bearer(bob)))
                .andExpect(status().isNotFound());
    }

    @Test
    void songsGetPositionsAndRemovingOneClosesTheGap() throws Exception {
        String token = registerAndGetToken("dj");
        long playlist = createPlaylist(token, "Set", false);
        long first = uploadSong(token, "First");
        long second = uploadSong(token, "Second");
        long third = uploadSong(token, "Third");
        for (long song : new long[] { first, second, third }) {
            mvc.perform(post("/api/playlists/" + playlist + "/songs/" + song).header("Authorization", bearer(token)))
                    .andExpect(status().isOk());
        }

        mvc.perform(delete("/api/playlists/" + playlist + "/songs/" + second).header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.songCount").value(2))
                .andExpect(jsonPath("$.songs[*].title", contains("First", "Third")))
                .andExpect(jsonPath("$.songs[*].position", contains(1, 2)))
                .andExpect(jsonPath("$.songs[0].album").value("Album"))
                .andExpect(jsonPath("$.songs[0].songUrl", org.hamcrest.Matchers.startsWith("http://localhost:8042/")));
    }

    @Test
    void theSameSongCannotBeAddedTwice() throws Exception {
        String token = registerAndGetToken("twice");
        long playlist = createPlaylist(token, "Once", false);
        long song = uploadSong(token, "Only once");

        mvc.perform(post("/api/playlists/" + playlist + "/songs/" + song).header("Authorization", bearer(token)))
                .andExpect(status().isOk());
        mvc.perform(post("/api/playlists/" + playlist + "/songs/" + song).header("Authorization", bearer(token)))
                .andExpect(status().isConflict());
    }

    @Test
    void deletingASongRemovesItFromPlaylists() throws Exception {
        String token = registerAndGetToken("remove");
        long playlist = createPlaylist(token, "Mix", false);
        long keep = uploadSong(token, "Keep");
        long gone = uploadSong(token, "Gone");
        mvc.perform(post("/api/playlists/" + playlist + "/songs/" + gone).header("Authorization", bearer(token)));
        mvc.perform(post("/api/playlists/" + playlist + "/songs/" + keep).header("Authorization", bearer(token)));

        mvc.perform(delete("/api/songs/" + gone).header("Authorization", bearer(token)))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/playlists/" + playlist).header("Authorization", bearer(token)))
                .andExpect(jsonPath("$.songs[*].title", contains("Keep")))
                .andExpect(jsonPath("$.songs[0].position").value(1));
    }

    @Test
    void ownerCanUpdateAndDelete() throws Exception {
        String token = registerAndGetToken("owner");
        long id = createPlaylist(token, "Old name", false);
        mvc.perform(post("/api/playlists/" + id + "/songs/" + uploadSong(token, "S")).header("Authorization", bearer(token)));

        mvc.perform(put("/api/playlists/" + id).header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"New name\",\"isPublic\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("New name"))
                .andExpect(jsonPath("$.isPublic").value(true));
        mvc.perform(delete("/api/playlists/" + id).header("Authorization", bearer(token)))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/playlists/" + id).header("Authorization", bearer(token)))
                .andExpect(status().isNotFound());
    }

    @Test
    void publicListContainsOnlyPublicPlaylists() throws Exception {
        String token = registerAndGetToken("lists");
        createPlaylist(token, "Visible-" + System.nanoTime(), true);
        createPlaylist(token, "Hidden-" + System.nanoTime(), false);

        mvc.perform(get("/api/playlists/public").param("size", "100").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].isPublic", org.hamcrest.Matchers.everyItem(
                        org.hamcrest.Matchers.is(true))));
        mvc.perform(get("/api/playlists/me").header("Authorization", bearer(token)))
                .andExpect(jsonPath("$.totalElements").value(2));
    }
}
