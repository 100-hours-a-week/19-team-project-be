package org.refit.refitbackend.domain.notification.service;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.Notification;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.util.List;

@Service
@Slf4j
public class FcmPushService {

    @Value("${fcm.enabled:false}")
    private boolean enabled;

    @Value("${fcm.credentials-path:}")
    private String credentialsPath;

    @Value("${fcm.project-id:}")
    private String projectId;

    private FirebaseApp firebaseApp;

    public record SendResult(
            int requestedCount,
            int successCount,
            int failureCount,
            boolean attempted
    ) {}

    @PostConstruct
    public void initialize() {
        if (!enabled) {
            log.info("FCM push is disabled");
            return;
        }
        if (credentialsPath == null || credentialsPath.isBlank()) {
            log.warn("FCM is enabled but credentials path is empty");
            return;
        }
        try (FileInputStream serviceAccount = new FileInputStream(credentialsPath)) {
            FirebaseOptions.Builder builder = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount));
            if (projectId != null && !projectId.isBlank()) {
                builder.setProjectId(projectId);
            }
            firebaseApp = FirebaseApp.initializeApp(builder.build(), "refit-fcm");
            log.info("FCM initialized successfully");
        } catch (Exception e) {
            log.error("Failed to initialize FCM", e);
        }
    }

    public SendResult sendToTokens(List<String> tokens, String title, String body) {
        if (!enabled || firebaseApp == null || tokens == null || tokens.isEmpty()) {
            return new SendResult(tokens == null ? 0 : tokens.size(), 0, 0, false);
        }
        try {
            MulticastMessage message = MulticastMessage.builder()
                    .setNotification(Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build())
                    .addAllTokens(tokens)
                    .build();

            BatchResponse response = FirebaseMessaging.getInstance(firebaseApp).sendEachForMulticast(message);
            int successCount = response.getSuccessCount();
            int failureCount = response.getFailureCount();
            log.info("FCM send result requested={}, success={}, failure={}", tokens.size(), successCount, failureCount);
            return new SendResult(tokens.size(), successCount, failureCount, true);
        } catch (FirebaseMessagingException e) {
            log.warn("FCM send failed", e);
            return new SendResult(tokens.size(), 0, tokens.size(), true);
        } catch (Exception e) {
            log.warn("Unexpected FCM send error", e);
            return new SendResult(tokens.size(), 0, tokens.size(), true);
        }
    }
}
