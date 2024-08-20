package springboot.integration.sftp.utils;

import org.apache.sshd.sftp.client.SftpClient;
import org.springframework.integration.file.remote.RemoteFileTemplate;
import org.springframework.integration.file.remote.SessionCallback;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.stream.Stream;

/**
 * SFTP utils
 */
public class SftpClientUtils {

    public static void createTestFile(RemoteFileTemplate<SftpClient.DirEntry> template,
                                      String fileName,
                                      String content) throws IOException {
        try (ByteArrayInputStream stream = new ByteArrayInputStream(content.getBytes())) {
            template.execute((SessionCallback<SftpClient.DirEntry, Void>) session -> {
                stream.reset();
                session.write(stream, fileName);
                return null;
            });
        }
    }


    public static void deleteTestFile(RemoteFileTemplate<SftpClient.DirEntry> template,
                                      String fileName) {
        template.execute((SessionCallback<SftpClient.DirEntry, Void>) session -> {
            session.remove(fileName);
            return null;
        });
    }


    public static void deleteLocalFiles(String path) {
        Path defaultHomeDir = Paths.get(path);
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

}
