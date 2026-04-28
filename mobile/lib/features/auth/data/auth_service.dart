import 'package:dio/dio.dart';

import '../../../core/notifications/push_notification_service.dart';
import '../../../core/network/api_client.dart';
import '../../../core/storage/secure_storage_service.dart';

class AuthService {
  final ApiClient _apiClient;
  final SecureStorageService _storageService;
  final PushNotificationService _pushNotificationService;

  AuthService(this._apiClient, this._storageService, this._pushNotificationService);

  Future<void> login({required String username, required String password}) async {
    try {
      final response = await _apiClient.dio.post(
        '/api/auth/mobile/login',
        data: {'username': username.trim(), 'password': password},
      );

      final token = response.data['token'] as String?;
      if (token == null || token.isEmpty) {
        throw Exception('El backend no devolvio token.');
      }

      await _storageService.saveAuthToken(token);
      await _pushNotificationService.syncTokenWithBackend();
    } on DioException catch (error) {
      throw Exception(_mapDioError(error));
    } catch (_) {
      throw Exception('No se pudo iniciar sesion. Intenta nuevamente.');
    }
  }

  Future<bool> hasSession() async {
    final token = await _storageService.getAuthToken();
    return token != null && token.isNotEmpty;
  }

  Future<void> logout() async {
    await _storageService.clearAuthToken();
    await _storageService.clearFcmToken();
  }

  String _mapDioError(DioException error) {
    if (error.type == DioExceptionType.connectionTimeout ||
        error.type == DioExceptionType.receiveTimeout ||
        error.type == DioExceptionType.sendTimeout) {
      return 'La conexion tardo demasiado. Verifica tu internet e intenta nuevamente.';
    }

    if (error.type == DioExceptionType.connectionError) {
      return 'No se pudo conectar con el servidor.';
    }

    final data = error.response?.data;
    if (data is Map<String, dynamic>) {
      final message = data['message']?.toString().trim();
      final errorText = data['error']?.toString().trim();
      if (message != null && message.isNotEmpty) {
        return message;
      }
      if (errorText != null && errorText.isNotEmpty) {
        return errorText;
      }
    }

    if (data is String && data.trim().isNotEmpty) {
      return data;
    }

    if (error.response?.statusCode == 401) {
      return 'Credenciales invalidas o usuario no autorizado para la app movil.';
    }

    return 'No se pudo iniciar sesion.';
  }
}
