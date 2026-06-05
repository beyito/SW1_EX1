import 'package:flutter/material.dart';

import '../../auth/data/auth_service.dart';
import '../../auth/presentation/login_screen.dart';
import '../../tasks/screens/pending_tasks_screen.dart';
import '../../tasks/services/task_service.dart';
import '../data/dashboard_service.dart';
import '../models/intelligent_reception_result.dart';
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
  final TextEditingController _agentTextController = TextEditingController();
  bool _agentSubmitting = false;
  String? _agentMessage;

  @override
  void initState() {
    super.initState();
    _policiesFuture = widget.dashboardService.getStartablePolicies();
  }

  @override
  void dispose() {
    _agentTextController.dispose();
    super.dispose();
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

  Future<void> _startWithAgent(List<StartablePolicy> policies) async {
    if (_agentSubmitting) {
      return;
    }

    final text = _agentTextController.text.trim();
    if (text.isEmpty) {
      setState(() => _agentMessage = 'Cuentanos que necesitas para que el agente encuentre tramites candidatos.');
      return;
    }

    setState(() {
      _agentSubmitting = true;
      _agentMessage = 'Analizando tu solicitud...';
    });

    try {
      final result = await widget.dashboardService.recommendPoliciesWithAgent(text);
      if (!mounted) {
        return;
      }

      if (result.candidates.isEmpty) {
        setState(() => _agentMessage = 'El agente no encontro tramites candidatos. Intenta describirlo con mas detalle.');
        return;
      }

      final selectedPolicy = await _showAgentCandidates(result.candidates, policies);

      if (!mounted || selectedPolicy == null) {
        return;
      }
      _agentTextController.clear();
      setState(() => _agentMessage = 'Seleccionaste ${selectedPolicy.name}. Completa sus requisitos para iniciar.');
      await _startPolicy(selectedPolicy);
    } catch (error) {
      if (!mounted) {
        return;
      }
      setState(() {
        _agentMessage = error.toString().replaceFirst('Exception: ', '');
      });
    } finally {
      if (mounted) {
        setState(() => _agentSubmitting = false);
      }
    }
  }

  Future<StartablePolicy?> _showAgentCandidates(
    List<IntelligentReceptionCandidate> candidates,
    List<StartablePolicy> policies,
  ) {
    final policyById = {for (final policy in policies) policy.id: policy};

    return showModalBottomSheet<StartablePolicy>(
      context: context,
      showDragHandle: true,
      builder: (sheetContext) {
        return SafeArea(
          child: ListView(
            padding: const EdgeInsets.fromLTRB(16, 8, 16, 20),
            shrinkWrap: true,
            children: [
              Text(
                'Tramites recomendados',
                style: Theme.of(sheetContext).textTheme.titleLarge?.copyWith(fontWeight: FontWeight.w800),
              ),
              const SizedBox(height: 8),
              const Text('Elige el tramite que veas mas conveniente. Luego podras adjuntar sus requisitos iniciales.'),
              const SizedBox(height: 12),
              ...candidates.map((candidate) {
                final policy = policyById[candidate.policyId];
                return Card(
                  margin: const EdgeInsets.only(bottom: 10),
                  child: ListTile(
                    title: Text(candidate.policyName),
                    subtitle: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text('Confianza: ${(candidate.confidence * 100).toStringAsFixed(0)}%'),
                        if (candidate.reason.trim().isNotEmpty) Text(candidate.reason),
                        if (candidate.missingRequirements.isNotEmpty)
                          Text('Requisitos: ${candidate.missingRequirements.join(', ')}'),
                      ],
                    ),
                    trailing: const Icon(Icons.chevron_right),
                    enabled: policy != null,
                    onTap: policy == null ? null : () => Navigator.of(sheetContext).pop(policy),
                  ),
                );
              }),
            ],
          ),
        );
      },
    );
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
              itemCount: policies.length + 1,
              itemBuilder: (context, index) {
                if (index == 0) {
                  return _buildAgentCard(context, policies);
                }

                final policyIndex = index - 1;
                final policy = policies[policyIndex];
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

  Widget _buildAgentCard(BuildContext context, List<StartablePolicy> policies) {
    final scheme = Theme.of(context).colorScheme;

    return Card(
      margin: const EdgeInsets.only(bottom: 14),
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Icon(Icons.auto_awesome, color: scheme.primary),
                const SizedBox(width: 8),
                Expanded(
                  child: Text(
                    'Agente de recepción inteligente',
                    style: Theme.of(context).textTheme.titleMedium?.copyWith(fontWeight: FontWeight.w800),
                  ),
                ),
              ],
            ),
            const SizedBox(height: 8),
            Text(
              'Escribe tu necesidad o dicta usando el microfono del teclado. El agente te mostrara tramites candidatos para que elijas uno.',
              style: TextStyle(color: Colors.grey.shade700),
            ),
            const SizedBox(height: 12),
            TextField(
              controller: _agentTextController,
              enabled: !_agentSubmitting,
              minLines: 2,
              maxLines: 4,
              decoration: const InputDecoration(
                labelText: 'Que necesitas?',
                hintText: 'Ej. Perdi mi tarjeta y necesito una nueva',
              ),
            ),
            if (_agentMessage != null) ...[
              const SizedBox(height: 10),
              Container(
                width: double.infinity,
                padding: const EdgeInsets.all(10),
                decoration: BoxDecoration(
                  color: scheme.primaryContainer.withValues(alpha: 0.55),
                  borderRadius: BorderRadius.circular(12),
                ),
                child: Text(_agentMessage!, style: TextStyle(color: scheme.onPrimaryContainer)),
              ),
            ],
            const SizedBox(height: 12),
            Align(
              alignment: Alignment.centerRight,
              child: FilledButton.icon(
                onPressed: _agentSubmitting ? null : () => _startWithAgent(policies),
                icon: _agentSubmitting
                    ? const SizedBox(
                        width: 16,
                        height: 16,
                        child: CircularProgressIndicator(strokeWidth: 2),
                      )
                    : const Icon(Icons.send_rounded),
                label: Text(_agentSubmitting ? 'Analizando...' : 'Buscar tramites'),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
