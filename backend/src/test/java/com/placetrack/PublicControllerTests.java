package com.placetrack;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "spring.datasource.url=jdbc:h2:mem:placetracktest;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1")
class PublicControllerTests {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void healthEndpointIsPublic() throws Exception {
        mockMvc.perform(get("/api/public/health"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("UP"))
            .andExpect(jsonPath("$.application").value("PlaceTrack"));
    }

    @Test
    void summaryEndpointIsDatabaseBackedAndPublic() throws Exception {
        mockMvc.perform(get("/api/public/summary"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.invitedStudents").value(1))
            .andExpect(jsonPath("$.registeredStudents").value(1))
            .andExpect(jsonPath("$.totalCompanies").value(1))
            .andExpect(jsonPath("$.publishedDrives").value(1))
            .andExpect(jsonPath("$.totalApplications").value(0));
    }

    @Test
    void invitationPreviewIsPublicForValidCode() throws Exception {
        mockMvc.perform(get("/api/auth/invitation").param("code", "invite-student-demo"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.studentName").value("Student Two"))
            .andExpect(jsonPath("$.collegeEmail").value("student2@college.edu"))
            .andExpect(jsonPath("$.branch").value("CSE"));
    }

    @Test
    void coordinatorCanLogin() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                .contentType("application/json")
                .content("{\"email\":\"coordinator@college.edu\",\"password\":\"Password@123\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").isString())
            .andExpect(jsonPath("$.user.role").value("PLACEMENT_COORDINATOR"));
    }
}
