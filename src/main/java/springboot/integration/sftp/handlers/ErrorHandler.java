package springboot.integration.sftp.handlers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.support.ErrorMessage;
import springboot.integration.sftp.ApplicationHealthIndicator;

/**
 * Handle processing errors and send them to health check
 */
public class ErrorHandler implements MessageHandler {

    private static final Logger L = LoggerFactory.getLogger(ErrorHandler.class);


    @Autowired
    private ApplicationHealthIndicator healthIndicator;


    @Override
    public void handleMessage(Message<?> message) throws MessagingException {
        if (message instanceof ErrorMessage errorMessage) {
            healthIndicator.error(errorMessage.getPayload().getMessage());
        } else if (message instanceof MessagingException messagingException) {
            healthIndicator.error(messagingException.getMostSpecificCause().getMessage());
        }
    }

}
