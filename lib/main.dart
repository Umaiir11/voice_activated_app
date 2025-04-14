import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

void main() {
  runApp(const MyApp());
}

const MethodChannel platform = MethodChannel('voice_trigger_channel');

class MyApp extends StatefulWidget {
  const MyApp({super.key});

  @override
  State<MyApp> createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> with WidgetsBindingObserver {
  bool _serviceStarted = false;
  bool _hotwordDetected = false;
  String _statusMessage = 'Initializing...';

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
    _startVoiceService();
    _setupMethodCallHandler();
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    super.dispose();
  }

  /// Reattach method channel listener if app is resumed
  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    if (state == AppLifecycleState.resumed) {
      _setupMethodCallHandler();
    }
  }

  void _setupMethodCallHandler() {
    platform.setMethodCallHandler((call) async {
      if (call.method == 'onHotwordDetected') {
        _onHotwordDetected();
      }
    });
  }

  void _onHotwordDetected() {
    setState(() {
      _hotwordDetected = true;
      _statusMessage = 'Hotword detected!';
    });

    Future.delayed(const Duration(seconds: 3), () {
      if (mounted) {
        setState(() {
          _hotwordDetected = false;
          _statusMessage = 'Listening for hotword...';
        });
      }
    });
  }

  Future<void> _startVoiceService() async {
    try {
      final result = await platform.invokeMethod('startService');
      setState(() {
        _serviceStarted = result == 'Service Started';
        _statusMessage = _serviceStarted ? 'Listening for hotword...' : result;
      });
    } on PlatformException catch (e) {
      setState(() {
        _serviceStarted = false;
        _statusMessage = "Error starting service: ${e.message}";
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Voice Wake App',
      theme: ThemeData(
        primarySwatch: Colors.deepPurple,
        useMaterial3: true,
      ),
      home: Scaffold(
        appBar: AppBar(
          title: const Text('Voice Wake App'),
          backgroundColor: Theme.of(context).colorScheme.inversePrimary,
        ),
        body: Center(
          child: Padding(
            padding: const EdgeInsets.all(20),
            child: Column(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                Icon(
                  _serviceStarted ? Icons.mic : Icons.mic_off,
                  size: 80,
                  color: _hotwordDetected
                      ? Colors.orange
                      : (_serviceStarted ? Colors.green : Colors.red),
                ),
                const SizedBox(height: 20),
                Text(
                  _hotwordDetected
                      ? 'Bro, I am here!'
                      : (_serviceStarted
                      ? 'Voice Service is Running'
                      : 'Voice Service is Not Running'),
                  style: TextStyle(
                    fontSize: 20,
                    fontWeight: FontWeight.bold,
                    color: _hotwordDetected
                        ? Colors.orange
                        : (_serviceStarted ? Colors.green : Colors.red),
                  ),
                ),
                const SizedBox(height: 10),
                const Text(
                  'Say "Hey My App" to trigger!',
                  style: TextStyle(fontSize: 16),
                ),
                const SizedBox(height: 20),
                Text(
                  _statusMessage,
                  style: const TextStyle(fontSize: 14),
                ),
                const SizedBox(height: 20),
                if (!_serviceStarted)
                  ElevatedButton(
                    onPressed: _startVoiceService,
                    child: const Text('Start Voice Service'),
                  ),
                if (_hotwordDetected)
                  Container(
                    margin: const EdgeInsets.only(top: 20),
                    padding: const EdgeInsets.all(16),
                    decoration: BoxDecoration(
                      color: Colors.orange.withOpacity(0.2),
                      borderRadius: BorderRadius.circular(12),
                    ),
                    child: const Text(
                      'Hotword detected! What can I help you with?',
                      style: TextStyle(
                        fontSize: 16,
                        fontWeight: FontWeight.bold,
                        color: Colors.orange,
                      ),
                    ),
                  ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
