package springboot.integration.sftp;

import org.apache.sshd.sftp.client.SftpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
 * Main SFTP setup and configuration.
 */
@Configuration
public class SftpAdapter {

    private static final Logger L = LoggerFactory.getLogger(SftpAdapter.class);

    @Autowired
    private ApplicationContext applicationContext;
    @Autowired
    private ApplicationProperties applicationProperties;

    /**
     * Using caching session to maintain permanent open connections.
     * Testing of stale sessions is enabled.
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
     * Needed as a Bean for the transaction manager.
     */
    @Bean
    public AcceptOnceFileListFilter acceptOnceFileListFilter() {
        return new AcceptOnceFileListFilter<>();
    }

    /**
     * Synchronize remote files to local.
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
     * Config poller.
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
     */
    @Bean
    PseudoTransactionManager transactionManager() {
        return new PseudoTransactionManager();
    }

    /**
     * Helper Bean for the transaction manager.
     */
    @Bean
    public TransactionManagerOps managerOps() {
        return new TransactionManagerOps();
    }

    /**
     * Rollback and commit logic.
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

    @Bean
    @ServiceActivator(inputChannel = "sftpChannel")
    public MessageHandler inboundHandler() {
        return new ReceivedFilesHandler();
    }

    @Bean
    @ServiceActivator(inputChannel = "errorChannel")
    public MessageHandler customErrorHandler() {
        return new ErrorHandler();
    }

}

