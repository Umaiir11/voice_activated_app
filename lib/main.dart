import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

void main() {
  runApp(const MyApp());
}

const platform = MethodChannel('voice_trigger_channel');

class MyApp extends StatefulWidget {
  const MyApp({super.key});

  @override
  State<MyApp> createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  bool _serviceStarted = false;
  String _statusMessage = 'Initializing...';

  @override
  void initState() {
    super.initState();
    _startVoiceService();
  }

  Future<void> _startVoiceService() async {
    try {
      final result = await platform.invokeMethod('startService');
      setState(() {
        _serviceStarted = true;
        _statusMessage = result;
      });
    } on PlatformException catch (e) {
      setState(() {
        _statusMessage = "Error: ${e.message}";
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Voice Trigger App',
      theme: ThemeData(
        primarySwatch: Colors.blue,
        useMaterial3: true,
      ),
      home: Scaffold(
        appBar: AppBar(
          title: const Text('Voice Wake App'),
          backgroundColor: Theme.of(context).colorScheme.inversePrimary,
        ),
        body: Center(
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Icon(
                _serviceStarted ? Icons.mic : Icons.mic_off,
                size: 80,
                color: _serviceStarted ? Colors.green : Colors.red,
              ),
              const SizedBox(height: 20),
              Text(
                _serviceStarted
                    ? 'Voice Service is Running'
                    : 'Voice Service is Not Running',
                style: TextStyle(
                  fontSize: 18,
                  fontWeight: FontWeight.bold,
                  color: _serviceStarted ? Colors.green : Colors.red,
                ),
              ),
              const SizedBox(height: 10),
              Text(
                'Say "Hey My App" to open!',
                style: const TextStyle(fontSize: 16),
              ),
              const SizedBox(height: 20),
              Text(_statusMessage),
              const SizedBox(height: 20),
              if (!_serviceStarted)
                ElevatedButton(
                  onPressed: _startVoiceService,
                  child: const Text('Start Voice Service'),
                ),
            ],
          ),
        ),
      ),
    );
  }
}