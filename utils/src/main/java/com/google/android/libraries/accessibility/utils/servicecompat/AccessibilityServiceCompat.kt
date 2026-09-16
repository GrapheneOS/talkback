package com.google.android.libraries.accessibility.utils.servicecompat

import android.accessibilityservice.AccessibilityService
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.annotation.CallSuper
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ServiceLifecycleDispatcher
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner

/** An AccessibilityService that is also a [LifecycleOwner] and a [SavedStateRegistryOwner]. */
abstract class AccessibilityServiceCompat :
  AccessibilityService(), LifecycleOwner, SavedStateRegistryOwner {

  private val dispatcher = ServiceLifecycleDispatcher(this)

  private val stateBundle = Bundle()
  private val savedStateRegistryController = SavedStateRegistryController.create(this)

  @CallSuper
  override fun onCreate() {
    runOnMain {
      savedStateRegistryController.performAttach()
      savedStateRegistryController.performRestore(stateBundle)
      dispatcher.onServicePreSuperOnCreate()
    }
    super.onCreate()
  }

  @CallSuper
  override fun onServiceConnected() {
    runOnMain { dispatcher.onServicePreSuperOnBind() }
    super.onServiceConnected()
  }

  @CallSuper
  override fun onDestroy() {
    runOnMain {
      savedStateRegistryController.performSave(stateBundle)
      dispatcher.onServicePreSuperOnDestroy()
    }
    super.onDestroy()
  }

  override val lifecycle: Lifecycle
    get() = dispatcher.lifecycle

  override val savedStateRegistry: SavedStateRegistry
    get() = savedStateRegistryController.savedStateRegistry

  private fun runOnMain(runnable: Runnable) {
    if (Looper.getMainLooper().isCurrentThread) {
      runnable.run()
    } else {
      Handler(Looper.getMainLooper()).post(runnable)
    }
  }
}
