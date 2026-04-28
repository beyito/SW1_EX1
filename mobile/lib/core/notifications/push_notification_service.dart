import 'dart:developer' as developer;

import 'package:firebase_core/firebase_core.dart';
import 'package:firebase_messaging/firebase_messaging.dart';
import 'package:flutter_local_notifications/flutter_local_notifications.dart';

import '../network/api_client.dart';
import '../storage/secure_storage_service.dart';

@pragma('vm:entry-point')
Future<void> firebaseMessagingBackgroundHandler(RemoteMessage message) async {
  await Firebase.initializeApp();
}

class PushNotificationService {
  static const AndroidNotificationChannel _androidChannel = AndroidNotificationChannel(
    'task_assignments_channel',
    'Asignacion de tareas',
    description: 'Notificaciones cuando el sistema asigna nuevas tareas',
    importance: Importance.high,
  );

  final ApiClient _apiClient;
  final SecureStorageService _storageService;
  FirebaseMessaging get _messaging => FirebaseMessaging.instance;
  final FlutterLocalNotificationsPlugin _localNotifications = FlutterLocalNotificationsPlugin();

  bool _initialized = false;

  PushNotificationService(this._apiClient, this._storageService);

  Future<void> initialize() async {
    if (_initialized) {
      return;
    }

    
    FirebaseMessaging.onBackgroundMessage(firebaseMessagingBackgroundHandler);

    await _requestPermissions();
    await _initializeLocalNotifications();
    await _registerMessageHandlers();
    await syncTokenWithBackend();

    _initialized = true;
  }

  Future<void> syncTokenWithBackend() async {
    try {
      final token = await _messaging.getToken();
      if (token == null || token.isEmpty) {
        return;
      }

      final storedToken = await _storageService.getFcmToken();
      if (storedToken == token) {
        return;
      }

      await _apiClient.dio.post(
        '/api/notifications/device-token',
        data: {'token': token},
      );

      await _storageService.saveFcmToken(token);
    } catch (error, stackTrace) {
      developer.log(
        'No se pudo sincronizar el token FCM con backend',
        name: 'push_notification_service',
        error: error,
        stackTrace: stackTrace,
      );
    }
  }

  Future<void> _requestPermissions() async {
    await _messaging.requestPermission(
      alert: true,
      announcement: false,
      badge: true,
      carPlay: false,
      criticalAlert: false,
      provisional: false,
      sound: true,
    );
  }

  Future<void> _initializeLocalNotifications() async {
    const androidSettings = AndroidInitializationSettings('@mipmap/ic_launcher');
    const initializationSettings = InitializationSettings(android: androidSettings);

    await _localNotifications.initialize(initializationSettings);

    final androidPlatform = _localNotifications.resolvePlatformSpecificImplementation<AndroidFlutterLocalNotificationsPlugin>();
    if (androidPlatform != null) {
      await androidPlatform.createNotificationChannel(_androidChannel);
    }
  }

  Future<void> _registerMessageHandlers() async {
    FirebaseMessaging.onMessage.listen(_handleForegroundMessage);

    FirebaseMessaging.onMessageOpenedApp.listen((message) {
      developer.log(
        'Notificacion abierta desde background: ${message.messageId}',
        name: 'push_notification_service',
      );
    });

    final initialMessage = await _messaging.getInitialMessage();
    if (initialMessage != null) {
      developer.log(
        'Notificacion que abrio la app (terminated): ${initialMessage.messageId}',
        name: 'push_notification_service',
      );
    }

    _messaging.onTokenRefresh.listen((token) async {
      try {
        await _apiClient.dio.post(
          '/api/notifications/device-token',
          data: {'token': token},
        );
        await _storageService.saveFcmToken(token);
      } catch (error, stackTrace) {
        developer.log(
          'No se pudo actualizar token FCM refrescado',
          name: 'push_notification_service',
          error: error,
          stackTrace: stackTrace,
        );
      }
    });
  }

  Future<void> _handleForegroundMessage(RemoteMessage message) async {
    final notification = message.notification;
    if (notification == null) {
      return;
    }

    await _localNotifications.show(
      notification.hashCode,
      notification.title ?? 'Nueva notificacion',
      notification.body ?? '',
      NotificationDetails(
        android: AndroidNotificationDetails(
          _androidChannel.id,
          _androidChannel.name,
          channelDescription: _androidChannel.description,
          importance: Importance.high,
          priority: Priority.high,
        ),
      ),
    );
  }
}
