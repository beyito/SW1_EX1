class ProcessInstanceModel {
  final String id;
  final String policyId;
  final String title;
  final String description;
  final String status;

  const ProcessInstanceModel({
    required this.id,
    required this.policyId,
    required this.title,
    required this.description,
    required this.status,
  });

  factory ProcessInstanceModel.fromJson(Map<String, dynamic> json) {
    return ProcessInstanceModel(
      id: (json['id'] ?? '').toString(),
      policyId: (json['policyId'] ?? '').toString(),
      title: (json['title'] ?? '').toString(),
      description: (json['description'] ?? '').toString(),
      status: (json['status'] ?? '').toString(),
    );
  }
}
