class IntelligentReceptionResult {
  final List<IntelligentReceptionCandidate> candidates;

  const IntelligentReceptionResult({
    required this.candidates,
  });

  factory IntelligentReceptionResult.fromJson(Map<String, dynamic> json) {
    final payload = json['candidates'];
    return IntelligentReceptionResult(
      candidates: payload is List
          ? payload
              .whereType<Map<String, dynamic>>()
              .map(IntelligentReceptionCandidate.fromJson)
              .where((candidate) => candidate.policyId.isNotEmpty)
              .toList(growable: false)
          : const [],
    );
  }
}

class IntelligentReceptionCandidate {
  final String policyId;
  final String policyName;
  final double confidence;
  final List<String> missingRequirements;
  final String reason;

  const IntelligentReceptionCandidate({
    required this.policyId,
    required this.policyName,
    required this.confidence,
    required this.missingRequirements,
    required this.reason,
  });

  factory IntelligentReceptionCandidate.fromJson(Map<String, dynamic> json) {
    final missingPayload = json['missingRequirements'] ?? json['missing_requirements'];
    return IntelligentReceptionCandidate(
      policyId: (json['policyId'] ?? '').toString(),
      policyName: (json['policyName'] ?? '').toString(),
      confidence: (json['confidence'] as num?)?.toDouble() ?? 0,
      missingRequirements: missingPayload is List
          ? missingPayload
              .map((item) => item.toString())
              .where((item) => item.trim().isNotEmpty)
              .toList(growable: false)
          : const [],
      reason: (json['reason'] ?? '').toString(),
    );
  }
}
