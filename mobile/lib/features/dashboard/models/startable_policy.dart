class StartablePolicy {
  final String id;
  final String name;
  final String description;

  const StartablePolicy({
    required this.id,
    required this.name,
    required this.description,
  });

  factory StartablePolicy.fromJson(Map<String, dynamic> json) {
    return StartablePolicy(
      id: (json['id'] ?? '').toString(),
      name: (json['name'] ?? 'Sin nombre').toString(),
      description: (json['description'] ?? '').toString(),
    );
  }
}
