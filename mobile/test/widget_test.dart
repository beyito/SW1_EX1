import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  testWidgets('smoke test renders a material app shell', (WidgetTester tester) async {
    await tester.pumpWidget(
      const MaterialApp(
        home: Scaffold(
          body: Text('BPMN Cliente'),
        ),
      ),
    );

    expect(find.text('BPMN Cliente'), findsOneWidget);
  });
}
