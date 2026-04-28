import 'package:flutter_secure_storage/flutter_secure_storage.dart';

class SecureStorageService {
  static const _tokenKey = 'auth_token';
  static const _fcmTokenKey = 'fcm_token';
  final FlutterSecureStorage _storage;

  const SecureStorageService({FlutterSecureStorage? storage})
      : _storage = storage ?? const FlutterSecureStorage();

  Future<void> saveAuthToken(String token) async {
    await _storage.write(key: _tokenKey, value: token);
  }

  Future<String?> getAuthToken() async {
    return _storage.read(key: _tokenKey);
  }

  Future<void> clearAuthToken() async {
    await _storage.delete(key: _tokenKey);
  }

  Future<void> saveFcmToken(String token) async {
    await _storage.write(key: _fcmTokenKey, value: token);
  }

  Future<String?> getFcmToken() async {
    return _storage.read(key: _fcmTokenKey);
  }

  Future<void> clearFcmToken() async {
    await _storage.delete(key: _fcmTokenKey);
  }
}
