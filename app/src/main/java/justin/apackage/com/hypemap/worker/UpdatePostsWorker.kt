package justin.apackage.com.hypemap.worker

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import justin.apackage.com.hypemap.model.HypeMapRepository

/**
 * A worker class to perform background updates for posts
 *
 * @author Justin Wong
 */
class UpdatePostsWorker constructor (
    private val context: Context,
    private val workerParams: WorkerParameters
) : Worker(context, workerParams) {

    companion object {
        const val TAG = "UpdatePostsWorker"
    }

    override fun doWork(): Result {
        val repository = HypeMapRepository(context.applicationContext as Application)
        Log.d(TAG, "Performing update posts work")
        repository.updatePosts()
        return Result.success()
    }
}