package springboot.integration.sftp;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Secured actuator endpoints.
 */
@Configuration
public class ApplicationSecurity {

    @Bean
    SecurityFilterChain permitOnly(final HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(authorize -> authorize
                .requestMatchers("/actuator/info/**", "/actuator/health/**").permitAll()
                .anyRequest().denyAll());
        return http.build();
    }

    /**
     * Fix AuthenticationManager user password in the logs.
     */
    @Bean
    public AuthenticationManager noopAuthenticationManager() {
        return authentication -> {
            throw new AuthenticationServiceException("Authentication is disabled");
        };
    }


}
