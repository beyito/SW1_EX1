import 'package:file_picker/file_picker.dart';
import 'package:flutter/material.dart';

import '../../tasks/services/task_service.dart';
import '../data/dashboard_service.dart';
import '../models/startable_policy.dart';

class StartProcessRequirementsScreen extends StatefulWidget {
  final StartablePolicy policy;
  final DashboardService dashboardService;
  final TaskService taskService;

  const StartProcessRequirementsScreen({
    super.key,
    required this.policy,
    required this.dashboardService,
    required this.taskService,
  });

  @override
  State<StartProcessRequirementsScreen> createState() => _StartProcessRequirementsScreenState();
}

class _StartProcessRequirementsScreenState extends State<StartProcessRequirementsScreen> {
  late final TextEditingController _titleController;
  late final TextEditingController _descriptionController;
  final Map<String, PlatformFile> _selectedFiles = <String, PlatformFile>{};
  bool _isSubmitting = false;
  String? _message;

  @override
  void initState() {
    super.initState();
    _titleController = TextEditingController(text: widget.policy.name);
    _descriptionController = TextEditingController();
  }

  @override
  void dispose() {
    _titleController.dispose();
    _descriptionController.dispose();
    super.dispose();
  }

  Future<void> _pickFile(PolicyInitialRequirement requirement) async {
    final extensions = requirement.allowedExtensions;
    final result = await FilePicker.platform.pickFiles(
      withData: false,
      type: extensions.isEmpty ? FileType.any : FileType.custom,
      allowedExtensions: extensions.isEmpty ? null : extensions,
    );

    if (result == null || result.files.isEmpty) {
      return;
    }

    final file = result.files.first;
    if (file.path == null || file.path!.trim().isEmpty) {
      setState(() => _message = 'No se pudo leer el archivo seleccionado.');
      return;
    }

    setState(() {
      _selectedFiles[requirement.id] = file;
      _message = null;
    });
  }

  Future<void> _startProcess() async {
    if (_isSubmitting) {
      return;
    }

    final title = _titleController.text.trim();
    if (title.isEmpty) {
      setState(() => _message = 'Ingresa un título para el trámite.');
      return;
    }

    final missingRequired = widget.policy.initialRequirements
        .where((requirement) => requirement.required && !_selectedFiles.containsKey(requirement.id))
        .toList(growable: false);

    if (missingRequired.isNotEmpty) {
      setState(() {
        _message = 'Adjunta los requisitos obligatorios: ${missingRequired.map((item) => item.name).join(', ')}.';
      });
      return;
    }

    setState(() {
      _isSubmitting = true;
      _message = 'Creando trámite...';
    });

    try {
      final instance = await widget.dashboardService.startProcess(
        widget.policy.id,
        title: title,
        description: _descriptionController.text.trim(),
      );

      if (instance.id.trim().isEmpty) {
        throw Exception('El backend no devolvió el identificador del trámite.');
      }

      for (final requirement in widget.policy.initialRequirements) {
        final file = _selectedFiles[requirement.id];
        final path = file?.path;
        if (path == null || path.trim().isEmpty) {
          continue;
        }

        if (mounted) {
          setState(() => _message = 'Subiendo ${requirement.name}...');
        }

        await widget.taskService.uploadFileToS3(
          path,
          processInstanceId: instance.id,
          documentId: requirement.id,
        );
      }

      if (!mounted) {
        return;
      }

      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('Trámite iniciado: ${widget.policy.name}')),
      );
      Navigator.of(context).pop(true);
    } catch (error) {
      if (!mounted) {
        return;
      }
      setState(() {
        _message = error.toString().replaceFirst('Exception: ', '');
      });
    } finally {
      if (mounted) {
        setState(() => _isSubmitting = false);
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    final requirements = widget.policy.initialRequirements;

    return Scaffold(
      appBar: AppBar(title: const Text('Iniciar trámite')),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          Card(
            child: Padding(
              padding: const EdgeInsets.all(16),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    widget.policy.name,
                    style: Theme.of(context).textTheme.titleLarge?.copyWith(fontWeight: FontWeight.w800),
                  ),
                  if (widget.policy.description.trim().isNotEmpty) ...[
                    const SizedBox(height: 8),
                    Text(widget.policy.description),
                  ],
                  const SizedBox(height: 16),
                  TextField(
                    controller: _titleController,
                    enabled: !_isSubmitting,
                    maxLength: 120,
                    decoration: const InputDecoration(
                      labelText: 'Título del trámite',
                      hintText: 'Ej. Solicitud de permiso',
                    ),
                  ),
                  const SizedBox(height: 8),
                  TextField(
                    controller: _descriptionController,
                    enabled: !_isSubmitting,
                    maxLines: 3,
                    maxLength: 300,
                    decoration: const InputDecoration(labelText: 'Descripción'),
                  ),
                ],
              ),
            ),
          ),
          const SizedBox(height: 14),
          Card(
            child: Padding(
              padding: const EdgeInsets.all(16),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    'Requisitos iniciales',
                    style: Theme.of(context).textTheme.titleMedium?.copyWith(fontWeight: FontWeight.w800),
                  ),
                  const SizedBox(height: 6),
                  Text(
                    requirements.isEmpty
                        ? 'Este trámite no tiene requisitos iniciales configurados.'
                        : 'Adjunta los archivos solicitados antes de iniciar el trámite.',
                    style: TextStyle(color: Colors.grey.shade700),
                  ),
                  const SizedBox(height: 12),
                  if (requirements.isEmpty)
                    const Text('Puedes iniciar el trámite directamente.')
                  else
                    ...requirements.map((requirement) => Padding(
                          padding: const EdgeInsets.only(bottom: 12),
                          child: _RequirementFileCard(
                            requirement: requirement,
                            file: _selectedFiles[requirement.id],
                            enabled: !_isSubmitting,
                            onPick: () => _pickFile(requirement),
                            onRemove: () {
                              setState(() {
                                _selectedFiles.remove(requirement.id);
                                _message = null;
                              });
                            },
                          ),
                        )),
                ],
              ),
            ),
          ),
          if (_message != null) ...[
            const SizedBox(height: 12),
            Container(
              padding: const EdgeInsets.all(12),
              decoration: BoxDecoration(
                color: _message!.toLowerCase().contains('error')
                    ? scheme.errorContainer
                    : scheme.primaryContainer.withValues(alpha: 0.6),
                borderRadius: BorderRadius.circular(12),
              ),
              child: Text(
                _message!,
                style: TextStyle(
                  color: _message!.toLowerCase().contains('error')
                      ? scheme.onErrorContainer
                      : scheme.onPrimaryContainer,
                ),
              ),
            ),
          ],
          const SizedBox(height: 18),
          FilledButton.icon(
            onPressed: _isSubmitting ? null : _startProcess,
            icon: _isSubmitting
                ? const SizedBox(
                    width: 16,
                    height: 16,
                    child: CircularProgressIndicator(strokeWidth: 2),
                  )
                : const Icon(Icons.send_rounded),
            label: Text(_isSubmitting ? 'Procesando...' : 'Iniciar trámite'),
          ),
        ],
      ),
    );
  }
}

