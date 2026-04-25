import 'package:flutter/material.dart';

import '../../auth/data/auth_service.dart';
import '../../auth/presentation/login_screen.dart';
import '../../tasks/screens/pending_tasks_screen.dart';
import '../../tasks/services/task_service.dart';
import '../data/dashboard_service.dart';
import '../models/startable_policy.dart';

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
  final Set<String> _startingPolicyIds = <String>{};

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
        const SnackBar(content: Text('La politica no tiene un identificador valido.')),
      );
      return;
    }

    setState(() {
      _startingPolicyIds.add(policy.id);
    });

    try {
      final processMetadata = await _askForProcessMetadata(policy);
      if (processMetadata == null) {
        return;
      }

      await widget.dashboardService.startProcess(
        policy.id,
        title: processMetadata.title,
        description: processMetadata.description,
      );
      if (!mounted) return;

      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('Tramite iniciado: ${policy.name}')),
      );

      Navigator.of(context).push(
        MaterialPageRoute(
          builder: (_) => PendingTasksScreen(taskService: widget.taskService),
        ),
      );
    } catch (error) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(error.toString().replaceFirst('Exception: ', '')),
        ),
      );
    } finally {
      if (mounted) {
        setState(() {
          _startingPolicyIds.remove(policy.id);
        });
      }
    }
  }

  Future<_ProcessMetadata?> _askForProcessMetadata(StartablePolicy policy) async {
    final titleController = TextEditingController(text: policy.name);
    final descriptionController = TextEditingController();

    final result = await showDialog<_ProcessMetadata>(
      context: context,
      builder: (dialogContext) {
        return AlertDialog(
          title: const Text('Nueva instancia'),
          content: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              TextField(
                controller: titleController,
                maxLength: 120,
                decoration: const InputDecoration(
                  labelText: 'Titulo',
                  hintText: 'Ej. Solicitud de compra abril',
                ),
              ),
              const SizedBox(height: 8),
              TextField(
                controller: descriptionController,
                maxLines: 3,
                maxLength: 300,
                decoration: const InputDecoration(
                  labelText: 'Descripcion',
                ),
              ),
            ],
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.of(dialogContext).pop(),
              child: const Text('Cancelar'),
            ),
            FilledButton(
              onPressed: () {
                final title = titleController.text.trim();
                if (title.isEmpty) {
                  return;
                }
                Navigator.of(dialogContext).pop(
                  _ProcessMetadata(
                    title: title,
                    description: descriptionController.text.trim(),
                  ),
                );
              },
              child: const Text('Iniciar'),
            ),
          ],
        );
      },
    );

    titleController.dispose();
    descriptionController.dispose();
    return result;
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Tramites Disponibles'),
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
              child: Text('No hay tramites disponibles para iniciar.'),
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
              padding: const EdgeInsets.all(12),
              itemCount: policies.length,
              itemBuilder: (context, index) {
                final policy = policies[index];
                final isStarting = _startingPolicyIds.contains(policy.id);
                return Card(
                  margin: const EdgeInsets.symmetric(vertical: 6),
                  child: ListTile(
                    title: Text(policy.name),
                    subtitle: Text(
                      policy.description.isEmpty
                          ? 'Sin descripcion'
                          : policy.description,
                    ),
                    trailing: isStarting
                        ? const SizedBox(
                            width: 20,
                            height: 20,
                            child: CircularProgressIndicator(strokeWidth: 2),
                          )
                        : const Icon(Icons.play_circle_outline),
                    onTap: isStarting ? null : () => _startPolicy(policy),
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

class _ProcessMetadata {
  final String title;
  final String description;

  const _ProcessMetadata({
    required this.title,
    required this.description,
  });
}
