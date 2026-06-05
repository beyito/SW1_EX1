import 'package:flutter/material.dart';

import '../../auth/data/auth_service.dart';
import '../../auth/presentation/login_screen.dart';
import '../../tasks/screens/pending_tasks_screen.dart';
import '../../tasks/services/task_service.dart';
import '../data/dashboard_service.dart';
import '../models/startable_policy.dart';
import 'start_process_requirements_screen.dart';

class DashboardScreen extends StatefulWidget {
  final AuthService authService;
  final DashboardService dashboardService;
  final TaskService taskService;

  const DashboardScreen({
    super.key,
    required this.authService,
    required this.dashboardService,
    required this.taskService,
  });

  @override
  State<DashboardScreen> createState() => _DashboardScreenState();
}

class _DashboardScreenState extends State<DashboardScreen> {
  late Future<List<StartablePolicy>> _policiesFuture;

  @override
  void initState() {
    super.initState();
    _policiesFuture = widget.dashboardService.getStartablePolicies();
  }

  Future<void> _logout() async {
    await widget.authService.logout();
    if (!mounted) return;
    Navigator.of(context).pushAndRemoveUntil(
      MaterialPageRoute(
        builder: (_) => LoginScreen(
          authService: widget.authService,
          dashboardService: widget.dashboardService,
          taskService: widget.taskService,
        ),
      ),
      (_) => false,
    );
  }

  void _openPendingTasks() {
    Navigator.of(context).push(
      MaterialPageRoute(
        builder: (_) => PendingTasksScreen(taskService: widget.taskService),
      ),
    );
  }

  Future<void> _startPolicy(StartablePolicy policy) async {
    if (policy.id.trim().isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('La política no tiene un identificador válido.')),
      );
      return;
    }

    final started = await Navigator.of(context).push<bool>(
      MaterialPageRoute(
        builder: (_) => StartProcessRequirementsScreen(
          policy: policy,
          dashboardService: widget.dashboardService,
          taskService: widget.taskService,
        ),
      ),
    );

    if (started == true && mounted) {
      Navigator.of(context).push(
        MaterialPageRoute(
          builder: (_) => PendingTasksScreen(
            taskService: widget.taskService,
            initialAutoRetry: true,
          ),
        ),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Trámites Disponibles'),
        actions: [
          IconButton(
            onPressed: _openPendingTasks,
            icon: const Icon(Icons.pending_actions),
            tooltip: 'Mis tareas pendientes',
          ),
          IconButton(
            onPressed: _logout,
            icon: const Icon(Icons.logout),
            tooltip: 'Cerrar sesion',
          ),
        ],
      ),
      body: FutureBuilder<List<StartablePolicy>>(
        future: _policiesFuture,
        builder: (context, snapshot) {
          if (snapshot.connectionState == ConnectionState.waiting) {
            return const Center(child: CircularProgressIndicator());
          }

          if (snapshot.hasError) {
            return Center(
              child: Padding(
                padding: const EdgeInsets.all(16),
                child: Text(
                  snapshot.error.toString().replaceFirst('Exception: ', ''),
                  textAlign: TextAlign.center,
                ),
              ),
            );
          }

          final policies = snapshot.data ?? const [];
          if (policies.isEmpty) {
            return const Center(
              child: Text('No hay trámites disponibles para iniciar.'),
            );
          }

          return RefreshIndicator(
            onRefresh: () async {
              final refreshed = widget.dashboardService.getStartablePolicies();
              setState(() {
                _policiesFuture = refreshed;
              });
              await refreshed;
            },
            child: ListView.builder(
              padding: const EdgeInsets.all(16),
              itemCount: policies.length,
              itemBuilder: (context, index) {
                final policy = policies[index];
                return Card(
                  margin: const EdgeInsets.symmetric(vertical: 8),
                  shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(16),
                    side: BorderSide(
                      color: Theme.of(context).colorScheme.primary.withValues(alpha: 0.28),
                      width: 1.4,
                    ),
                  ),
                  child: Padding(
                    padding: const EdgeInsets.all(12),
                    child: Row(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Container(
                          width: 42,
                          height: 42,
                          decoration: BoxDecoration(
                            color: Theme.of(context).colorScheme.primaryContainer,
                            borderRadius: BorderRadius.circular(12),
                          ),
                          child: Icon(
                            Icons.account_tree_outlined,
                            color: Theme.of(context).colorScheme.onPrimaryContainer,
                          ),
                        ),
                        const SizedBox(width: 12),
                        Expanded(
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Text(
                                policy.name,
                                style: Theme.of(context).textTheme.titleMedium?.copyWith(
                                      fontWeight: FontWeight.w700,
                                    ),
                              ),
                              const SizedBox(height: 4),
                              Text(
                                policy.description.isEmpty ? 'Sin descripción' : policy.description,
                                style: Theme.of(context).textTheme.bodyMedium,
                              ),
                              if (policy.initialRequirements.isNotEmpty) ...[
                                const SizedBox(height: 8),
                                Text(
                                  '${policy.initialRequirements.length} requisitos iniciales',
                                  style: TextStyle(
                                    color: Theme.of(context).colorScheme.primary,
                                    fontWeight: FontWeight.w700,
                                  ),
                                ),
                              ],
                              const SizedBox(height: 10),
                              Align(
                                alignment: Alignment.centerRight,
                                child: FilledButton.icon(
                                  onPressed: () => _startPolicy(policy),
                                  icon: const Icon(Icons.play_arrow_rounded),
                                  label: const Text('Iniciar'),
                                ),
                              )
                            ],
                          ),
                        ),
                      ],
                    ),
                  ),
                );
              },
            ),
          );
        },
      ),
    );
  }
}
