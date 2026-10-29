package com.muzikApp.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.jayway.jsonpath.JsonPath;

/**
 * Real HTTP stack (filters, security, validation, JSON) against an in-memory database.
 * Every test creates its own users with a unique e-mail, so tests do not depend on each other.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
abstract class ApiTestSupport {

    protected static final String PASSWORD = "Secret123!";

    @Autowired
    protected MockMvc mvc;

    protected static String uniqueEmail(String name) {
        return name + "-" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
    }

    /** @return the JSON of the register response */
    protected String register(String email) throws Exception {
        return mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(json("name", "Test User", "email", email, "password", PASSWORD)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
    }

    protected String registerAndGetToken(String name) throws Exception {
        return read(register(uniqueEmail(name)), "$.accessToken");
    }

    protected long uploadSong(String token, String title) throws Exception {
        String body = mvc.perform(multipart("/api/songs")
                        .file(new MockMultipartFile("file", "song.mp3", "audio/mpeg", "ID3-fake-mp3".getBytes()))
                        .param("title", title).param("artist", "Artist").param("album", "Album")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    protected static String bearer(String token) {
        return "Bearer " + token;
    }

    protected static String read(String json, String path) {
        return JsonPath.read(json, path).toString();
    }

    /** Small JSON object builder for flat string fields: json("a", "1", "b", "2"). */
    protected static String json(String... keyValues) {
        StringBuilder sb = new StringBuilder("{");
        for (int i = 0; i < keyValues.length; i += 2) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append('"').append(keyValues[i]).append("\":\"").append(keyValues[i + 1]).append('"');
        }
        return sb.append('}').toString();
    }
}
