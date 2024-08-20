package springboot.integration.sftp;


import org.apache.sshd.sftp.client.SftpClient;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.data.mongo.AutoConfigureDataMongo;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.integration.file.remote.RemoteFileTemplate;
import org.springframework.integration.file.remote.session.CachingSessionFactory;
import org.springframework.integration.file.remote.session.SessionFactory;
import springboot.integration.sftp.utils.EmbeddedSftpServer;
import springboot.integration.sftp.utils.SftpClientUtils;
import springboot.integration.sftp.utils.TransactionManagerOps;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureDataMongo
@EnableAutoConfiguration
public class SftpAdapterTest {

    private static final Logger LOG = LoggerFactory.getLogger(SftpAdapterTest.class);
    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private ApplicationProperties applicationProperties;

    @Autowired
    private EmbeddedSftpServer server;

    @Autowired
    private ApplicationHealthIndicator healthIndicator;

    @Autowired
    public TransactionManagerOps managerOps;

    @TestConfiguration
    public static class TestConfig {

        @Autowired
        private ApplicationProperties applicationProperties;

        @Bean
        @Primary
        public EmbeddedSftpServer embeddedSftpServer() {
            EmbeddedSftpServer server = new EmbeddedSftpServer();
            server.setPort(applicationProperties.getSftpPort());
            server.setUploadDir(applicationProperties.getSftpRemoteDir());
            server.start();
            return server;
        }

    }

    @Test
    void inSftpWithTransactionManagerSuccess() {
        String fileName = "numbers1.txt";
        File remoteFile = uploadFile(fileName, "12345\n 5\n");

        //TransactionManager reported success
        assertThat(managerOps.inspect(remoteFile.getName())).isTrue();

        //local fileName was deleted since it was successful processed
        File localFile = new File(applicationProperties.getSftpLocalDir() + File.separator + fileName);
        assertThat(localFile.exists()).isFalse();
        //healthy
        assertThat(healthIndicator.isSuccess()).isTrue();
        //clean it since we are done
        deleteUploadedFile(remoteFile);
    }

    @Test
    void inSftpWithTransactionManagerError() {
        String fileName = "numbers2.txt";
        File remoteFile = uploadFile(fileName, "not a number");

        //TransactionManager reported error
        assertThat(managerOps.inspect(remoteFile.getName())).isFalse();

        //local file should be present for retry on the next poll since an error happened
        File localFile = new File(applicationProperties.getSftpLocalDir() + File.separator + fileName);
        assertThat(localFile.exists()).isTrue();
        // not healthy
        assertThat(healthIndicator.isSuccess()).isFalse();
        //clean it since we are done
        SftpClientUtils.deleteLocalFiles(localFile.getPath());
        deleteUploadedFile(remoteFile);

    }

    private File uploadFile(String filename, String content) {
        File file = null;
        try {
            @SuppressWarnings("unchecked")
            SessionFactory<SftpClient.DirEntry> inSessionFactory =
                    applicationContext.getBean("sftpSessionFactory", CachingSessionFactory.class);
            RemoteFileTemplate<SftpClient.DirEntry> inTemplate = new RemoteFileTemplate<>(inSessionFactory);
            file = new File(applicationProperties.getSftpRemoteDir() + File.separator + filename);
            SftpClientUtils.createTestFile(inTemplate, file.getPath(), content);
            //wait for poller to process the file
            while (managerOps.inspect(file.getName()) == null) {
                Thread.sleep(500);
            }
        } catch (Exception e) {
            LOG.info("Got exception: ", e);
        }
        return file;
    }

    private void deleteUploadedFile(File file) {
        try {
            @SuppressWarnings("unchecked")
            SessionFactory<SftpClient.DirEntry> inSessionFactory =
                    applicationContext.getBean("sftpSessionFactory", CachingSessionFactory.class);
            RemoteFileTemplate<SftpClient.DirEntry> inTemplate = new RemoteFileTemplate<>(inSessionFactory);
            SftpClientUtils.deleteTestFile(inTemplate, file.getPath());
        } catch (Exception e) {
            LOG.info("Got exception: ", e);
        }
    }


}
