package com.muzikApp.demo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.jayway.jsonpath.JsonPath;
import com.muzikApp.repository.PlaylistRepository;
import com.muzikApp.repository.SongRepository;

/** Own in-memory database, so the demo data does not leak into the other API tests. */
@SpringBootTest(properties = {
        "app.demo.enabled=true",
        "spring.datasource.url=jdbc:h2:mem:demo;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;NON_KEYWORDS=POSITION,KEY,VALUE" })
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DemoDataSeederTest {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private DemoDataSeeder seeder;
    @Autowired
    private SongRepository songs;
    @Autowired
    private PlaylistRepository playlists;

    @Test
    void theDemoAccountFromTheReadmeCanLogInAndSeesSongsAndPlaylists() throws Exception {
        String body = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + DemoDataSeeder.EMAIL + "\",\"password\":\""
                                + DemoDataSeeder.PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String token = JsonPath.read(body, "$.accessToken");

        mvc.perform(get("/api/songs").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.totalElements").value(8));
        mvc.perform(get("/api/playlists/me").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.totalElements").value(3));
    }

    @Test
    void demoFilesAreRealWavAndPngAndAreServed() throws Exception {
        var song = songs.findAll().getFirst();

        byte[] audio = mvc.perform(get(song.getSongUrl())).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();
        byte[] image = mvc.perform(get(song.getImageUrl())).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();

        assertThat(new String(audio, 0, 4)).isEqualTo("RIFF");
        assertThat(new String(audio, 8, 4)).isEqualTo("WAVE");
        assertThat(audio).hasSize(44 + 16_000 * 6 * 2); // 6 s, 16 kHz, 16 bit mono
        assertThat(new String(image, 1, 3)).isEqualTo("PNG");
    }

    @Test
    void runningTheSeederAgainCreatesNoDuplicates() {
        long songsBefore = songs.count();
        long playlistsBefore = playlists.count();

        seeder.seed();

        assertThat(songs.count()).isEqualTo(songsBefore);
        assertThat(playlists.count()).isEqualTo(playlistsBefore);
    }
}
