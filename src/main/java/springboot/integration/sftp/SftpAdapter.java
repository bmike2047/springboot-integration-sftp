package springboot.integration.sftp;

import org.apache.sshd.sftp.client.SftpClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.integration.annotation.InboundChannelAdapter;
import org.springframework.integration.annotation.Poller;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.core.MessageSource;
import org.springframework.integration.dsl.Pollers;
import org.springframework.integration.file.filters.AcceptOnceFileListFilter;
import org.springframework.integration.file.filters.CompositeFileListFilter;
import org.springframework.integration.file.remote.session.CachingSessionFactory;
import org.springframework.integration.file.remote.session.SessionFactory;
import org.springframework.integration.metadata.SimpleMetadataStore;
import org.springframework.integration.scheduling.PollerMetadata;
import org.springframework.integration.sftp.filters.SftpPersistentAcceptOnceFileListFilter;
import org.springframework.integration.sftp.inbound.SftpInboundFileSynchronizer;
import org.springframework.integration.sftp.inbound.SftpInboundFileSynchronizingMessageSource;
import org.springframework.integration.sftp.session.DefaultSftpSessionFactory;
import org.springframework.integration.transaction.DefaultTransactionSynchronizationFactory;
import org.springframework.integration.transaction.ExpressionEvaluatingTransactionSynchronizationProcessor;
import org.springframework.integration.transaction.PseudoTransactionManager;
import org.springframework.integration.transaction.TransactionSynchronizationFactory;
import org.springframework.messaging.MessageHandler;
import springboot.integration.sftp.handlers.ErrorHandler;
import springboot.integration.sftp.handlers.ReceivedFilesHandler;
import springboot.integration.sftp.utils.TransactionManagerOps;

import java.io.File;
import java.util.List;

/**
 * Main SFTP adapter setup and configuration.
 */
@Configuration
public class SftpAdapter {
    /**
     * ApplicationContext for transaction manager
     */
    @Autowired
    private ApplicationContext applicationContext;
    /**
     * ApplicationProperties for SFTP connection parameters and local file storage.
     */
    @Autowired
    private ApplicationProperties applicationProperties;

    /**
     * Session factory configuration.
     * Using caching session to maintain permanent open connections.
     * Testing of stale sessions for better stability.
     * Each session will create its own connection.
     */
    @Bean
    public SessionFactory<SftpClient.DirEntry> sftpSessionFactory() {
        DefaultSftpSessionFactory factory = new DefaultSftpSessionFactory();
        factory.setHost(applicationProperties.getSftpHost());
        factory.setPort(applicationProperties.getSftpPort());
        factory.setUser(applicationProperties.getSftpUsername());
        factory.setKnownHostsResource(applicationProperties.getSftpKnownHostsFile());
        factory.setPrivateKey(applicationProperties.getSftpPrivateKeyFile());
        CachingSessionFactory<SftpClient.DirEntry> cachingSessionFactory = new CachingSessionFactory<>(factory);
        cachingSessionFactory.setTestSession(true);
        cachingSessionFactory.setPoolSize(applicationProperties.getSftpPoolSize());
        cachingSessionFactory.setSessionWaitTimeout(applicationProperties.getSftpSessionWaitTimeout());
        return cachingSessionFactory;
    }

    /**
     * Handles the synchronization between remote SFTP directory and local mount.
     *
     * @return SftpInboundFileSynchronizer
     */
    @Bean
    public SftpInboundFileSynchronizer sftpFileSynchronizer() {
        SftpInboundFileSynchronizer fileSynchronizer = new SftpInboundFileSynchronizer(sftpSessionFactory());
        fileSynchronizer.setDeleteRemoteFiles(false);
        fileSynchronizer.setPreserveTimestamp(true);

        CompositeFileListFilter<SftpClient.DirEntry> remoteFilter = new CompositeFileListFilter<>(
                List.of(new SftpPersistentAcceptOnceFileListFilter(
                        new SimpleMetadataStore(), "sftpMessageSource"))
        );
        fileSynchronizer.setFilter(remoteFilter);
        fileSynchronizer.setRemoteDirectory(applicationProperties.getSftpRemoteDir());
        return fileSynchronizer;
    }

    /**
     * Filter to prevent file duplication.
     * Needed as a bean also for the transaction manager.
     *
     * @return AcceptOnceFileListFilter
     */
    @Bean
    public AcceptOnceFileListFilter acceptOnceFileListFilter() {
        return new AcceptOnceFileListFilter<>();
    }

    /**
     * Synchronize remote files to local.
     *
     * @return received file as a message
     */
    @Bean
    @InboundChannelAdapter(channel = "sftpChannel", poller = @Poller(value = "pollerMetadata"))
    public MessageSource<File> sftpMessageSource() {
        SftpInboundFileSynchronizingMessageSource source =
                new SftpInboundFileSynchronizingMessageSource(sftpFileSynchronizer());
        source.setLocalDirectory(new File(applicationProperties.getSftpLocalDir()));
        source.setAutoCreateLocalDirectory(true);
        source.setLocalFilter(acceptOnceFileListFilter());
        source.setMaxFetchSize(applicationProperties.getSftpMaxFetchSize());
        return source;
    }

    /**
     * Poller configuration.
     *
     * @return PollerMetadata
     */
    @Bean
    public PollerMetadata pollerMetadata() {
        return Pollers.fixedDelay(applicationProperties.getSftpPollerDelay())
                .transactional(transactionManager())
                .transactionSynchronizationFactory(transactionSynchronizationFactory())
                .maxMessagesPerPoll(applicationProperties.getSftpMaxMessagesPerPoll())
                .getObject();
    }

    /**
     * Logical transaction manager.
     *
     * @return PseudoTransactionManager
     */
    @Bean
    PseudoTransactionManager transactionManager() {
        return new PseudoTransactionManager();
    }

    /**
     * Helper bean for the transaction manager.
     *
     * @return TransactionManagerOps
     */
    @Bean
    public TransactionManagerOps managerOps() {
        return new TransactionManagerOps();
    }

    /**
     * Transaction manager rollback and commit logic.
     *
     * @return TransactionSynchronizationFactory
     */
    @Bean
    TransactionSynchronizationFactory transactionSynchronizationFactory() {
        ExpressionParser parser = new SpelExpressionParser();
        ExpressionEvaluatingTransactionSynchronizationProcessor syncProcessor =
                new ExpressionEvaluatingTransactionSynchronizationProcessor();
        syncProcessor.setBeanFactory(applicationContext.getAutowireCapableBeanFactory());
        syncProcessor.setAfterRollbackExpression(
                parser.parseExpression("@acceptOnceFileListFilter.remove(payload) and " +
                        "@managerOps.error(payload.name)"));
        syncProcessor.setAfterCommitExpression(
                parser.parseExpression(
                        "payload.delete() and @acceptOnceFileListFilter.remove(payload) and " +
                                "@managerOps.success(payload.name)"));
        return new DefaultTransactionSynchronizationFactory(syncProcessor);
    }

    /**
     * Handler for incoming files.
     *
     * @return MessageHandler
     */
    @Bean
    @ServiceActivator(inputChannel = "sftpChannel")
    public MessageHandler inboundHandler() {
        return new ReceivedFilesHandler();
    }

    /**
     * Handler for error messages.
     *
     * @return MessageHandler
     */
    @Bean
    @ServiceActivator(inputChannel = "errorChannel")
    public MessageHandler customErrorHandler() {
        return new ErrorHandler();
    }

}

