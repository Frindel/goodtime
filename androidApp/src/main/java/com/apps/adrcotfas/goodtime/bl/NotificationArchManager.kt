package com.apps.adrcotfas.goodtime.bl

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.graphics.drawable.IconCompat
import com.apps.adrcotfas.goodtime.shared.R as SharedR
import com.apps.adrcotfas.goodtime.R

class NotificationArchManager(private val context: Context, private val activityClass: Class<*>) {

    private val notificationManager: NotificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createNotificationChannel()
    }

    fun buildInProgressNotification(data: DomainTimerData): Notification {
        val isCountDown = data.label!!.timerProfile.isCountdown
        val endTime = data.endTime
        val running = data.state != TimerState.PAUSED
        val timerType = data.type
        val labelName = data.label?.name

        val mainStateText = if (timerType == TimerType.WORK) {
            if (running) {
                //TODO: extract strings
                "Work session in progress"
            } else {
                "Work session paused"
            }
        } else {
            "Break in progress"
        }
        val stateText = if (labelName != null) "$labelName: $mainStateText" else mainStateText

        val builder = NotificationCompat.Builder(context, CHANNEL_ID).apply {
            setSmallIcon(SharedR.drawable.ic_status_goodtime)
            setCategory(NotificationCompat.CATEGORY_PROGRESS)
            setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            setContentIntent(createOpenActivityIntent(activityClass))
            setOngoing(true)
            setShowWhen(false)
            setAutoCancel(false)
            setStyle(NotificationCompat.DecoratedCustomViewStyle())
            setCustomContentView(
                buildChronometer(
                    base = endTime,
                    running = running,
                    stateText = stateText,
                    isCountDown = isCountDown
                )
            )
        }
        if (timerType == TimerType.WORK) {
            if (running) {
                val pauseAction = createNotificationAction(
                    title = "Pause",
                    action = TimerService.BUTTON_ACTION_PAUSE
                )
                builder.addAction(pauseAction)
                val addOneMinuteAction = createNotificationAction(
                    title = "+1 min",
                    action = TimerService.BUTTON_ACTION_ADD_ONE_MIN
                )
                builder.addAction(addOneMinuteAction)
            } else {
                val resumeAction = createNotificationAction(
                    title = "Resume",
                    action = TimerService.BUTTON_ACTION_RESUME
                )
                builder.addAction(resumeAction)
                val stopAction = createNotificationAction(
                    title = "Stop",
                    action = TimerService.BUTTON_ACTION_RESET
                )
                builder.addAction(stopAction)
            }
        } else {
            val stopAction = createNotificationAction(
                title = "Stop",
                action = TimerService.BUTTON_ACTION_RESET
            )
            builder.addAction(stopAction)
            val addOneMinuteAction = createNotificationAction(
                title = "+1 min",
                action = TimerService.BUTTON_ACTION_ADD_ONE_MIN
            )
            builder.addAction(addOneMinuteAction)
        }
        val nextActionTitle = if (timerType == TimerType.WORK) {
            "Start break"
        } else {
            "Start work"
        }
        val nextAction = createNotificationAction(
            title = nextActionTitle,
            action = TimerService.BUTTON_ACTION_NEXT
        )
        builder.addAction(nextAction)
        return builder.build()
    }

    fun notifyFinished(data: DomainTimerData) {
        val timerType = data.type
        val labelName = data.label?.name

        val mainStateText = if (timerType == TimerType.WORK) {
            "Work session finished"
        } else {
            "Break finished"
        }
        val stateText = if (labelName != null) "$labelName: $mainStateText" else mainStateText

        val builder = NotificationCompat.Builder(context, CHANNEL_ID).apply {
            setSmallIcon(SharedR.drawable.ic_status_goodtime)
            setCategory(NotificationCompat.CATEGORY_PROGRESS)
            setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            setContentIntent(createOpenActivityIntent(activityClass))
            setOngoing(false)
            setShowWhen(false)
            setAutoCancel(true)
            setStyle(NotificationCompat.DecoratedCustomViewStyle())
            setContentTitle(stateText)
            setContentText("Continue?")
        }
        val nextActionTitle = if (timerType == TimerType.WORK) {
            "Start break"
        } else {
            "Start work"
        }
        val nextAction = createNotificationAction(
            title = nextActionTitle,
            action = TimerService.BUTTON_ACTION_NEXT
        )
        val extender = NotificationCompat.WearableExtender()
        extender.addAction(nextAction)
        builder.addAction(nextAction)
        builder.extend(extender)
        notificationManager.notify(NOTIFICATION_ID, builder.build())
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name =
                "Goodtime Notifications"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                setSound(null, null)
                enableVibration(false)
                setBypassDnd(true)
                setShowBadge(true)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun buildChronometer(
        base: Long,
        running: Boolean,
        stateText: CharSequence,
        isCountDown: Boolean = true,
    ): RemoteViews {
        val content =
            RemoteViews(context.packageName, R.layout.chronometer_notif_content)
        content.setChronometerCountDown(R.id.chronometer, isCountDown)
        content.setChronometer(R.id.chronometer, base, null, running)
        content.setTextViewText(R.id.state, stateText)
        return content
    }

    private fun createOpenActivityIntent(
        activityClass: Class<*>
    ): PendingIntent {
        val intent = Intent(context, activityClass)
        intent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun createNotificationAction(
        icon: IconCompat? = null,
        title: String,
        action: String
    ): NotificationCompat.Action {
        return NotificationCompat.Action.Builder(
            icon,
            title,
            PendingIntent.getService(
                context,
                0,
                TimerService.createIntentWithAction(context, action),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        ).build()
    }

    companion object {
        const val CHANNEL_ID = "goodtime.notification"
        const val NOTIFICATION_ID = 42
    }
}
