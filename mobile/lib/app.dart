import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import 'features/auth/data/auth_service.dart';
import 'features/auth/presentation/login_screen.dart';
import 'features/dashboard/data/dashboard_service.dart';
import 'features/dashboard/presentation/dashboard_screen.dart';
import 'features/tasks/services/task_service.dart';

class MobileApp extends StatelessWidget {
  final AuthService authService;
  final DashboardService dashboardService;
  final TaskService taskService;

  const MobileApp({
    super.key,
    required this.authService,
    required this.dashboardService,
    required this.taskService,
  });

  @override
  Widget build(BuildContext context) {
    const seed = Color(0xFF0F4C81);
    final colorScheme = ColorScheme.fromSeed(seedColor: seed, brightness: Brightness.light);

    return MaterialApp(
      debugShowCheckedModeBanner: false,
      title: 'BPMN Cliente',
      theme: ThemeData(
        useMaterial3: true,
        colorScheme: colorScheme,
        scaffoldBackgroundColor: const Color(0xFFF4F7FB),
        appBarTheme: AppBarTheme(
          centerTitle: false,
          elevation: 0,
          backgroundColor: const Color(0xFFF4F7FB),
          foregroundColor: colorScheme.onSurface,
          surfaceTintColor: Colors.transparent,
          systemOverlayStyle: SystemUiOverlayStyle.dark.copyWith(
            statusBarColor: const Color(0xFFF4F7FB),
            statusBarIconBrightness: Brightness.dark,
            statusBarBrightness: Brightness.light,
          ),
          titleTextStyle: TextStyle(
            color: colorScheme.onSurface,
            fontSize: 20,
            fontWeight: FontWeight.w700,
          ),
        ),
        cardTheme: CardThemeData(
          elevation: 2.5,
          shadowColor: Colors.black.withValues(alpha: 0.12),
          color: Colors.white,
          surfaceTintColor: Colors.white,
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(16),
            side: BorderSide(color: colorScheme.outlineVariant.withValues(alpha: 0.85), width: 1.2),
          ),
          margin: EdgeInsets.zero,
        ),
        inputDecorationTheme: InputDecorationTheme(
          filled: true,
          fillColor: Colors.white,
          border: OutlineInputBorder(
            borderRadius: BorderRadius.circular(12),
            borderSide: BorderSide(color: colorScheme.outlineVariant),
          ),
          enabledBorder: OutlineInputBorder(
            borderRadius: BorderRadius.circular(12),
            borderSide: BorderSide(color: colorScheme.outlineVariant),
          ),
          focusedBorder: OutlineInputBorder(
            borderRadius: BorderRadius.circular(12),
            borderSide: BorderSide(color: colorScheme.primary, width: 1.4),
          ),
          contentPadding: const EdgeInsets.symmetric(horizontal: 14, vertical: 12),
        ),
        filledButtonTheme: FilledButtonThemeData(
          style: FilledButton.styleFrom(
            padding: const EdgeInsets.symmetric(horizontal: 18, vertical: 12),
            shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
            textStyle: const TextStyle(fontWeight: FontWeight.w600),
          ),
        ),
      ),
      home: _AuthGate(
        authService: authService,
        dashboardService: dashboardService,
        taskService: taskService,
      ),
    );
  }
}

class _AuthGate extends StatelessWidget {
  final AuthService authService;
  final DashboardService dashboardService;
  final TaskService taskService;

  const _AuthGate({
    required this.authService,
    required this.dashboardService,
    required this.taskService,
  });

  @override
  Widget build(BuildContext context) {
    return FutureBuilder<bool>(
      future: authService.hasSession(),
      builder: (context, snapshot) {
        if (!snapshot.hasData) {
          return const Scaffold(
            body: Center(child: CircularProgressIndicator()),
          );
        }

        if (snapshot.data == true) {
          return DashboardScreen(
            authService: authService,
            dashboardService: dashboardService,
            taskService: taskService,
          );
        }

        return LoginScreen(
          authService: authService,
          dashboardService: dashboardService,
          taskService: taskService,
        );
      },
    );
  }
}
