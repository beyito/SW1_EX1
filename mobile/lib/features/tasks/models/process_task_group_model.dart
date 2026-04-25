class ProcessTaskModel {
  final String taskInstanceId;
  final String taskId;
  final String taskName;
  final String laneId;
  final String status;
  final DateTime? createdAt;
  final DateTime? completedAt;

  const ProcessTaskModel({
    required this.taskInstanceId,
    required this.taskId,
    required this.taskName,
    required this.laneId,
    required this.status,
    required this.createdAt,
    required this.completedAt,
  });

  factory ProcessTaskModel.fromJson(Map<String, dynamic>? json) {
    final map = json ?? const <String, dynamic>{};
    return ProcessTaskModel(
      taskInstanceId: (map['taskInstanceId'] ?? '').toString(),
      taskId: (map['taskId'] ?? '').toString(),
      taskName: (map['taskName'] ?? '').toString().trim().isNotEmpty
          ? map['taskName'].toString().trim()
          : 'Tarea',
      laneId: (map['laneId'] ?? '').toString(),
      status: (map['status'] ?? 'PENDING').toString(),
      createdAt: _parseDate(map['createdAt']),
      completedAt: _parseDate(map['completedAt']),
    );
  }

  static DateTime? _parseDate(dynamic value) {
    final raw = value?.toString().trim();
    if (raw == null || raw.isEmpty) {
      return null;
    }
    return DateTime.tryParse(raw)?.toLocal();
  }
}

class ProcessTaskGroupModel {
  final String processInstanceId;
  final String policyId;
  final String processTitle;
  final String processDescription;
  final String processStatus;
  final DateTime? startedAt;
  final DateTime? completedAt;
  final List<ProcessTaskModel> tasks;

  const ProcessTaskGroupModel({
    required this.processInstanceId,
    required this.policyId,
    required this.processTitle,
    required this.processDescription,
    required this.processStatus,
    required this.startedAt,
    required this.completedAt,
    required this.tasks,
  });

  factory ProcessTaskGroupModel.fromJson(Map<String, dynamic>? json) {
    final map = json ?? const <String, dynamic>{};
    final rawTasks = map['tasks'];
    final tasks = rawTasks is List
        ? rawTasks
            .whereType<Map<String, dynamic>>()
            .map(ProcessTaskModel.fromJson)
            .toList(growable: false)
        : const <ProcessTaskModel>[];

    return ProcessTaskGroupModel(
      processInstanceId: (map['processInstanceId'] ?? '').toString(),
      policyId: (map['policyId'] ?? '').toString(),
      processTitle: (map['processTitle'] ?? '').toString().trim().isNotEmpty
          ? map['processTitle'].toString().trim()
          : 'Proceso',
      processDescription: (map['processDescription'] ?? '').toString().trim(),
      processStatus: (map['processStatus'] ?? 'ACTIVE').toString(),
      startedAt: ProcessTaskModel._parseDate(map['startedAt']),
      completedAt: ProcessTaskModel._parseDate(map['completedAt']),
      tasks: tasks,
    );
  }
}
