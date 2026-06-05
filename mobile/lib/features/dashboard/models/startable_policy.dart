class StartablePolicy {
  final String id;
  final String name;
  final String description;
  final List<PolicyInitialRequirement> initialRequirements;

  const StartablePolicy({
    required this.id,
    required this.name,
    required this.description,
    required this.initialRequirements,
  });

  factory StartablePolicy.fromJson(Map<String, dynamic> json) {
    final requirementsPayload = json['initialRequirements'];
    return StartablePolicy(
      id: (json['id'] ?? '').toString(),
      name: (json['name'] ?? 'Sin nombre').toString(),
      description: (json['description'] ?? '').toString(),
      initialRequirements: requirementsPayload is List
          ? requirementsPayload
              .whereType<Map<String, dynamic>>()
              .map(PolicyInitialRequirement.fromJson)
              .where((requirement) => requirement.id.isNotEmpty)
              .toList(growable: false)
          : const [],
    );
  }
}

class PolicyInitialRequirement {
  final String id;
  final String name;
  final String description;
  final bool required;
  final List<String> allowedExtensions;

  const PolicyInitialRequirement({
    required this.id,
    required this.name,
    required this.description,
    required this.required,
    required this.allowedExtensions,
  });

  factory PolicyInitialRequirement.fromJson(Map<String, dynamic> json) {
    final extensionsPayload = json['allowedExtensions'];
    return PolicyInitialRequirement(
      id: (json['id'] ?? '').toString(),
      name: (json['name'] ?? 'Requisito').toString(),
      description: (json['description'] ?? '').toString(),
      required: json['required'] == true,
      allowedExtensions: extensionsPayload is List
          ? extensionsPayload
              .map((extension) => extension.toString().trim().replaceFirst('.', '').toLowerCase())
              .where((extension) => extension.isNotEmpty)
              .toList(growable: false)
          : const [],
    );
  }
}
