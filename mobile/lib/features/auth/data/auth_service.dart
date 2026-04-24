import 'package:dio/dio.dart';

import '../../../core/network/api_client.dart';
import '../../../core/storage/secure_storage_service.dart';

class AuthService {
  final ApiClient _apiClient;
  final SecureStorageService _storageService;

  AuthService(this._apiClient, this._storageService);

  Future<void> login({required String username, required String password}) async {
    try {
      final response = await _apiClient.dio.post(
        '/api/auth/login',
        data: {'username': username.trim(), 'password': password},
      );

      final token = response.data['token'] as String?;
      if (token == null || token.isEmpty) {
        throw Exception('El backend no devolvio token.');
      }

      await _storageService.saveAuthToken(token);
    } on DioException catch (error) {
      final backendMessage = error.response?.data is Map
          ? (error.response?.data['message']?.toString() ?? '')
          : '';
      throw Exception(backendMessage.isNotEmpty ? backendMessage : 'No se pudo iniciar sesion.');
    }
  }

  Future<bool> hasSession() async {
    final token = await _storageService.getAuthToken();
    return token != null && token.isNotEmpty;
  }

  Future<void> logout() async {
    await _storageService.clearAuthToken();
  }
}
