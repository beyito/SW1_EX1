import 'dart:convert';

class TaskFieldOptionModel {
  final String label;
  final String value;

  const TaskFieldOptionModel({
    required this.label,
    required this.value,
  });

  factory TaskFieldOptionModel.fromJson(dynamic json) {
    if (json is Map<String, dynamic>) {
      final label = (json['label'] ?? json['value'] ?? '').toString();
      final value = (json['value'] ?? json['label'] ?? '').toString();
      return TaskFieldOptionModel(label: label, value: value);
    }
    final text = (json ?? '').toString();
    return TaskFieldOptionModel(label: text, value: text);
  }
}

class TaskFormFieldModel {
  final String id;
  final String name;
  final String type;
  final String label;
  final bool required;
  final List<TaskFieldOptionModel> options;

  const TaskFormFieldModel({
    required this.id,
    required this.name,
    required this.type,
    required this.label,
    required this.required,
    required this.options,
  });

  factory TaskFormFieldModel.fromJson(Map<String, dynamic> json, int index) {
    final optionsRaw = json['options'];
    final options = optionsRaw is List
        ? optionsRaw.map(TaskFieldOptionModel.fromJson).toList(growable: false)
        : const <TaskFieldOptionModel>[];

    final id = (json['id'] ?? '').toString();
    final name = (json['name'] ?? '').toString();
    final baseKey = name.trim().isNotEmpty
        ? name.trim()
        : (id.trim().isNotEmpty ? id.trim() : 'field_$index');

    return TaskFormFieldModel(
      id: id,
      name: baseKey,
      type: (json['type'] ?? 'text').toString().toLowerCase(),
      label: (json['label'] ?? baseKey).toString(),
      required: json['required'] == true,
      options: options,
    );
  }
}

class TaskDetailModel {
  final String id;
  final String taskName;
  final String processName;
  final String status;
  final String description;
  final List<TaskFormFieldModel> formFields;
  final Map<String, dynamic> initialFormData;

  const TaskDetailModel({
    required this.id,
    required this.taskName,
    required this.processName,
    required this.status,
    required this.description,
    required this.formFields,
    required this.initialFormData,
  });

  factory TaskDetailModel.fromJson(Map<String, dynamic> json) {
    final parsedFields = _parseFields(json['formSchema']);
    final parsedFormData = _parseFormData(json['formData']);

    return TaskDetailModel(
      id: (json['id'] ?? '').toString(),
      taskName: (json['taskName'] ?? 'Tarea').toString(),
      processName: (json['processName'] ?? 'Proceso').toString(),
      status: (json['status'] ?? '').toString(),
      description: (json['description'] ?? '').toString(),
      formFields: parsedFields,
      initialFormData: parsedFormData,
    );
  }

  static List<TaskFormFieldModel> _parseFields(dynamic rawSchema) {
    if (rawSchema == null) {
      return const [];
    }

    dynamic decoded = rawSchema;
    if (decoded is String) {
      final text = decoded.trim();
      if (text.isEmpty) {
        return const [];
      }
      try {
        decoded = jsonDecode(text);
      } catch (_) {
        return const [];
      }
    }

    if (decoded is Map<String, dynamic> && decoded['fields'] is List) {
      decoded = decoded['fields'];
    }

    if (decoded is! List) {
      return const [];
    }

    final result = <TaskFormFieldModel>[];
    for (var i = 0; i < decoded.length; i++) {
      final item = decoded[i];
      if (item is Map<String, dynamic>) {
        result.add(TaskFormFieldModel.fromJson(item, i));
      }
    }
    return result;
  }

  static Map<String, dynamic> _parseFormData(dynamic rawFormData) {
    if (rawFormData == null) {
      return const {};
    }

    dynamic decoded = rawFormData;
    if (decoded is String) {
      final text = decoded.trim();
      if (text.isEmpty) {
        return const {};
      }
      try {
        decoded = jsonDecode(text);
        if (decoded is String) {
          decoded = jsonDecode(decoded);
        }
      } catch (_) {
        return const {};
      }
    }

    if (decoded is Map<String, dynamic>) {
      return decoded;
    }
    return const {};
  }
}
