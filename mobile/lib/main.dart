import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import 'app.dart';
import 'core/config/env.dart';
import 'core/notifications/push_notification_service.dart';
import 'core/network/api_client.dart';
import 'core/storage/secure_storage_service.dart';
import 'features/auth/data/auth_service.dart';
import 'features/dashboard/data/dashboard_service.dart';
import 'features/tasks/services/task_service.dart';
import 'package:firebase_core/firebase_core.dart';

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();
  SystemChrome.setSystemUIOverlayStyle(
    const SystemUiOverlayStyle(
      statusBarColor: Color(0xFFF4F7FB),
      statusBarIconBrightness: Brightness.dark,
      statusBarBrightness: Brightness.light,
      systemNavigationBarColor: Colors.white,
      systemNavigationBarIconBrightness: Brightness.dark,
    ),
  );
  await Env.load();
  await Firebase.initializeApp();

  final secureStorageService = const SecureStorageService();
  final apiClient = ApiClient(secureStorageService);
  final pushNotificationService = PushNotificationService(apiClient, secureStorageService);
  await pushNotificationService.initialize();

  final authService = AuthService(apiClient, secureStorageService, pushNotificationService);
  final dashboardService = DashboardService(apiClient);
  final taskService = TaskService(apiClient);

  runApp(
    MobileApp(
      authService: authService,
      dashboardService: dashboardService,
      taskService: taskService,
    ),
  );
}
