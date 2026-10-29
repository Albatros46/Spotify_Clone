package com.muzikApp.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import com.muzikApp.repository.AppUserRepository;

class AuthApiTest extends ApiTestSupport {

    @Autowired
    private AppUserRepository users;

    @Test
    void registerReturnsTokensAndAUserRole() throws Exception {
        String email = uniqueEmail("anna");
        String body = register(email);

        assertThat(read(body, "$.email")).isEqualTo(email);
        assertThat(read(body, "$.role")).isEqualTo("USER");
        assertThat(read(body, "$.accessToken")).isNotBlank();
        assertThat(body).doesNotContain(PASSWORD).doesNotContain("password");
    }

    /** Security: the old RegisterUserRequest had a "role" field; a client must not become ADMIN. */
    @Test
    void registeringWithRoleAdminStillCreatesAUser() throws Exception {
        String email = uniqueEmail("mallory");
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Mallory\",\"email\":\"" + email
                                + "\",\"password\":\"" + PASSWORD + "\",\"role\":\"ADMIN\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("USER"));

        assertThat(users.findByEmailIgnoreCase(email).orElseThrow().getRole().name()).isEqualTo("USER");
    }

    @Test
    void passwordIsStoredAsBcryptHashAndRefreshTokenAsSha256() throws Exception {
        String email = uniqueEmail("hash");
        String refreshToken = read(register(email), "$.refreshToken");

        var stored = users.findByEmailIgnoreCase(email).orElseThrow();
        assertThat(stored.getPassword()).startsWith("$2").isNotEqualTo(PASSWORD);
        assertThat(stored.getRefreshToken()).hasSize(64).isNotEqualTo(refreshToken);
    }

    @Test
    void duplicateEmailIsRejectedCaseInsensitively() throws Exception {
        String email = uniqueEmail("dup");
        register(email);

        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(json("name", "Again", "email", email.toUpperCase(), "password", PASSWORD)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Email is already registered"));
    }

    @Test
    void invalidRegistrationListsEveryFieldError() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(json("name", "Al", "email", "not-an-email", "password", "short")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.errors", containsInAnyOrder(
                        "email: Email should be valid.",
                        "name: Name must be between 3 and 50 characters!",
                        "password: Password must be between 8 and 72 characters.")));
    }

    @Test
    void malformedJsonGives400NotA500() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content("{not json"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void wrongPasswordAndUnknownUserGetTheSameAnswer() throws Exception {
        String email = uniqueEmail("login");
        register(email);

        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json("email", email, "password", "wrong-password")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json("email", uniqueEmail("nobody"), "password", PASSWORD)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    void protectedEndpointWithoutTokenGivesJson401() throws Exception {
        mvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.path").value("/api/users/me"));
    }

    @Test
    void garbageTokenGives401() throws Exception {
        mvc.perform(get("/api/users/me").header("Authorization", bearer("not.a.jwt")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void meReturnsTheProfileWithoutTokens() throws Exception {
        String email = uniqueEmail("me");
        String token = read(register(email), "$.accessToken");

        mvc.perform(get("/api/users/me").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.accessToken").doesNotExist())
                .andExpect(jsonPath("$.refreshToken").doesNotExist());
    }

    @Test
    void refreshRotatesTheTokenAndTheOldOneStopsWorking() throws Exception {
        String oldRefresh = read(register(uniqueEmail("rotate")), "$.refreshToken");

        String body = mvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                        .content(json("refreshToken", oldRefresh)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(read(body, "$.refreshToken")).isNotEqualTo(oldRefresh);

        mvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                        .content(json("refreshToken", oldRefresh)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Refresh token was already used or revoked"));
    }

    @Test
    void accessTokenCannotBeUsedToRefresh() throws Exception {
        String access = read(register(uniqueEmail("type")), "$.accessToken");

        mvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                        .content(json("refreshToken", access)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logoutRevokesTheRefreshToken() throws Exception {
        String body = register(uniqueEmail("logout"));

        mvc.perform(post("/api/auth/logout").header("Authorization", bearer(read(body, "$.accessToken"))))
                .andExpect(status().isOk());
        mvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                        .content(json("refreshToken", read(body, "$.refreshToken"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void changePasswordNeedsTheOldPasswordAndThenTheNewOneWorks() throws Exception {
        String email = uniqueEmail("pw");
        String token = read(register(email), "$.accessToken");

        mvc.perform(put("/api/users/me/password").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("oldPassword", "wrong-old", "newPassword", "NewSecret456!")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Old password is wrong"));
        mvc.perform(put("/api/users/me/password").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("oldPassword", PASSWORD, "newPassword", "NewSecret456!")))
                .andExpect(status().isOk());

        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json("email", email, "password", PASSWORD)))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json("email", email, "password", "NewSecret456!")))
                .andExpect(status().isOk());
    }

    @Test
    void profileCannotTakeAnotherUsersEmail() throws Exception {
        String taken = uniqueEmail("taken");
        register(taken);
        String token = registerAndGetToken("other");

        mvc.perform(put("/api/users/me").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("name", "Other User", "email", taken)))
                .andExpect(status().isConflict());
    }

    @Test
    void corsAllowsOnlyTheConfiguredFrontend() throws Exception {
        mvc.perform(options("/api/songs")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
        mvc.perform(options("/api/songs")
                        .header("Origin", "http://127.0.0.1:5173")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isOk());
        mvc.perform(options("/api/songs")
                        .header("Origin", "http://evil.example")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isForbidden());
    }

    /** Regression: the catch-all handler used to turn these into 500. */
    @Test
    void springHttpErrorsKeepTheirStatus() throws Exception {
        String token = registerAndGetToken("status");
        mvc.perform(get("/api/does-not-exist").header("Authorization", bearer(token)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
        mvc.perform(put("/api/songs").header("Authorization", bearer(token)))
                .andExpect(status().isMethodNotAllowed());
        mvc.perform(post("/api/playlists").header("Authorization", bearer(token))
                        .contentType(MediaType.TEXT_PLAIN).content("x"))
                .andExpect(status().isUnsupportedMediaType());
        mvc.perform(get("/uploads/songs/missing.mp3"))
                .andExpect(status().isNotFound());
    }

    @Test
    void errorResponsesNeverContainStackTraces() throws Exception {
        mvc.perform(get("/api/songs/abc").header("Authorization", bearer(registerAndGetToken("trace"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.trace").doesNotExist())
                .andExpect(jsonPath("$.exception").doesNotExist());
    }
}
