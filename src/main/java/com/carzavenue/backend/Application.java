package com.carzavenue.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import com.carzavenue.backend.config.GoogleOAuthProperties;
import io.github.cdimascio.dotenv.Dotenv;

import java.util.Arrays;

@SpringBootApplication
@EnableConfigurationProperties(GoogleOAuthProperties.class)
public class Application {
    public static void main(String[] args) {
        loadDotenvForLocalProfiles();
        SpringApplication.run(Application.class, args);
    }

    private static void loadDotenvForLocalProfiles() {
        if (!isLocalProfileActive()) {
            return;
        }
        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
        applyIfMissing("GOOGLE_CLIENT_ID", dotenv);
        applyIfMissing("GOOGLE_CLIENT_SECRET", dotenv);
        applyIfMissing("GOOGLE_REDIRECT_URI", dotenv);
    }

    private static boolean isLocalProfileActive() {
        String active = System.getProperty("spring.profiles.active");
        if (active == null || active.isBlank()) {
            active = System.getenv("SPRING_PROFILES_ACTIVE");
        }
        if (active == null || active.isBlank()) {
            return false;
        }
        return Arrays.stream(active.split(","))
                .map(String::trim)
                .anyMatch(profile -> profile.equalsIgnoreCase("local") || profile.equalsIgnoreCase("debug"));
    }

    private static void applyIfMissing(String key, Dotenv dotenv) {
        if (System.getProperty(key) != null || System.getenv(key) != null) {
            return;
        }
        String value = dotenv.get(key);
        if (value != null && !value.isBlank()) {
            System.setProperty(key, value);
        }
    }
}
