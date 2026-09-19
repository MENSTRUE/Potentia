# POTENTIA release rules.
# WorkManager may recreate the Worker by class name after process death/app restart.
-keep class com.potentia.reminder.WeeklyReminderWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}
