import 'package:dio/dio.dart';

import '../../../core/network/api_client.dart';
import '../models/startable_policy.dart';

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

  Future<void> startProcess(String policyId) async {
    try {
      await _apiClient.dio.post(
        '/api/execution/process/start',
        data: {'policyId': policyId},
      );
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
