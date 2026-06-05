import 'package:dio/dio.dart';

import '../../../core/network/api_client.dart';
import '../models/startable_policy.dart';
import '../models/process_instance.dart';

class DashboardService {
  final ApiClient _apiClient;

  DashboardService(this._apiClient);

  Future<List<StartablePolicy>> getStartablePolicies() async {
    try {
      final response = await _apiClient.dio.get('/api/execution/startable-policies');
      final payload = response.data;
      if (payload is! List) {
        return const [];
      }

      return payload
          .whereType<Map<String, dynamic>>()
          .map(StartablePolicy.fromJson)
          .toList(growable: false);
    } on DioException catch (error) {
      final backendMessage = error.response?.data is Map
          ? (error.response?.data['message']?.toString() ?? '')
          : '';
      throw Exception(backendMessage.isNotEmpty ? backendMessage : 'No se pudo cargar el dashboard.');
    }
  }

  Future<ProcessInstanceModel> startProcess(String policyId, {required String title, required String description}) async {
    try {
      final response = await _apiClient.dio.post(
        '/api/execution/process/start',
        data: {
          'policyId': policyId,
          'title': title,
          'description': description,
        },
      );
      if (response.data is! Map<String, dynamic>) {
        throw Exception('El backend no devolvio la instancia del tramite.');
      }
      return ProcessInstanceModel.fromJson(response.data as Map<String, dynamic>);
    } on DioException catch (error) {
      final backendMessage = error.response?.data is Map
          ? (error.response?.data['message']?.toString() ?? '')
          : '';
      throw Exception(
        backendMessage.isNotEmpty ? backendMessage : 'No se pudo iniciar el tramite.',
      );
    }
  }
}
