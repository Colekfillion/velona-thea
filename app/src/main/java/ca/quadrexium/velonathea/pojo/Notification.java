package ca.quadrexium.velonathea.pojo;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

/**
 * Wrapper for Notifications.
 */
public class Notification {
    private final static int MAX_PROGRESS = 1000;
    private final String notificationChannel;
    private final Context context;
    private final int id;
    private int priority = -1;
    private int smallIcon = -1;
    private String title;
    private String content;
    private NotificationManagerCompat notificationManager;
    private NotificationCompat.Builder notificationBuilder;

    public Notification(Notification.Builder builder) {
        this.notificationChannel = builder.notificationChannel;
        this.context = builder.context;
        this.id = builder.id;
        this.title = builder.title;
        this.content = builder.content;
        this.smallIcon = builder.smallIcon;
        this.priority = builder.priority;
        create(); //prepare for user display
    }

    /**
     * Prepares notification for user display.
     */
    public void create() {
        notificationManager = NotificationManagerCompat.from(context);
        notificationBuilder = new NotificationCompat.Builder(context, notificationChannel);
        notificationBuilder.setContentTitle(title).setContentText(content);
        if (priority != -1) {
            notificationBuilder.setPriority(priority);
        }
        notificationBuilder.setChannelId(notificationChannel);
    }

    /**
     * Pushes the notification.
     */
    public void show() {
        if (notificationManager == null) {
            create();
        }
        notificationManager.notify(id, notificationBuilder.build());
    }

    /**
     * Dismisses the notification.
     */
    public void dismiss() {
        if (notificationManager != null) {
            notificationManager.cancel(id);
        }
    }

    public void setTitle(String title) {
        title = title != null ? title : "";
        this.title = title;
        notificationBuilder.setContentTitle(title);
    }

    public void setContent(String content) {
        content = content != null ? content : "";
        this.content = content;
        notificationBuilder.setContentText(content);
    }

    public void setProgress(int progress) {
        if (progress >= 0 && progress <= MAX_PROGRESS) {
            notificationBuilder.setProgress(MAX_PROGRESS, progress, false);
            return;
        }
        throw new IllegalArgumentException("Progress must be between 0 and 1000. ");
    }

    /**
     * Builder to set and validate initial values for a Notification.
     */
    public static class Builder {
        private final String notificationChannel;
        private final Context context;
        private final int id;
        private String title;
        private String content;
        private int priority;
        private int smallIcon;

        public Builder(@NonNull Context context, String notificationChannel, int id) {
            this.context = context;
            this.notificationChannel = notificationChannel;
            this.id = id;
        }

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder content(String content) {
            this.content = content;
            return this;
        }

        public Builder smallIcon(int smallIcon) {
            this.smallIcon = smallIcon;
            return this;
        }

        public Builder priority(int priority) {
            this.priority = priority;
            return this;
        }

        private void validate(Notification notification) {
            if (notification.id == -1) {
                throw new IllegalStateException("Notification ID must be positive. ");
            }
            if (notification.smallIcon == -1) {
                throw new IllegalStateException("Notification must have an icon. ");
            }
        }

        public Notification build() {
            Notification notification = new Notification(this);
            validate(notification);
            return notification;
        }
    }
}