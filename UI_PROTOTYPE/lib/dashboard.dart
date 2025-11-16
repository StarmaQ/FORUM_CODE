import 'dart:async';
import 'dart:math';

import 'package:flutter/material.dart';

import 'models.dart';
import 'notification_overlay.dart';

class DashboardScreen extends StatefulWidget {
  const DashboardScreen({super.key});

  @override
  State<DashboardScreen> createState() => _DashboardScreenState();
}

class _DashboardScreenState extends State<DashboardScreen> {
  final List<NotificationItem> _notifications = [];
  final List<DrivingData> _history = [];
  Timer? _timer;
  final Random _rnd = Random();
  double _speed = 0.0; // m/s

  @override
  void initState() {
    super.initState();
    // start simulation
    _timer = Timer.periodic(const Duration(milliseconds: 600), (_) => _generateTick());
  }

  @override
  void dispose() {
    _timer?.cancel();
    super.dispose();
  }

  void _generateTick() {
    // simulate speed changes: convert to m/s (UI shows km/h)
    // small random acceleration between -5 and +3 m/s^2, but sometimes harsher braking
    double accel = (_rnd.nextDouble() * 8) - 5; // -5 .. +3

    // inject a harsh braking occasionally
    if (_rnd.nextDouble() < 0.06) {
      accel = -8 - _rnd.nextDouble() * 6; // -8 .. -14
    }

    // limit accel
    accel = accel.clamp(-20.0, 5.0);

    // update speed (simple Euler step)
    double newSpeed = max(0.0, _speed + accel * 0.6);

    final data = DrivingData(speed: newSpeed, acceleration: accel);

    setState(() {
      _speed = newSpeed;
      _history.insert(0, data);
      if (_history.length > 100) _history.removeLast();
    });

    _analyzeData(data);
  }

  void _analyzeData(DrivingData d) {
    // harsh braking detection threshold: accel <= -6 m/s^2
    if (d.acceleration <= -6.0) {
      final n = NotificationItem(
        id: DateTime.now().millisecondsSinceEpoch.toString(),
        title: 'Harsh Braking',
        message: 'Detected strong deceleration: ${d.acceleration.toStringAsFixed(1)} m/s²',
      );
      setState(() {
        _notifications.insert(0, n);
        if (_notifications.length > 5) _notifications.removeLast();
      });
    }
  }

  double _computeEcoScore() {
    // simple heuristic: start at 100, subtract penalties for harsh accel/brake
    double score = 100;
    for (final d in _history.take(30)) {
      if (d.acceleration > 2.5) score -= (d.acceleration - 2.5) * 1.8;
      if (d.acceleration < -4.0) score -= (-d.acceleration - 4.0) * 2.5;
    }
    return score.clamp(0, 100);
  }

