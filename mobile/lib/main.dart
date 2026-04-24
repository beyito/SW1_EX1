import 'package:flutter/material.dart';

import 'app.dart';
import 'core/config/env.dart';
import 'core/network/api_client.dart';
import 'core/storage/secure_storage_service.dart';
import 'features/auth/data/auth_service.dart';
import 'features/dashboard/data/dashboard_service.dart';

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();
  await Env.load();

  final secureStorageService = const SecureStorageService();
  final apiClient = ApiClient(secureStorageService);
  final authService = AuthService(apiClient, secureStorageService);
  final dashboardService = DashboardService(apiClient);

  runApp(
    MobileApp(
      authService: authService,
      dashboardService: dashboardService,
    ),
  );
}
