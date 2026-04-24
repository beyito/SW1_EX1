import 'package:flutter/material.dart';

import '../../auth/data/auth_service.dart';
import '../../auth/presentation/login_screen.dart';
import '../data/dashboard_service.dart';
import '../models/startable_policy.dart';

class DashboardScreen extends StatefulWidget {
  final AuthService authService;
  final DashboardService dashboardService;

  const DashboardScreen({
    super.key,
    required this.authService,
    required this.dashboardService,
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
        ),
      ),
      (_) => false,
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Tramites Disponibles'),
        actions: [
          IconButton(
            onPressed: _logout,
            icon: const Icon(Icons.logout),
            tooltip: 'Cerrar sesion',
          )
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
                return Card(
                  margin: const EdgeInsets.symmetric(vertical: 6),
                  child: ListTile(
                    title: Text(policy.name),
                    subtitle: Text(
                      policy.description.isEmpty
                          ? 'Sin descripcion'
                          : policy.description,
                    ),
                    trailing: const Icon(Icons.chevron_right),
                    onTap: () {
                      ScaffoldMessenger.of(context).showSnackBar(
                        SnackBar(
                          content: Text('Iniciar tramite: ${policy.name}'),
                        ),
                      );
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
