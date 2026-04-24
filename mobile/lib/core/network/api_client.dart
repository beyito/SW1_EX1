import 'package:dio/dio.dart';

import '../config/env.dart';
import '../storage/secure_storage_service.dart';

class ApiClient {
  final Dio dio;

  ApiClient._(this.dio);

  factory ApiClient(SecureStorageService storageService) {
    final dio = Dio(
      BaseOptions(
        baseUrl: Env.apiBaseUrl,
        connectTimeout: const Duration(seconds: 20),
        receiveTimeout: const Duration(seconds: 20),
        headers: {'Content-Type': 'application/json'},
      ),
    );

    dio.interceptors.add(
      QueuedInterceptorsWrapper(
        onRequest: (options, handler) async {
          final token = await storageService.getAuthToken();
          if (token != null && token.isNotEmpty) {
            options.headers['Authorization'] = 'Bearer $token';
          }
          handler.next(options);
        },
      ),
    );

    return ApiClient._(dio);
  }
}
