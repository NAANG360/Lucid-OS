package com.lucid.os.island;

import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.app.Notification;
import android.os.Bundle;

public final class IslandNotificationListener extends NotificationListenerService {
    @Override public void onNotificationPosted(StatusBarNotification sbn) { publish(sbn); }
    @Override public void onNotificationRemoved(StatusBarNotification sbn) {}
    private void publish(StatusBarNotification sbn) {
        Notification n = sbn.getNotification();
        Bundle e = n.extras;
        CharSequence title = e.getCharSequence(Notification.EXTRA_TITLE);
        CharSequence text = e.getCharSequence(Notification.EXTRA_BIG_TEXT);
        if (text == null) text = e.getCharSequence(Notification.EXTRA_TEXT);
        IslandService.publishNotification(title == null ? sbn.getPackageName() : title.toString(), text == null ? "" : text.toString());
    }
}
