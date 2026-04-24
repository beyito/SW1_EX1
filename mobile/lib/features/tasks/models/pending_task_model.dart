class PendingTaskModel {
  final String taskInstanceId;
  final String processName;
  final String taskName;
  final DateTime? createdAt;

  const PendingTaskModel({
    required this.taskInstanceId,
    required this.processName,
    required this.taskName,
    required this.createdAt,
  });

  factory PendingTaskModel.fromJson(Map<String, dynamic>? json) {
    final map = json ?? const <String, dynamic>{};

    final rawCreatedAt = map['createdAt']?.toString();
    DateTime? parsedCreatedAt;
    if (rawCreatedAt != null && rawCreatedAt.trim().isNotEmpty) {
      parsedCreatedAt = DateTime.tryParse(rawCreatedAt)?.toLocal();
    }

    return PendingTaskModel(
      taskInstanceId: map['taskInstanceId']?.toString() ?? '',
      processName: map['processName']?.toString().trim().isNotEmpty == true
          ? map['processName'].toString().trim()
          : 'Proceso sin nombre',
      taskName: map['taskName']?.toString().trim().isNotEmpty == true
          ? map['taskName'].toString().trim()
          : 'Tarea pendiente',
      createdAt: parsedCreatedAt,
    );
  }
}
