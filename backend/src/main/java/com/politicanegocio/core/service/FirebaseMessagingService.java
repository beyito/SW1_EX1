package com.politicanegocio.core.service;

import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class FirebaseMessagingService {
    private static final Logger log = LoggerFactory.getLogger(FirebaseMessagingService.class);

    public void sendTaskAssignedNotification(String userToken, String taskName) {
        if (!StringUtils.hasText(userToken)) {
            return;
        }

        if (FirebaseApp.getApps().isEmpty()) {
            log.warn("No se envio push: Firebase Admin no esta inicializado.");
            return;
        }

        String safeTaskName = StringUtils.hasText(taskName) ? taskName.trim() : "Nueva tarea";

        Message message = Message.builder()
                .setToken(userToken.trim())
                .setNotification(Notification.builder()
                        .setTitle("Nueva tarea asignada")
                        .setBody("Se te asigno la tarea: " + safeTaskName)
                        .build())
                .putData("eventType", "TASK_ASSIGNED")
                .putData("taskName", safeTaskName)
                .build();

        try {
            String messageId = FirebaseMessaging.getInstance().send(message);
            log.info("Push enviada correctamente. messageId={}", messageId);
        } catch (Exception exception) {
            log.warn("Fallo al enviar push FCM.", exception);
        }
    }
}
