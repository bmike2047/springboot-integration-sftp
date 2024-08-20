package springboot.integration.sftp.handlers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessagingException;
import org.springframework.stereotype.Component;
import springboot.integration.sftp.ApplicationHealthIndicator;
import springboot.integration.sftp.processors.PrimeNumberProcessor;
import springboot.integration.sftp.processors.Processor;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Stream;

/**
 * Handle received files.
 */
@Component
public class ReceivedFilesHandler implements MessageHandler {

    private static final Logger LOG = LoggerFactory.getLogger(ReceivedFilesHandler.class);

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private ApplicationHealthIndicator healthIndicator;

    @Override
    public void handleMessage(Message<?> message) throws MessagingException {
        File inFile = new File(String.valueOf(message.getPayload()));
        LOG.info("Received file: {}", inFile.getName());
        Processor processor = new PrimeNumberProcessor();
        try (Stream<String> filesStream = Files.lines(
                Paths.get(inFile.getAbsolutePath()), StandardCharsets.UTF_8)) {

            filesStream.forEach(processor::process);

            healthIndicator.success();

        } catch (Exception e) {
            throw new MessagingException(message, "Got processing error: " + e.getMessage());
        }

    }
}
