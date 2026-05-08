package til.stegmueller.centic;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.AutoConfigureDataJpa;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.*;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestTemplate;
import org.springframework.boot.json.JacksonJsonParser;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@AutoConfigureMockMvc
@AutoConfigureDataJpa
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Rollback(false)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CategoryControllerTest {

    @Autowired
    private MockMvc api;

    private final ObjectMapper mapper = new ObjectMapper();

    private Long createdCategoryId;

    // Create
    @Test
    @Order(1)
    void testCreate_asAdmin() throws Exception {
        String token = obtainAccessToken("admin", "1234");

        String response = api.perform(
                        post("/api/categories")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"name\":\"Lebensmittel\",\"colorCode\":\"#00FF00\",\"globalFlag\":true}")
                )
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Lebensmittel"))
                .andReturn().getResponse().getContentAsString();

        createdCategoryId = mapper.readTree(response).get("id").asLong();
    }

    @Test
    @Order(2)
    void testCreate_asUser_forbidden() throws Exception {
        String token = obtainAccessToken("user", "user");

        api.perform(
                        post("/api/categories")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"name\":\"Verboten\",\"colorCode\":\"#FF0000\",\"globalFlag\":false}")
                )
                .andDo(print())
                .andExpect(status().isForbidden());
    }

    // Read
    @Test
    @Order(3)
    void testGetAll_asUser() throws Exception {
        String token = obtainAccessToken("user", "user");

        api.perform(
                        get("/api/categories")
                                .header("Authorization", "Bearer " + token)
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Lebensmittel"));
    }

    @Test
    @Order(4)
    void testGetById_asUser() throws Exception {
        String token = obtainAccessToken("user", "user");

        api.perform(
                        get("/api/categories/" + createdCategoryId)
                                .header("Authorization", "Bearer " + token)
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(createdCategoryId));
    }

    // Update
    @Test
    @Order(5)
    void testUpdate_asAdmin() throws Exception {
        String token = obtainAccessToken("admin", "1234");

        api.perform(
                        put("/api/categories/" + createdCategoryId)
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"name\":\"Lebensmittel aktualisiert\",\"colorCode\":\"#00FF00\",\"globalFlag\":true}")
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Lebensmittel aktualisiert"));
    }

    @Test
    @Order(6)
    void testUpdate_asUser_forbidden() throws Exception {
        String token = obtainAccessToken("user", "user");

        api.perform(
                        put("/api/categories/" + createdCategoryId)
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"name\":\"Hack\",\"colorCode\":\"#FF0000\",\"globalFlag\":false}")
                )
                .andDo(print())
                .andExpect(status().isForbidden());
    }

    // Delete
    @Test
    @Order(7)
    void testDelete_asUser_forbidden() throws Exception {
        String token = obtainAccessToken("user", "user");

        api.perform(
                        delete("/api/categories/" + createdCategoryId)
                                .header("Authorization", "Bearer " + token)
                )
                .andDo(print())
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(8)
    void testDelete_asAdmin() throws Exception {
        String token = obtainAccessToken("admin", "1234");

        api.perform(
                        delete("/api/categories/" + createdCategoryId)
                                .header("Authorization", "Bearer " + token)
                )
                .andDo(print())
                .andExpect(status().isNoContent());
    }

    // Unauthorized
    @Test
    @Order(9)
    void testUnauthorized_noToken() throws Exception {
        api.perform(get("/api/categories"))
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }

    // Token Access
    private String obtainAccessToken(String username, String password) {
        RestTemplate rest = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        String body = "client_id=centic&" +
                "grant_type=password&" +
                "scope=openid profile roles offline_access&" +
                "username=" + username + "&" +
                "password=" + password;

        HttpEntity<String> entity = new HttpEntity<>(body, headers);
        ResponseEntity<String> resp = rest.postForEntity(
                "http://localhost:8080/realms/ILV/protocol/openid-connect/token",
                entity,
                String.class
        );

        JacksonJsonParser jsonParser = new JacksonJsonParser();
        return jsonParser.parseMap(resp.getBody()).get("access_token").toString();
    }
}