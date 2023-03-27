package com.sample.vpn

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CompletableDeferred


class ActivityResult(
    val resultCode: Int,
    val data: Intent?) {
}

/**
 * 去tm的onActivityResult这种傻x设计
 */
abstract class AsyncActivity : AppCompatActivity() {
    var currentCode : Int = 0
    var resultByCode = mutableMapOf<Int, CompletableDeferred<ActivityResult?>>()

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        resultByCode[requestCode]?.let {
            it.complete(ActivityResult(resultCode, data))
            resultByCode.remove(requestCode)
        } ?: run {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    suspend fun launchIntent(intent: Intent) : ActivityResult?
    {
        val activityResult = CompletableDeferred<ActivityResult?>()

        if (intent.resolveActivity(packageManager) != null) {
            val resultCode = currentCode++
            resultByCode[resultCode] = activityResult
            startActivityForResult(intent, resultCode)
        } else {
            activityResult.complete(null)
        }
        return activityResult.await()
    }
}