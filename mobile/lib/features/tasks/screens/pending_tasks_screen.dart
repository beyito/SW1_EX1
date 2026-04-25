import 'package:flutter/material.dart';

import '../models/process_task_group_model.dart';
import 'task_detail_screen.dart';
import '../services/task_service.dart';

class PendingTasksScreen extends StatefulWidget {
  final TaskService taskService;

  const PendingTasksScreen({
    super.key,
    required this.taskService,
  });

  @override
  State<PendingTasksScreen> createState() => _PendingTasksScreenState();
}

class _PendingTasksScreenState extends State<PendingTasksScreen> {
  late Future<List<ProcessTaskGroupModel>> _processGroupsFuture;

  @override
  void initState() {
    super.initState();
    _processGroupsFuture = widget.taskService.getMyProcessTaskGroups();
  }

  Future<void> _refreshTasks() async {
    final refreshedFuture = widget.taskService.getMyProcessTaskGroups();
    setState(() {
      _processGroupsFuture = refreshedFuture;
    });
    await refreshedFuture;
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Mis instancias y tareas')),
      body: FutureBuilder<List<ProcessTaskGroupModel>>(
        future: _processGroupsFuture,
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

          final groups = snapshot.data ?? const [];
          if (groups.isEmpty) {
            return RefreshIndicator(
              onRefresh: _refreshTasks,
              child: ListView(
                children: const [
                  SizedBox(height: 200),
                  Center(child: Text('No tienes instancias con tareas por ahora.')),
                ],
              ),
            );
          }

          return RefreshIndicator(
            onRefresh: _refreshTasks,
            child: ListView.builder(
              padding: const EdgeInsets.all(12),
              itemCount: groups.length,
              itemBuilder: (context, index) {
                final group = groups[index];
                final startedAtLabel = _formatDate(group.startedAt);
                return Card(
                  margin: const EdgeInsets.symmetric(vertical: 6),
                  child: ExpansionTile(
                    title: Text(group.processTitle),
                    subtitle: Text(
                      startedAtLabel.isEmpty
                          ? '${group.tasks.length} tareas'
                          : 'Iniciado: $startedAtLabel · ${group.tasks.length} tareas',
                    ),
                    children: [
                      if (group.processDescription.isNotEmpty)
                        Padding(
                          padding: const EdgeInsets.fromLTRB(16, 0, 16, 8),
                          child: Align(
                            alignment: Alignment.centerLeft,
                            child: Text(
                              group.processDescription,
                              style: TextStyle(color: Colors.grey.shade700),
                            ),
                          ),
                        ),
                      for (final task in group.tasks)
                        ListTile(
                          leading: _statusIcon(task.status),
                          title: Text(task.taskName),
                          subtitle: Text(
                            'Estado: ${_statusLabel(task.status)} · Creada: ${_formatDate(task.createdAt)}',
                          ),
                          trailing: const Icon(Icons.chevron_right),
                          onTap: () async {
                            final changed = await Navigator.of(context).push<bool>(
                              MaterialPageRoute(
                                builder: (_) => TaskDetailScreen(
                                  taskService: widget.taskService,
                                  taskInstanceId: task.taskInstanceId,
                                ),
                              ),
                            );
                            if (changed == true && mounted) {
                              await _refreshTasks();
                            }
                          },
                        ),
                    ],
                  ),
                );
              },
            ),
          );
        },
      ),
    );
  }

  String _formatDate(DateTime? date) {
    if (date == null) {
      return 'Fecha no disponible';
    }
    return '${date.day.toString().padLeft(2, '0')}/${date.month.toString().padLeft(2, '0')}/${date.year} ${date.hour.toString().padLeft(2, '0')}:${date.minute.toString().padLeft(2, '0')}';
  }

  String _statusLabel(String status) {
    switch (status) {
      case 'IN_PROGRESS':
        return 'En proceso';
      case 'COMPLETED':
        return 'Hecha';
      case 'REJECTED':
        return 'Rechazada';
      case 'PENDING':
      default:
        return 'Pendiente';
    }
  }

  Widget _statusIcon(String status) {
    switch (status) {
      case 'IN_PROGRESS':
        return const Icon(Icons.timelapse, color: Colors.blue);
      case 'COMPLETED':
        return const Icon(Icons.check_circle, color: Colors.green);
      case 'REJECTED':
        return const Icon(Icons.cancel, color: Colors.red);
      case 'PENDING':
      default:
        return const Icon(Icons.pending_actions, color: Colors.orange);
    }
  }
}
