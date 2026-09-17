package com.brand.expensetracker;

import android.Manifest;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import java.text.NumberFormat;
import java.util.Calendar;
import java.util.Locale;

public class SummaryReceiver extends BroadcastReceiver {
    public static final String CHANNEL_ID = "expense_summaries";

    @Override
    public void onReceive(Context context, Intent intent) {
        ExpenseDb db = new ExpenseDb(context);
        Calendar now = Calendar.getInstance();
        long dayStart = ExpenseDb.startOfDay(now.getTimeInMillis());
        long monthStart = ExpenseDb.startOfMonth(now.getTimeInMillis());
        boolean daily = db.getBoolSetting("daily_reminder", true);
        boolean weekly = db.getBoolSetting("weekly_reminder", true) && now.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY;
        Calendar tomorrow = (Calendar) now.clone(); tomorrow.add(Calendar.DAY_OF_MONTH, 1);
        boolean monthly = db.getBoolSetting("monthly_reminder", true) && tomorrow.get(Calendar.MONTH) != now.get(Calendar.MONTH);
        if (!daily && !weekly && !monthly) return;

        NumberFormat nf = NumberFormat.getNumberInstance(new Locale("en", "PK"));
        StringBuilder body = new StringBuilder();
        if (daily) body.append("Today: Rs ").append(nf.format(db.total("All", dayStart, ExpenseDb.addDays(dayStart,1))));
        if (weekly) {
            long weekStart = ExpenseDb.addDays(dayStart, -6);
            if (body.length() > 0) body.append("  •  ");
            body.append("7 days: Rs ").append(nf.format(db.total("All", weekStart, ExpenseDb.addDays(dayStart,1))));
        }
        if (monthly) {
            if (body.length() > 0) body.append("  •  ");
            body.append("Month: Rs ").append(nf.format(db.total("All", monthStart, ExpenseDb.addMonths(monthStart,1))));
        }
        show(context, body.toString());
    }

    public static void schedule(Context context) {
        createChannel(context);
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent i = new Intent(context, SummaryReceiver.class);
        PendingIntent pi = PendingIntent.getBroadcast(context, 991, i, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Calendar next = Calendar.getInstance();
        next.set(Calendar.HOUR_OF_DAY, 20); next.set(Calendar.MINUTE, 0); next.set(Calendar.SECOND, 0); next.set(Calendar.MILLISECOND, 0);
        if (next.getTimeInMillis() <= System.currentTimeMillis()) next.add(Calendar.DAY_OF_YEAR, 1);
        if (am != null) am.setInexactRepeating(AlarmManager.RTC_WAKEUP, next.getTimeInMillis(), AlarmManager.INTERVAL_DAY, pi);
    }

    private static void show(Context context, String body) {
        createChannel(context);
        if (Build.VERSION.SDK_INT >= 33 && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return;
        Intent launch = new Intent(context, MainActivity.class);
        PendingIntent open = PendingIntent.getActivity(context, 992, launch, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder b = Build.VERSION.SDK_INT >= 26 ? new Notification.Builder(context, CHANNEL_ID) : new Notification.Builder(context);
        b.setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Expense summary")
                .setContentText(body)
                .setStyle(new Notification.BigTextStyle().bigText(body))
                .setContentIntent(open)
                .setAutoCancel(true);
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) nm.notify((int) (System.currentTimeMillis() % 100000), b.build());
    }

    public static void createChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(CHANNEL_ID, "Expense summaries", NotificationManager.IMPORTANCE_DEFAULT);
            ch.setDescription("Daily, weekly and monthly local expense summaries");
            NotificationManager nm = context.getSystemService(NotificationManager.class);
            if (nm != null) nm.createNotificationChannel(ch);
        }
    }
}
