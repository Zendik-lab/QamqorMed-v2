package com.qamqormed.config;

import com.qamqormed.ai.OllamaClient;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.env.SystemEnvironmentPropertySource;

import java.net.URI;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AiPropertiesTest {

    @ParameterizedTest
    @ValueSource(strings = {"dev", "prod"})
    void bindsOllamaEnvironmentCredentialsThroughAiConfig(String profile) {
        contextRunner(profile, Map.of(
                "OLLAMA_BASE_URL", "https://ollama-tunnel.example.com",
                "OLLAMA_BASIC_AUTH_USERNAME", "test-user",
                "OLLAMA_BASIC_AUTH_PASSWORD", "test-password"
        )).run(context -> {
            assertThat(context).hasNotFailed().hasSingleBean(OllamaClient.class);
            AiProperties properties = context.getBean(AiProperties.class);
            assertThat(properties.getBaseUrl()).isEqualTo(URI.create("https://ollama-tunnel.example.com"));
            assertThat(properties.getBasicAuthUsername()).isEqualTo("test-user");
            assertThat(properties.getBasicAuthPassword()).isEqualTo("test-password");
            if ("prod".equals(profile)) assertThat(properties.isEnabled()).isFalse();
        });
    }

    @ParameterizedTest
    @ValueSource(strings = {"dev", "prod"})
    void credentialsRemainOptional(String profile) {
        contextRunner(profile, Map.of()).run(context -> {
            assertThat(context).hasNotFailed().hasSingleBean(OllamaClient.class);
            AiProperties properties = context.getBean(AiProperties.class);
            assertThat(properties.getBaseUrl()).isEqualTo(URI.create("http://localhost:11434"));
            assertThat(properties.getBasicAuthUsername()).isEmpty();
            assertThat(properties.getBasicAuthPassword()).isEmpty();
            if ("prod".equals(profile)) assertThat(properties.isEnabled()).isFalse();
        });
    }

    private ApplicationContextRunner contextRunner(String profile, Map<String, Object> environment) {
        return new ApplicationContextRunner()
                .withUserConfiguration(AiConfig.class)
                .withPropertyValues("spring.config.location=classpath:/application.yml", "spring.profiles.active=" + profile)
                .withInitializer(context -> context.getEnvironment().getPropertySources().replace(
                        StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME,
                        new SystemEnvironmentPropertySource(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME, environment)))
                .withInitializer(new ConfigDataApplicationContextInitializer());
    }
}
