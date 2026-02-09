package com.carzavenue.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import com.carzavenue.backend.config.GoogleOAuthProperties;
import io.github.cdimascio.dotenv.Dotenv;

import java.util.Arrays;
import java.nio.file.Path;

@SpringBootApplication
@EnableConfigurationProperties(GoogleOAuthProperties.class)
public class Application {
    public static void main(String[] args) {
        loadDotenvForLocalProfiles();
        SpringApplication.run(Application.class, args);
    }

    private static void loadDotenvForLocalProfiles() {
        Dotenv dotenv = Dotenv.configure()
                .directory(resolveDotenvDirectory())
                .ignoreIfMissing()
                .load();
        applyIfMissing("GOOGLE_CLIENT_ID", dotenv);
        applyIfMissing("GOOGLE_CLIENT_SECRET", dotenv);
        applyIfMissing("GOOGLE_REDIRECT_URI", dotenv);
        applyIfMissing("MAIL_HOST", dotenv);
        applyIfMissing("MAIL_PORT", dotenv);
        applyIfMissing("MAIL_USERNAME", dotenv);
        applyIfMissing("MAIL_PASSWORD", dotenv);
        applyIfMissing("MAIL_SMTP_AUTH", dotenv);
        applyIfMissing("MAIL_SMTP_STARTTLS", dotenv);
        applyIfMissing("RESET_PASSWORD_FRONTEND_URL", dotenv);
        applyIfMissing("RESET_PASSWORD_FROM_EMAIL", dotenv);
        applyIfMissing("RESET_PASSWORD_EXPIRATION_MINUTES", dotenv);
        applyIfMissing("INVOO_API_KEY", dotenv);
        applyIfMissing("INVOO_BASE_URL", dotenv);
        applyIfMissing("INVOO_SUCCESS_URL", dotenv);
        applyIfMissing("INVOO_CANCEL_URL", dotenv);
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

    private static String resolveDotenvDirectory() {
        Path current = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        for (int i = 0; i < 6 && current != null; i++) {
            if (current.resolve(".env").toFile().exists()) {
                return current.toString();
            }
            current = current.getParent();
        }
        return System.getProperty("user.dir");
    }
}
