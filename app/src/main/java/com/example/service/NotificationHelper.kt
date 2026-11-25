package com.example.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.model.Achievement
import com.example.model.AchievementCategory
import com.example.model.AchievementManager
import com.example.model.ActivityRecord
import com.example.model.UserProfile

object NotificationHelper {
    private const val CHANNEL_ID = "el_caminante_achievements"
    private const val CHANNEL_NAME = "Logros y Metas"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            if (manager?.getNotificationChannel(CHANNEL_ID) == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Alertas de logros desbloqueados y metas semanales cumplidas"
                }
                manager?.createNotificationChannel(channel)
            }
        }
    }

    fun showAchievementNotification(context: Context, achievement: Achievement) {
        createNotificationChannel(context)
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("¡Nuevo Logro Desbloqueado! ${achievement.iconName}")
            .setContentText("Has conseguido: ${achievement.title} - ${achievement.description}")
            .setStyle(NotificationCompat.BigTextStyle().bigText("¡Felicitaciones! Has desbloqueado la medalla \"${achievement.title}\": ${achievement.description}."))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        try {
            manager.notify(achievement.hashCode(), builder.build())
        } catch (_: SecurityException) {}
    }

    fun showWeeklyGoalNotification(context: Context, goalType: String, percent: Int, current: Float, target: Float) {
        createNotificationChannel(context)
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

        val title = when (percent) {
            50 -> "🎯 ¡50% de tu Meta Semanal Alcanzado!"
            75 -> "🔥 ¡75% de tu Meta Semanal Completado!"
            else -> "🏆 ¡100% de tu Meta Semanal Cumplida!"
        }

        val unit = if (goalType == "distancia") "km" else "m"
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText("Has alcanzado el $percent%% de tu objetivo de $goalType (%.1f / %.0f %s)".format(current, target, unit))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        try {
            manager.notify("$goalType-$percent".hashCode(), builder.build())
        } catch (_: SecurityException) {}
    }

    private var lastOffTrackNotificationTime = 0L

    fun showOffTrackNotification(context: Context, distanceMeters: Double) {
        val now = System.currentTimeMillis()
        if (now - lastOffTrackNotificationTime < 30000L) return
        lastOffTrackNotificationTime = now

        createNotificationChannel(context)
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("⚠️ ¡Alerta de Desvío de Ruta!")
            .setContentText("Te has desviado %.0f metros de la ruta planificada (más de 20m).".format(distanceMeters))
            .setStyle(NotificationCompat.BigTextStyle().bigText("Atención: Te encuentras a %.0f metros de la ruta planificada. Por favor, regresa al track original.".format(distanceMeters)))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        try {
            manager.notify(9999, builder.build())
        } catch (_: SecurityException) {}
    }

    // Checks and triggers notifications if newly unlocked
    private val notifiedAchievements = mutableSetOf<String>()
    private var notifiedDist50 = false
    private var notifiedDist75 = false
    private var notifiedDist100 = false

    private var notifiedEle50 = false
    private var notifiedEle75 = false
    private var notifiedEle100 = false

    fun checkAndNotifyProgress(context: Context, activities: List<ActivityRecord>, profile: UserProfile) {
        val totalDistance = activities.sumOf { it.distanceKm }
        val totalElevation = activities.sumOf { it.elevationGain }
        val totalActivities = activities.size

        // Check Achievements
        AchievementManager.allAchievements.forEach { ach ->
            val currVal = when (ach.category) {
                AchievementCategory.DISTANCE -> totalDistance
                AchievementCategory.ELEVATION -> totalElevation
                AchievementCategory.ACTIVITIES -> totalActivities.toDouble()
            }
            if (currVal >= ach.requiredValue && !notifiedAchievements.contains(ach.id)) {
                notifiedAchievements.add(ach.id)
                showAchievementNotification(context, ach)
            }
        }

        // Check Weekly Goals (last 7 days)
        val now = System.currentTimeMillis()
        val weekActs = activities.filter { (now - it.startTime) <= (7 * 86400000L) }
        val weekDist = weekActs.sumOf { it.distanceKm }.toFloat()
        val weekEle = weekActs.sumOf { it.elevationGain }.toFloat()

        // Distance milestones (50%, 75%, 100%)
        val targetDist = profile.weeklyDistanceGoalKm
        if (targetDist > 0f) {
            val distRatio = weekDist / targetDist
            if (distRatio >= 0.5f && !notifiedDist50) {
                notifiedDist50 = true
                showWeeklyGoalNotification(context, "distancia", 50, weekDist, targetDist)
            }
            if (distRatio >= 0.75f && !notifiedDist75) {
                notifiedDist75 = true
                showWeeklyGoalNotification(context, "distancia", 75, weekDist, targetDist)
            }
            if (distRatio >= 1.0f && !notifiedDist100) {
                notifiedDist100 = true
                showWeeklyGoalNotification(context, "distancia", 100, weekDist, targetDist)
            }
        }

        // Elevation milestones (50%, 75%, 100%)
        val targetEle = profile.weeklyElevationGoalM
        if (targetEle > 0f) {
            val eleRatio = weekEle / targetEle
            if (eleRatio >= 0.5f && !notifiedEle50) {
                notifiedEle50 = true
                showWeeklyGoalNotification(context, "desnivel", 50, weekEle, targetEle)
            }
            if (eleRatio >= 0.75f && !notifiedEle75) {
                notifiedEle75 = true
                showWeeklyGoalNotification(context, "desnivel", 75, weekEle, targetEle)
            }
            if (eleRatio >= 1.0f && !notifiedEle100) {
                notifiedEle100 = true
                showWeeklyGoalNotification(context, "desnivel", 100, weekEle, targetEle)
            }
        }
    }
}
