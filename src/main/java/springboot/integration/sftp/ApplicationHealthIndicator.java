package springboot.integration.sftp;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Application Health indicator.
 */
@Component
public class ApplicationHealthIndicator implements HealthIndicator {
    /**
     * Initial status is success
     */
    private boolean success = true;
    /**
     * Details message in case of errors
     */
    private String details;

    /**
     * Mark health-check as healthy.
     */
    public void success() {
        this.success = true;
    }

    /**
     * Mark health-check as error.
     *
     * @param details error details
     */
    public void error(final String details) {
        this.details = details;
        this.success = false;
    }

    /**
     * Retrieve health-check status.
     *
     * @return
     */
    public boolean isSuccess() {
        return success;
    }

    /**
     * Actuator health-check logic.
     *
     * @return Health report
     */
    @Override
    public Health health() {
        if (!success) {
            return Health.down()
                    .withDetail("message", details).build();
        }
        return Health.up().build();
    }

}