class _RequirementFileCard extends StatelessWidget {
  final PolicyInitialRequirement requirement;
  final PlatformFile? file;
  final bool enabled;
  final VoidCallback onPick;
  final VoidCallback onRemove;

  const _RequirementFileCard({
    required this.requirement,
    required this.file,
    required this.enabled,
    required this.onPick,
    required this.onRemove,
  });

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    final extensions = requirement.allowedExtensions;

    return Container(
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(
        border: Border.all(color: scheme.outlineVariant),
        borderRadius: BorderRadius.circular(14),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Expanded(
                child: Text(
                  requirement.name,
                  style: Theme.of(context).textTheme.titleSmall?.copyWith(fontWeight: FontWeight.w800),
                ),
              ),
              Chip(
                label: Text(requirement.required ? 'Obligatorio' : 'Opcional'),
                visualDensity: VisualDensity.compact,
              ),
            ],
          ),
          if (requirement.description.trim().isNotEmpty) ...[
            const SizedBox(height: 6),
            Text(requirement.description),
          ],
          if (extensions.isNotEmpty) ...[
            const SizedBox(height: 6),
            Text(
              'Formatos permitidos: ${extensions.map((extension) => '.$extension').join(', ')}',
              style: TextStyle(color: Colors.grey.shade700, fontSize: 12),
            ),
          ],
          const SizedBox(height: 10),
          if (file == null)
            OutlinedButton.icon(
              onPressed: enabled ? onPick : null,
              icon: const Icon(Icons.attach_file),
              label: const Text('Adjuntar archivo'),
            )
          else
            Row(
              children: [
                const Icon(Icons.description_outlined),
                const SizedBox(width: 8),
                Expanded(
                  child: Text(
                    file!.name,
                    overflow: TextOverflow.ellipsis,
                  ),
                ),
                IconButton(
                  onPressed: enabled ? onRemove : null,
                  icon: const Icon(Icons.close),
                  tooltip: 'Quitar archivo',
                ),
              ],
            ),
        ],
      ),
    );
  }
}
