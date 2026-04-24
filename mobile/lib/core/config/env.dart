import 'package:flutter_dotenv/flutter_dotenv.dart';

class Env {
  Env._();

  static Future<void> load() async {
    await dotenv.load(fileName: '.env');
  }

  static String get apiBaseUrl {
    return dotenv.env['API_BASE_URL']?.trim().isNotEmpty == true
        ? dotenv.env['API_BASE_URL']!.trim()
        : 'http://localhost:8080';
  }
}
