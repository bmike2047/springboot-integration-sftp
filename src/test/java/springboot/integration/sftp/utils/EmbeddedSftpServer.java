package springboot.integration.sftp.utils;

import org.apache.sshd.common.file.virtualfs.VirtualFileSystemFactory;
import org.apache.sshd.common.keyprovider.FileKeyPairProvider;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.auth.pubkey.AcceptAllPublickeyAuthenticator;
import org.apache.sshd.sftp.server.SftpSubsystemFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Comparator;
import java.util.stream.Stream;

/**
 * Simple Mina sshd server.
 */
public class EmbeddedSftpServer {

    private static final Logger LOG = LoggerFactory.getLogger(EmbeddedSftpServer.class);

    public static final String PATHNAME = System.getProperty("java.io.tmpdir") +
            File.separator + "sftp_test" + File.separator;
    public static final String PRIV_KEY = "ssh_host_ed25519_key";
    private final SshServer server = SshServer.setUpDefaultServer();

    private volatile int port;

    private volatile boolean running;

    private String uploadDir;

    public void setPort(int port) {
        this.port = port;
    }

    public void setUploadDir(String uploadDir) {
        this.uploadDir = uploadDir;
    }

    public void setup() throws URISyntaxException {
        this.server.setPublickeyAuthenticator(AcceptAllPublickeyAuthenticator.INSTANCE);
        this.server.setPort(this.port);
        Path path = Paths.get(getClass().getClassLoader().getResource(PRIV_KEY).toURI());
        this.server.setKeyPairProvider(new FileKeyPairProvider(path));
        server.setSubsystemFactories(Collections.singletonList(new SftpSubsystemFactory()));
        Path defaultHomeDir = Paths.get(PATHNAME);
        new File(PATHNAME).mkdirs();
        server.setFileSystemFactory(new VirtualFileSystemFactory(defaultHomeDir));
        //create upload dir if needed
        if (uploadDir != null && !uploadDir.isEmpty()) {
            new File(PATHNAME + File.separator + uploadDir + File.separator).mkdirs();
        }

    }

    private static void deleteFiles(Path defaultHomeDir) {
        if (Files.exists(defaultHomeDir)) {
            try (Stream<Path> pathStream = Files.walk(defaultHomeDir)) {
                pathStream.sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void start() {
        try {
            this.setup();
            this.server.start();
            this.running = true;
            LOG.info("---> Started embedded SFTP server");
        } catch (IOException | URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }

    public void shutdown() {
        if (this.running) {
            try {
                server.stop(true);
                deleteFiles(Paths.get(PATHNAME));
                LOG.info("---> Stopped embedded SFTP server");
            } catch (Exception e) {
                throw new IllegalStateException(e);
            } finally {
                this.running = false;
            }
        }
    }

}
