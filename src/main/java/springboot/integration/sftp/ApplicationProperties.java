package springboot.integration.sftp;

import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.validation.annotation.Validated;

/**
 * Application Properties.
 */
@Configuration
@ConfigurationProperties
@Validated
public class ApplicationProperties {

    @NotNull
    private String sftpUsername;

    @NotNull
    private String sftpHost;

    @NotNull
    private Integer sftpPort;

    @NotNull
    private FileSystemResource sftpPrivateKeyFile;

    @NotNull
    private FileSystemResource sftpKnownHostsFile;

    @NotNull
    private String sftpRemoteDir;

    @NotNull
    private String sftpLocalDir;

    @NotNull
    private Integer sftpPoolSize;

    @NotNull
    private Integer sftpSessionWaitTimeout;

    @NotNull
    private Integer sftpPollerDelay;

    @NotNull
    private Integer sftpMaxFetchSize;

    @NotNull
    private Integer sftpMaxMessagesPerPoll;

    public @NotNull String getSftpUsername() {
        return sftpUsername;
    }

    public void setSftpUsername(@NotNull String sftpUsername) {
        this.sftpUsername = sftpUsername;
    }

    public @NotNull String getSftpHost() {
        return sftpHost;
    }

    public void setSftpHost(@NotNull String sftpHost) {
        this.sftpHost = sftpHost;
    }

    public @NotNull Integer getSftpPort() {
        return sftpPort;
    }

    public void setSftpPort(@NotNull Integer sftpPort) {
        this.sftpPort = sftpPort;
    }

    public @NotNull String getSftpRemoteDir() {
        return sftpRemoteDir;
    }

    public void setSftpRemoteDir(@NotNull String sftpRemoteDir) {
        this.sftpRemoteDir = sftpRemoteDir;
    }

    public @NotNull FileSystemResource getSftpPrivateKeyFile() {
        return sftpPrivateKeyFile;
    }

    public void setSftpPrivateKeyFile(@NotNull FileSystemResource sftpPrivateKeyFile) {
        this.sftpPrivateKeyFile = sftpPrivateKeyFile;
    }

    public @NotNull FileSystemResource getSftpKnownHostsFile() {
        return sftpKnownHostsFile;
    }

    public void setSftpKnownHostsFile(@NotNull FileSystemResource sftpKnownHostsFile) {
        this.sftpKnownHostsFile = sftpKnownHostsFile;
    }

    public @NotNull Integer getSftpMaxMessagesPerPoll() {
        return sftpMaxMessagesPerPoll;
    }

    public void setSftpMaxMessagesPerPoll(@NotNull Integer sftpMaxMessagesPerPoll) {
        this.sftpMaxMessagesPerPoll = sftpMaxMessagesPerPoll;
    }

    public @NotNull Integer getSftpMaxFetchSize() {
        return sftpMaxFetchSize;
    }

    public void setSftpMaxFetchSize(@NotNull Integer sftpMaxFetchSize) {
        this.sftpMaxFetchSize = sftpMaxFetchSize;
    }

    public @NotNull Integer getSftpPollerDelay() {
        return sftpPollerDelay;
    }

    public void setSftpPollerDelay(@NotNull Integer sftpPollerDelay) {
        this.sftpPollerDelay = sftpPollerDelay;
    }

    public @NotNull Integer getSftpSessionWaitTimeout() {
        return sftpSessionWaitTimeout;
    }

    public void setSftpSessionWaitTimeout(@NotNull Integer sftpSessionWaitTimeout) {
        this.sftpSessionWaitTimeout = sftpSessionWaitTimeout;
    }

    public @NotNull Integer getSftpPoolSize() {
        return sftpPoolSize;
    }

    public void setSftpPoolSize(@NotNull Integer sftpPoolSize) {
        this.sftpPoolSize = sftpPoolSize;
    }

    public @NotNull String getSftpLocalDir() {
        return sftpLocalDir;
    }

    public void setSftpLocalDir(@NotNull String sftpLocalDir) {
        this.sftpLocalDir = sftpLocalDir;
    }
}
