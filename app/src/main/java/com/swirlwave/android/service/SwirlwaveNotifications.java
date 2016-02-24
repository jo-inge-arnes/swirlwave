package com.swirlwave.android.service;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;

import com.swirlwave.android.MainActivity;
import com.swirlwave.android.R;

class SwirlwaveNotifications {
    public static final int SERVICE_NOTIFICATION_ID = 1;
    private Context mContext;
    private NotificationManagerCompat mNotificationManager;

    public SwirlwaveNotifications(Context context) {
        mContext = context;
        mNotificationManager = NotificationManagerCompat.from(context);
    }

    public Notification createStartupNotification() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(mContext);
        return builder.setCategory(Notification.CATEGORY_SERVICE)
                .setOngoing(true)
                .setContentTitle(mContext.getString(R.string.service_name))
                .setContentText(mContext.getString(R.string.service_starting))
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentIntent(getMainPendingIntent())
                .build();
    }

    private PendingIntent getMainPendingIntent() {
        Intent notificationIntent = new Intent(mContext, MainActivity.class);
        notificationIntent.setAction("android.intent.action.MAIN");
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        return PendingIntent.getActivity(mContext, 0, notificationIntent, 0);
    }

    public void notifyNoConnection() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(mContext);
        Notification notification = builder.setCategory(Notification.CATEGORY_SERVICE)
                .setOngoing(true)
                .setContentTitle(mContext.getString(R.string.service_name))
                .setContentText(mContext.getString(R.string.no_internet_connection))
                .setSmallIcon(R.mipmap.ic_launcher)
                .setProgress(0, 0, false)
                .setContentIntent(getMainPendingIntent())
                .build();
        mNotificationManager.notify(SERVICE_NOTIFICATION_ID, notification);
    }

    public void notifyHasConnection(String msg) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(mContext);
        Notification notification = builder.setCategory(Notification.CATEGORY_SERVICE)
                .setOngoing(true)
                .setContentTitle(mContext.getString(R.string.service_name))
                .setContentText(msg)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setProgress(0, 0, false)
                .setContentIntent(getMainPendingIntent())
                .build();
        mNotificationManager.notify(SERVICE_NOTIFICATION_ID, notification);
    }

    public void notifyConnecting() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(mContext);
        Notification notification = builder.setCategory(Notification.CATEGORY_SERVICE)
                .setOngoing(true)
                .setContentTitle(mContext.getString(R.string.service_name))
                .setContentText(mContext.getString(R.string.service_connecting))
                .setSmallIcon(R.mipmap.ic_launcher)
                .setProgress(100, 50, true)
                .setContentIntent(getMainPendingIntent())
                .build();
        mNotificationManager.notify(SERVICE_NOTIFICATION_ID, notification);
    }
}
