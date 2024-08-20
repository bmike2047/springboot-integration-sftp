package springboot.integration.sftp;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Application Health indicator.
 */
@Component
public class ApplicationHealthIndicator implements HealthIndicator {

    private boolean success = true;
    private String details;

    public void success() {
        this.success = true;
    }

    public void error(final String details) {
        this.details = details;
        this.success = false;
    }

    public boolean isSuccess() {
        return success;
    }

    @Override
    public Health health() {
        if (!success) {
            return Health.down()
                    .withDetail("message", details).build();
        }
        return Health.up().build();
    }


}
