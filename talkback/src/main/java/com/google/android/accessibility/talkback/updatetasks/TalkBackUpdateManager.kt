package com.google.android.accessibility.talkback.updatetasks

import com.google.android.accessibility.talkback.updatetasks.Versions.VERSION_UNKNOWN
import com.google.android.libraries.accessibility.utils.log.LogUtils

/**
 * A manager to run a list of [VersionUpdateTask] based on the version before migration and the
 * target version.
 */
class TalkBackUpdateManager(private val updateTasks: List<VersionUpdateTask> = emptyList()) {

  companion object {
    private const val TAG = "TalkBackUpdateManager"
  }

  fun startMigration(currentVersionName: String?, previousVersionName: String?) {
    LogUtils.d(TAG, "current version name: $currentVersionName")
    LogUtils.d(TAG, "previous version name: $previousVersionName")

    val currentVersion = convertToVersion(currentVersionName)
    val previousVersion = convertToVersion(previousVersionName)
    LogUtils.d(TAG, "current version: $currentVersion")
    LogUtils.d(TAG, "previous version: $previousVersion")
    if (currentVersion == VERSION_UNKNOWN || previousVersion == VERSION_UNKNOWN) {
      LogUtils.e(
        TAG,
        "Failed to perform update tasks. currentVersionName=$currentVersionName, previousVersionName=$previousVersionName",
      )
      return
    }

    updateTasks.forEach { updateTask ->
      if (updateTask.shouldMigrate(previousVersion)) {
        updateTask.onMigrate?.invoke()
      }

      if (updateTask.matchCurrentVersion(currentVersion)) {
        updateTask.onMatchCurrentVersion?.invoke()
      }
    }
  }
}
