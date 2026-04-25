import 'dart:convert';

import 'package:dio/dio.dart';
import 'package:dio/dio.dart' as dio;

import '../../../core/network/api_client.dart';
import '../models/pending_task_model.dart';
import '../models/process_task_group_model.dart';
import '../models/task_detail_model.dart';

class TaskService {
  final ApiClient _apiClient;

  TaskService(this._apiClient);

  Future<List<PendingTaskModel>> getMyPendingTasks() async {
    try {
      final response = await _apiClient.dio.get('/api/execution/my-tasks');
      final payload = response.data;

      if (payload is! List) {
        return const [];
      }

      return payload
          .whereType<Map<String, dynamic>>()
          .map(PendingTaskModel.fromJson)
          .toList(growable: false);
    } on DioException catch (error) {
      final data = error.response?.data;
      if (data is Map<String, dynamic>) {
        final message = data['message']?.toString().trim();
        if (message != null && message.isNotEmpty) {
          throw Exception(message);
        }
      }
      if (data is String && data.trim().isNotEmpty) {
        throw Exception(data.trim());
      }
      throw Exception('No se pudieron cargar tus tareas pendientes.');
    }
  }

  Future<List<ProcessTaskGroupModel>> getMyProcessTaskGroups() async {
    try {
      final response = await _apiClient.dio.get('/api/execution/my-processes/tasks');
      final payload = response.data;
      if (payload is! List) {
        return const [];
      }
      return payload
          .whereType<Map<String, dynamic>>()
          .map(ProcessTaskGroupModel.fromJson)
          .toList(growable: false);
    } on DioException catch (error) {
      final data = error.response?.data;
      if (data is Map<String, dynamic>) {
        final message = data['message']?.toString().trim();
        if (message != null && message.isNotEmpty) {
          throw Exception(message);
        }
      }
      if (data is String && data.trim().isNotEmpty) {
        throw Exception(data.trim());
      }
      throw Exception('No se pudieron cargar tus instancias y tareas.');
    }
  }

  Future<TaskDetailModel> getTaskDetail(String taskInstanceId) async {
    try {
      final response = await _apiClient.dio.get('/api/execution/tasks/$taskInstanceId');
      if (response.data is! Map<String, dynamic>) {
        throw Exception('Respuesta invalida del detalle de tarea.');
      }
      return TaskDetailModel.fromJson(response.data as Map<String, dynamic>);
    } on DioException catch (error) {
      throw Exception(_extractBackendMessage(error, fallback: 'No se pudo cargar el detalle de la tarea.'));
    }
  }

  Future<void> takeTask(String taskInstanceId) async {
    try {
      await _apiClient.dio.post('/api/execution/tasks/$taskInstanceId/take');
    } on DioException catch (error) {
      throw Exception(_extractBackendMessage(error, fallback: 'No se pudo tomar la tarea.'));
    }
  }

  Future<void> completeTask(String taskInstanceId, Map<String, dynamic> formData) async {
    try {
      await _apiClient.dio.post(
        '/api/execution/tasks/$taskInstanceId/complete',
        data: {'formData': jsonEncode(formData)},
      );
    } on DioException catch (error) {
      throw Exception(_extractBackendMessage(error, fallback: 'No se pudo completar la tarea.'));
    }
  }

  Future<String> uploadFileToS3(String filePath, {String? policyId}) async {
    try {
      final filename = filePath.split(RegExp(r'[\\/]')).last;
      final formData = FormData.fromMap({
        'file': await dio.MultipartFile.fromFile(filePath, filename: filename),
        if (policyId != null && policyId.trim().isNotEmpty) 'policyId': policyId.trim(),
      });

      final response = await _apiClient.dio.post('/api/files/upload', data: formData);
      if (response.data is Map<String, dynamic>) {
        final map = response.data as Map<String, dynamic>;
        final url = (map['url'] ?? map['fileUrl'] ?? '').toString();
        if (url.trim().isNotEmpty) {
          return url.trim();
        }
      }
      throw Exception('La subida no devolvio URL del archivo.');
    } on DioException catch (error) {
      throw Exception(_extractBackendMessage(error, fallback: 'No se pudo subir el archivo a S3.'));
    }
  }

  String _extractBackendMessage(DioException error, {required String fallback}) {
    final data = error.response?.data;
    if (data is Map<String, dynamic>) {
      final message = data['message']?.toString().trim();
      if (message != null && message.isNotEmpty) {
        return message;
      }
      final err = data['error']?.toString().trim();
      if (err != null && err.isNotEmpty) {
        return err;
      }
    }
    if (data is String && data.trim().isNotEmpty) {
      return data.trim();
    }
    return fallback;
  }
}
