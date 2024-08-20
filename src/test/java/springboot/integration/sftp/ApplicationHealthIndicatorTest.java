package springboot.integration.sftp;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class ApplicationHealthIndicatorTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ApplicationHealthIndicator healthIndicator;

    @Test
    void down() throws Exception {
        healthIndicator.error("got some error");
        this.mockMvc.perform(get("/actuator/health/application"))
                .andExpect(status().is(503))
                .andExpect(content().string("{\"status\":\"DOWN\",\"details\":{\"message\":\"got some error\"}}"));
    }

    @Test
    void up() throws Exception {
        healthIndicator.success();
        this.mockMvc.perform(get("/actuator/health/application"))
                .andExpect(status().isOk())
                .andExpect(content().string("{\"status\":\"UP\"}"));
    }

    @Test
    void loadBalancerOk() throws Exception {
        healthIndicator.success();
        this.mockMvc.perform(get("/actuator/info"))
                .andExpect(status().isOk());
    }

}