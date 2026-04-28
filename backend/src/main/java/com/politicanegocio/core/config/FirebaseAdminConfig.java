package com.politicanegocio.core.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.io.FileInputStream;
import java.io.InputStream;

@Configuration
public class FirebaseAdminConfig {
    private static final Logger log = LoggerFactory.getLogger(FirebaseAdminConfig.class);

    // Simplificado: Que lea directamente la llave del application.properties
    @Value("${app.firebase.credentials-path}")
    private String credentialsPath;

    @PostConstruct
    public void initializeFirebase() {
        System.out.println("\n=======================================================");
        System.out.println("🚀 ARRANCANDO FIREBASE CONFIG");
        System.out.println("📂 RUTA RECIBIDA POR SPRING: '" + credentialsPath + "'");
        System.out.println("=======================================================\n");

        if (!StringUtils.hasText(credentialsPath)) {
            log.warn("FCM deshabilitado: app.firebase.credentials-path no configurado o vacío.");
            return;
        }

        if (!FirebaseApp.getApps().isEmpty()) {
            return;
        }

        try (InputStream credentialsStream = new FileInputStream(credentialsPath.trim())) {
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(credentialsStream))
                    .build();
            FirebaseApp.initializeApp(options);
            log.info("✅ Firebase Admin inicializado correctamente.");
        } catch (Exception exception) {
            log.error("❌ No se pudo inicializar Firebase Admin. El archivo probablemente no existe en la ruta.", exception);
        }
    }
}