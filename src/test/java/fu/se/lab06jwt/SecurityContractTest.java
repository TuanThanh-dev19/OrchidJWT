package fu.se.lab06jwt;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fu.se.lab06jwt.entities.User;
import fu.se.lab06jwt.repositories.UserRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.util.Date;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.hasKey;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityContractTest {
    private static final String EMAIL = "student01@fpt.edu.vn";
    private static final String PASSWORD = "P@ssword123";
    private static final String SECRET = "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=";

    @Autowired MockMvc mvc;
    @Autowired UserRepository userRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired ObjectMapper objectMapper;

    @BeforeEach
    void cleanDatabase() {
        userRepository.deleteAll();
    }

    @Test
    void signupCreatesUserWithHashAndSafeResponse() throws Exception {
        mvc.perform(post("/auth/signup").contentType(MediaType.APPLICATION_JSON)
                .content(signupJson(EMAIL, PASSWORD)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.email").value(EMAIL))
            .andExpect(jsonPath("$", not(hasKey("password"))));

        User saved = userRepository.findByEmail(EMAIL).orElseThrow();
        assertFalse(PASSWORD.equals(saved.getPassword()));
        assertTrue(passwordEncoder.matches(PASSWORD, saved.getPassword()));
    }

    @Test
    void duplicateSignupIsConflict() throws Exception {
        register();
        mvc.perform(post("/auth/signup").contentType(MediaType.APPLICATION_JSON)
                .content(signupJson("Student01@FPT.EDU.VN", PASSWORD)))
            .andExpect(status().isConflict());
        assertEquals(1, userRepository.count());
    }

    @Test
    void loginAndProtectedEndpointsReturnCorrectUserWithoutPassword() throws Exception {
        register();
        String token = loginToken();
        assertEquals(3, token.split("\\.").length);

        mvc.perform(get("/users/me").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value(EMAIL))
            .andExpect(jsonPath("$", not(hasKey("password"))));
        mvc.perform(get("/users").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].email").value(EMAIL))
            .andExpect(jsonPath("$[0]", not(hasKey("password"))));
    }

    @Test
    void badPasswordIsUnauthorized() throws Exception {
        register();
        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + EMAIL + "\",\"password\":\"wrong-password\"}"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$", not(hasKey("token"))));
    }

    @Test
    void protectedEndpointWithoutTokenIsUnauthorized() throws Exception {
        mvc.perform(get("/users/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void malformedAndTamperedTokensAreUnauthorized() throws Exception {
        register();
        String token = loginToken();
        mvc.perform(get("/users/me").header("Authorization", "Bearer abc"))
            .andExpect(status().isUnauthorized());

        String[] parts = token.split("\\.");
        String signature = parts[2];
        char replacement = signature.charAt(0) == 'A' ? 'B' : 'A';
        String tampered = parts[0] + "." + parts[1] + "." + replacement + signature.substring(1);
        mvc.perform(get("/users/me").header("Authorization", "Bearer " + tampered))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void expiredTokenIsUnauthorized() throws Exception {
        register();
        SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET));
        String expired = Jwts.builder().subject(EMAIL)
            .issuedAt(new Date(System.currentTimeMillis() - 120_000))
            .expiration(new Date(System.currentTimeMillis() - 60_000))
            .signWith(key).compact();
        mvc.perform(get("/users/me").header("Authorization", "Bearer " + expired))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void invalidEmailIsBadRequestAndCreatesNoUser() throws Exception {
        mvc.perform(post("/auth/signup").contentType(MediaType.APPLICATION_JSON)
                .content(signupJson("invalid-email", PASSWORD)))
            .andExpect(status().isBadRequest());
        assertEquals(0, userRepository.count());
    }

    @Test
    void emptyPasswordIsBadRequestAndCreatesNoUser() throws Exception {
        mvc.perform(post("/auth/signup").contentType(MediaType.APPLICATION_JSON)
                .content(signupJson(EMAIL, "")))
            .andExpect(status().isBadRequest());
        assertEquals(0, userRepository.count());
    }

    private void register() throws Exception {
        mvc.perform(post("/auth/signup").contentType(MediaType.APPLICATION_JSON)
                .content(signupJson(EMAIL, PASSWORD)))
            .andExpect(status().isCreated());
    }

    private String loginToken() throws Exception {
        MvcResult result = mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + EMAIL + "\",\"password\":\"" + PASSWORD + "\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.expiresIn").value(3_600_000))
            .andReturn();
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return body.get("token").asText();
    }

    private String signupJson(String email, String password) {
        return "{\"fullName\":\"SBA301 Student\",\"email\":\"" + email
            + "\",\"password\":\"" + password + "\"}";
    }
}
