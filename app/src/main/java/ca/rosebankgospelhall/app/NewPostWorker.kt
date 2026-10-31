package ca.rosebankgospelhall.app

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import ca.rosebankgospelhall.app.data.Post
import ca.rosebankgospelhall.app.data.Prefs
import ca.rosebankgospelhall.app.data.Repository
import java.util.concurrent.TimeUnit

/** Checks a few times a day for a new post and shows a notification. */
class NewPostWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {
        val prefs = Prefs(applicationContext)
        if (!prefs.notify) return Result.success()
        return try {
            val newest = Repository(applicationContext).fetchPosts().maxByOrNull { it.dateMillis }
            if (newest != null) {
                val last = prefs.lastSeenMillis
                if (last > 0L && newest.dateMillis > last) notifyPost(newest)
                if (newest.dateMillis > last) prefs.lastSeenMillis = newest.dateMillis
            }
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    private fun notifyPost(p: Post) {
        val ctx = applicationContext
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        val nm = ctx.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL, "New posts", NotificationManager.IMPORTANCE_DEFAULT),
        )
        val intent = Intent(ctx, MainActivity::class.java)
            .putExtra("postId", p.id)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pi = PendingIntent.getActivity(
            ctx, p.id, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val text = p.excerpt.ifBlank { "A new post is available." }
        val n = NotificationCompat.Builder(ctx, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(p.title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setAutoCancel(true)
            .setContentIntent(pi)
            .build()
        nm.notify(p.id, n)
    }

    companion object {
        private const val CHANNEL = "new_posts"
        private const val WORK = "new_post_check"

        fun schedule(ctx: Context) {
            val req = PeriodicWorkRequestBuilder<NewPostWorker>(6, TimeUnit.HOURS)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            WorkManager.getInstance(ctx).enqueueUniquePeriodicWork(WORK, ExistingPeriodicWorkPolicy.KEEP, req)
        }

        fun cancel(ctx: Context) {
            WorkManager.getInstance(ctx).cancelUniqueWork(WORK)
        }
    }
}
