package net.jrodolfo.solace.broker.api.service;

/**
 * Interface for accessing system environment variables.
 * This abstraction allows for easier testing by mocking environment variable access.
 */
public interface EnvironmentConfig {
    /**
     * Retrieves the value of the specified environment variable.
     *
     * @param name the name of the environment variable
     * @return the string value of the variable, or {@code null} if it is not defined
     */
    String getEnv(String name);
}
