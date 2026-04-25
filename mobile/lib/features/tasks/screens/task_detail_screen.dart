import 'package:file_picker/file_picker.dart';
import 'package:flutter/material.dart';

import '../models/task_detail_model.dart';
import '../services/task_service.dart';

class TaskDetailScreen extends StatefulWidget {
  final TaskService taskService;
  final String taskInstanceId;

  const TaskDetailScreen({
    super.key,
    required this.taskService,
    required this.taskInstanceId,
  });

  @override
  State<TaskDetailScreen> createState() => _TaskDetailScreenState();
}

class _TaskDetailScreenState extends State<TaskDetailScreen> {
  late Future<TaskDetailModel> _taskDetailFuture;
  final Map<String, dynamic> _formValues = {};
  bool _isSubmitting = false;
  String? _message;

  @override
  void initState() {
    super.initState();
    _taskDetailFuture = _loadTaskDetail();
  }

  Future<TaskDetailModel> _loadTaskDetail() async {
    final detail = await widget.taskService.getTaskDetail(widget.taskInstanceId);
    _formValues
      ..clear()
      ..addAll(detail.initialFormData);
    return detail;
  }

  Future<void> _refreshDetail() async {
    final nextFuture = _loadTaskDetail();
    setState(() {
      _taskDetailFuture = nextFuture;
      _message = null;
    });
    await nextFuture;
  }

  Future<void> _pickAndUploadFile(TaskFormFieldModel field) async {
    final result = await FilePicker.platform.pickFiles(withData: false);
    if (result == null || result.files.isEmpty) {
      return;
    }
    final path = result.files.first.path;
    if (path == null || path.trim().isEmpty) {
      return;
    }

    setState(() => _message = 'Subiendo archivo...');
    try {
      final url = await widget.taskService.uploadFileToS3(path);
      setState(() {
        _formValues[field.name] = url;
        _message = 'Archivo subido correctamente.';
      });
    } catch (error) {
      setState(() {
        _message = error.toString().replaceFirst('Exception: ', '');
      });
    }
  }

  Future<void> _submit(TaskDetailModel detail) async {
    if (_isSubmitting) {
      return;
    }

    for (final field in detail.formFields) {
      if (!field.required) {
        continue;
      }
      final value = _formValues[field.name];
      if (value == null || value.toString().trim().isEmpty) {
        setState(() {
          _message = 'Completa el campo obligatorio: ${field.label}';
        });
        return;
      }
    }

    setState(() {
      _isSubmitting = true;
      _message = null;
    });

    try {
      if (detail.status == 'PENDING') {
        await widget.taskService.takeTask(detail.id);
      }
      await widget.taskService.completeTask(detail.id, Map<String, dynamic>.from(_formValues));
      if (!mounted) {
        return;
      }
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Tarea completada con exito.')),
      );
      Navigator.of(context).pop(true);
    } catch (error) {
      setState(() {
        _message = error.toString().replaceFirst('Exception: ', '');
      });
    } finally {
      if (mounted) {
        setState(() {
          _isSubmitting = false;
        });
      }
    }
  }

  Widget _buildField(TaskFormFieldModel field) {
    final currentValue = _formValues[field.name];

    if (field.type == 'file') {
      return Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          FilledButton.tonalIcon(
            onPressed: _isSubmitting ? null : () => _pickAndUploadFile(field),
            icon: const Icon(Icons.upload_file),
            label: Text('Subir archivo (${field.label})'),
          ),
          if (currentValue != null && currentValue.toString().trim().isNotEmpty)
            Padding(
              padding: const EdgeInsets.only(top: 8),
              child: Text(
                currentValue.toString(),
                style: TextStyle(color: Colors.grey.shade700, fontSize: 12),
              ),
            ),
        ],
      );
    }

    if (field.type == 'select' && field.options.isNotEmpty) {
      return DropdownButtonFormField<String>(
        initialValue: currentValue?.toString().isNotEmpty == true ? currentValue.toString() : null,
        items: field.options
            .map((option) => DropdownMenuItem<String>(
                  value: option.value,
                  child: Text(option.label),
                ))
            .toList(growable: false),
        onChanged: _isSubmitting
            ? null
            : (value) {
                setState(() {
                  _formValues[field.name] = value ?? '';
                });
              },
        decoration: InputDecoration(labelText: field.label),
      );
    }

    return TextFormField(
      enabled: !_isSubmitting,
      initialValue: currentValue?.toString() ?? '',
      maxLines: field.type == 'textarea' ? 4 : 1,
      keyboardType: field.type == 'number' ? TextInputType.number : TextInputType.text,
      onChanged: (value) => _formValues[field.name] = value,
      decoration: InputDecoration(
        labelText: field.required ? '${field.label} *' : field.label,
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Detalle de tarea')),
      body: FutureBuilder<TaskDetailModel>(
        future: _taskDetailFuture,
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

          final detail = snapshot.data;
          if (detail == null) {
            return const Center(child: Text('No se pudo cargar el detalle de la tarea.'));
          }

          return RefreshIndicator(
            onRefresh: _refreshDetail,
            child: ListView(
              padding: const EdgeInsets.all(16),
              children: [
                Text(detail.processName, style: Theme.of(context).textTheme.titleLarge),
                const SizedBox(height: 6),
                Text(detail.taskName, style: Theme.of(context).textTheme.titleMedium),
                if (detail.description.trim().isNotEmpty) ...[
                  const SizedBox(height: 10),
                  Text(detail.description),
                ],
                const SizedBox(height: 16),
                if (detail.formFields.isEmpty)
                  const Text('Esta tarea no tiene formulario, puedes completarla directamente.')
                else
                  ...detail.formFields.map((field) => Padding(
                        padding: const EdgeInsets.only(bottom: 12),
                        child: _buildField(field),
                      )),
                if (_message != null) ...[
                  const SizedBox(height: 8),
                  Text(
                    _message!,
                    style: TextStyle(
                      color: _message!.toLowerCase().contains('error') ? Colors.red : Colors.blueGrey,
                    ),
                  ),
                ],
                const SizedBox(height: 16),
                FilledButton.icon(
                  onPressed: _isSubmitting ? null : () => _submit(detail),
                  icon: _isSubmitting
                      ? const SizedBox(
                          width: 16,
                          height: 16,
                          child: CircularProgressIndicator(strokeWidth: 2),
                        )
                      : const Icon(Icons.check_circle_outline),
                  label: Text(_isSubmitting ? 'Completando...' : 'Completar tarea'),
                ),
              ],
            ),
          );
        },
      ),
    );
  }
}
