package org.orgname.solace.broker.api;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Configuration class for JPA (Java Persistence API).
 * Enables JPA Auditing to automatically handle auditing fields like creation and modification dates.
 */
@Configuration
@EnableJpaAuditing  // Enables auditing
public class JpaConfig {
}
