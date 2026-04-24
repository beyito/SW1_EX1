import 'package:flutter/material.dart';

import '../models/pending_task_model.dart';
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
  late Future<List<PendingTaskModel>> _pendingTasksFuture;

  @override
  void initState() {
    super.initState();
    _pendingTasksFuture = widget.taskService.getMyPendingTasks();
  }

  Future<void> _refreshTasks() async {
    final refreshedFuture = widget.taskService.getMyPendingTasks();
    setState(() {
      _pendingTasksFuture = refreshedFuture;
    });
    await refreshedFuture;
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Mis tareas pendientes')),
      body: FutureBuilder<List<PendingTaskModel>>(
        future: _pendingTasksFuture,
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

          final tasks = snapshot.data ?? const [];
          if (tasks.isEmpty) {
            return RefreshIndicator(
              onRefresh: _refreshTasks,
              child: ListView(
                children: const [
                  SizedBox(height: 200),
                  Center(child: Text('No tienes tareas pendientes por ahora.')),
                ],
              ),
            );
          }

          return RefreshIndicator(
            onRefresh: _refreshTasks,
            child: ListView.builder(
              padding: const EdgeInsets.all(12),
              itemCount: tasks.length,
              itemBuilder: (context, index) {
                final task = tasks[index];
                final createdAtLabel = task.createdAt != null
                    ? '${task.createdAt!.day.toString().padLeft(2, '0')}/${task.createdAt!.month.toString().padLeft(2, '0')}/${task.createdAt!.year} ${task.createdAt!.hour.toString().padLeft(2, '0')}:${task.createdAt!.minute.toString().padLeft(2, '0')}'
                    : 'Fecha no disponible';

                return Card(
                  elevation: 1.5,
                  margin: const EdgeInsets.symmetric(vertical: 6),
                  child: ListTile(
                    contentPadding: const EdgeInsets.symmetric(horizontal: 14, vertical: 8),
                    title: Text(
                      task.processName,
                      style: const TextStyle(fontWeight: FontWeight.w600),
                    ),
                    subtitle: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        const SizedBox(height: 4),
                        Text(task.taskName),
                        const SizedBox(height: 4),
                        Text(
                          'Creada: $createdAtLabel',
                          style: TextStyle(
                            fontSize: 12,
                            color: Colors.grey.shade600,
                          ),
                        ),
                      ],
                    ),
                    trailing: const Icon(Icons.assignment_turned_in_outlined),
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
                );
              },
            ),
          );
        },
      ),
    );
  }
}
