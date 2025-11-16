class DrivingData {
  final double speed; // m/s
  final double acceleration; // m/s^2
  final DateTime timestamp;

  DrivingData({required this.speed, required this.acceleration, DateTime? timestamp})
      : timestamp = timestamp ?? DateTime.now();
}

class NotificationItem {
  final String id;
  final String title;
  final String message;
  final DateTime time;

  NotificationItem({required this.id, required this.title, required this.message, DateTime? time})
      : time = time ?? DateTime.now();
}
