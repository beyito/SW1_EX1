import 'package:flutter/material.dart';

import '../data/auth_service.dart';
import '../../dashboard/data/dashboard_service.dart';
import '../../dashboard/presentation/dashboard_screen.dart';
import '../../tasks/services/task_service.dart';

class LoginScreen extends StatefulWidget {
  final AuthService authService;
  final DashboardService dashboardService;
  final TaskService taskService;

  const LoginScreen({
    super.key,
    required this.authService,
    required this.dashboardService,
    required this.taskService,
  });

  @override
  State<LoginScreen> createState() => _LoginScreenState();
}

class _LoginScreenState extends State<LoginScreen> {
  final _formKey = GlobalKey<FormState>();
  final _usernameCtrl = TextEditingController();
  final _passwordCtrl = TextEditingController();
  bool _isLoading = false;
  String? _error;

  @override
  void dispose() {
    _usernameCtrl.dispose();
    _passwordCtrl.dispose();
    super.dispose();
  }

  Future<void> _submit() async {
    if (!_formKey.currentState!.validate()) {
      return;
    }

    setState(() {
      _isLoading = true;
      _error = null;
    });

    try {
      await widget.authService.login(
        username: _usernameCtrl.text,
        password: _passwordCtrl.text,
      );

      if (!mounted) return;
      Navigator.of(context).pushReplacement(
        MaterialPageRoute(
          builder: (_) => DashboardScreen(
            authService: widget.authService,
            dashboardService: widget.dashboardService,
            taskService: widget.taskService,
          ),
        ),
      );
    } catch (error) {
      setState(() {
        _error = error.toString().replaceFirst('Exception: ', '');
      });
    } finally {
      if (mounted) {
        setState(() => _isLoading = false);
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Login Cliente')),
      body: SafeArea(
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: Form(
            key: _formKey,
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                TextFormField(
                  controller: _usernameCtrl,
                  decoration: const InputDecoration(labelText: 'Usuario'),
                  validator: (value) => (value == null || value.trim().isEmpty)
                      ? 'Ingresa tu usuario'
                      : null,
                ),
                const SizedBox(height: 12),
                TextFormField(
                  controller: _passwordCtrl,
                  obscureText: true,
                  decoration: const InputDecoration(labelText: 'Contrasena'),
                  validator: (value) => (value == null || value.isEmpty)
                      ? 'Ingresa tu contrasena'
                      : null,
                ),
                const SizedBox(height: 18),
                FilledButton(
                  onPressed: _isLoading ? null : _submit,
                  child: _isLoading
                      ? const SizedBox(
                          width: 18,
                          height: 18,
                          child: CircularProgressIndicator(strokeWidth: 2),
                        )
                      : const Text('Iniciar sesion'),
                ),
                if (_error != null) ...[
                  const SizedBox(height: 12),
                  Text(
                    _error!,
                    style: const TextStyle(color: Colors.redAccent),
                  ),
                ],
              ],
            ),
          ),
        ),
      ),
    );
  }
}
