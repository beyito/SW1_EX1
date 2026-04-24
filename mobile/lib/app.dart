import 'package:flutter/material.dart';

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
    return MaterialApp(
      debugShowCheckedModeBanner: false,
      title: 'BPMN Cliente',
      theme: ThemeData(
        colorScheme: ColorScheme.fromSeed(seedColor: Colors.indigo),
        useMaterial3: true,
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