  @override
  Widget build(BuildContext context) {
    final eco = _computeEcoScore();

    return Scaffold(
      appBar: AppBar(
        title: const Text('Eco Driving Dashboard'),
        actions: [
          IconButton(
            icon: const Icon(Icons.pause),
            onPressed: () {
              setState(() {
                if (_timer?.isActive ?? false) {
                  _timer?.cancel();
                  _timer = null;
                } else {
                  _timer = Timer.periodic(const Duration(milliseconds: 600), (_) => _generateTick());
                }
              });
            },
            tooltip: 'Pause/Resume simulation',
          )
        ],
      ),
      body: SafeArea(
        child: Stack(
          children: [
            SingleChildScrollView(
              padding: const EdgeInsets.all(12.0),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    children: [
                      _StatCard(label: 'Eco Score', value: eco.toStringAsFixed(0), unit: '%'),
                      const SizedBox(width: 8),
                      _StatCard(label: 'Speed', value: (_speed * 3.6).toStringAsFixed(0), unit: 'km/h'),
                      const SizedBox(width: 8),
                      _StatCard(label: 'Accel', value: _history.isNotEmpty ? _history.first.acceleration.toStringAsFixed(1) : '0.0', unit: 'm/s²'),
                    ],
                  ),
                  const SizedBox(height: 12),
                  const Text('Recent events', style: TextStyle(fontWeight: FontWeight.bold)),
                  const SizedBox(height: 8),
                  SizedBox(
                    height: 300,
                    child: Card(
                      child: Padding(
                        padding: const EdgeInsets.all(8.0),
                        child: _history.isEmpty
                            ? const Center(child: Text('No data yet'))
                            : ListView.builder(
                                shrinkWrap: true,
                                physics: const AlwaysScrollableScrollPhysics(),
                                itemCount: _history.length,
                                itemBuilder: (context, i) {
                                  final d = _history[i];
                                  return ListTile(
                                    dense: true,
                                    leading: Icon(
                                      d.acceleration <= -6 ? Icons.car_crash : Icons.directions_car,
                                      color: d.acceleration <= -6 ? Colors.red : Colors.blueGrey,
                                    ),
                                    title: Text('${(d.speed * 3.6).toStringAsFixed(0)} km/h • ${d.acceleration.toStringAsFixed(1)} m/s²'),
                                    subtitle: Text('${d.timestamp.toLocal().toIso8601String()}'),
                                  );
                                },
                              ),
                      ),
                    ),
                  ),
                  const SizedBox(height: 12),
                  const Text('Notifications', style: TextStyle(fontWeight: FontWeight.bold)),
                  const SizedBox(height: 8),
                  SizedBox(
                    height: 90,
                    child: Card(
                      child: Padding(
                        padding: const EdgeInsets.all(8.0),
                        child: _notifications.isEmpty
                            ? const Center(child: Text('No notifications'))
                            : ListView.separated(
                                scrollDirection: Axis.horizontal,
                                itemCount: _notifications.length,
                                separatorBuilder: (_, __) => const SizedBox(width: 8),
                                itemBuilder: (context, i) {
                                  final n = _notifications[i];
                                  return Container(
                                    width: 260,
                                    padding: const EdgeInsets.all(8),
                                    decoration: BoxDecoration(borderRadius: BorderRadius.circular(8), color: Colors.white),
                                    child: Column(
                                      crossAxisAlignment: CrossAxisAlignment.start,
                                      children: [
                                        Text(n.title, style: const TextStyle(fontWeight: FontWeight.bold)),
                                        const SizedBox(height: 6),
                                        Text(n.message, style: const TextStyle(fontSize: 12)),
                                        const Spacer(),
                                        Text('${n.time.hour}:${n.time.minute.toString().padLeft(2, '0')}', style: const TextStyle(fontSize: 11, color: Colors.grey)),
                                      ],
                                    ),
                                  );
                                },
                              ),
                      ),
                    ),
                  ),
                  const SizedBox(height: 8),
                  Row(
                    children: [
                      ElevatedButton.icon(
                        onPressed: () {
                          setState(() {
                            _notifications.clear();
                          });
                        },
                        icon: const Icon(Icons.clear_all),
                        label: const Text('Clear'),
                      ),
                      const SizedBox(width: 12),
                      ElevatedButton.icon(
                        onPressed: () {
                          setState(() {
                            _history.clear();
                          });
                        },
                        icon: const Icon(Icons.refresh),
                        label: const Text('Reset Data'),
                      ),
                    ],
                  ),
                  const SizedBox(height: 24),
                ],
              ),
            ),
            // overlay notifications
            NotificationOverlay(items: _notifications),
          ],
        ),
      ),
    );
  }
}

class _StatCard extends StatelessWidget {
  final String label;
  final String value;
  final String unit;

  const _StatCard({required this.label, required this.value, required this.unit});

  @override
  Widget build(BuildContext context) {
    return Expanded(
      child: Card(
        child: Padding(
          padding: const EdgeInsets.all(12.0),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(label, style: const TextStyle(color: Colors.grey, fontSize: 12)),
              const SizedBox(height: 8),
              Row(
                crossAxisAlignment: CrossAxisAlignment.end,
                children: [
                  Text(value, style: const TextStyle(fontSize: 22, fontWeight: FontWeight.bold)),
                  const SizedBox(width: 6),
                  Text(unit, style: const TextStyle(color: Colors.grey)),
                ],
              )
            ],
          ),
        ),
      ),
    );
  }
}
