package springboot.integration.sftp;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.integration.core.MessageSource;
import org.springframework.messaging.MessageHandler;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@EnableAutoConfiguration(exclude = {TransactionAutoConfiguration.class})
public class ApplicationSecurityTest {
    @Autowired
    private MockMvc mockMvc;

    /**
     * Beans to exclude from application context since we don't want to initialize them.
     */
    @TestConfiguration
    public static class TestMockConfig {
        @MockBean
        private SftpAdapter sftpAdapter;
        @MockBean
        private MessageSource sftpMessageSource;
        @MockBean
        @Qualifier("inboundHandler")
        public MessageHandler inboundHandler;
        @MockBean
        @Qualifier("customErrorHandler")
        public MessageHandler customErrorHandler;

    }

    @Test
    void isHealthAllowed() throws Exception {
        this.mockMvc.perform(get("/actuator/health/application"))
                .andExpect(status().isOk());
    }

    @Test
    void isInfoAllowed() throws Exception {
        this.mockMvc.perform(get("/actuator/info"))
                .andExpect(status().isOk());
    }

    @Test
    void otherURLDenied() throws Exception {
        this.mockMvc.perform(get("/actuator/beans"))
                .andExpect(status().isForbidden());
    }


}
