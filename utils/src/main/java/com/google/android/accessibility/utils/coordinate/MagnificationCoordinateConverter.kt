package com.google.android.accessibility.utils.coordinate

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityService.MagnificationController.OnMagnificationChangedListener
import android.accessibilityservice.MagnificationConfig
import android.graphics.Point
import android.graphics.Rect
import android.graphics.Region
import androidx.annotation.VisibleForTesting
import com.google.android.accessibility.utils.BuildVersionUtils
import com.google.android.accessibility.utils.FeatureSupport

/**
 * Convert between magnified coordinates and unmagnified coordinates. Magnified coordinates will be
 * noted Local, and unmagnified will be noted global.
 *
 * Problem it's trying to solve: When magnified, [AccessibilityNodeInfo] returns magnified (local)
 * coordinates, while user interaction (user selected rect), and drawing works in unmagnified
 * (global) coordinate. This class helps converting between two.
 */
class MagnificationCoordinateConverter() {

  /** Metadata for calculating coordinates. Null if magnification is not active. */
  private var config: MagnificationCoordinateConfig? = null

  /** Local display window (after magnification). */
  private val displayLocal: Rect?
    get() = config?.displayLocal

  /** Global display size (before magnification). */
  private val displayGlobal: Rect?
    get() = config?.displayGlobal

  private val magnificationChangedListener: OnMagnificationChangedListener? =
    if (!FeatureSupport.supportMagnificationController()) {
      null
    } else if (BuildVersionUtils.isAtLeastT()) {
      OnMagnificationChangedListener { _, _, scale, centerX, centerY ->
        applyConfig(scale, centerX.toInt(), centerY.toInt())
      }
    } else {
      object : OnMagnificationChangedListener {
        override fun onMagnificationChanged(
          magnificationController: AccessibilityService.MagnificationController,
          region: Region,
          config: MagnificationConfig,
        ) {
          applyConfig(config.scale, config.centerX.toInt(), config.centerY.toInt())
        }

        @Deprecated("Deprecated in Java")
        override fun onMagnificationChanged(
          controller: AccessibilityService.MagnificationController,
          region: Region,
          scale: Float,
          centerX: Float,
          centerY: Float,
        ) {
          // Deprecated, no action.
        }
      }
    }

  /**
   * This is called once when Select to Speak is triggered from the shortcut button. Initialize all
   * metadata required for coordinate conversion, and listen for future magnification changes if not
   * already listening.
   */
  fun startWatchingMagnification(service: AccessibilityService) {
    updateCoordinates(MagnificationCoordinateConfig.createConfig(service))
    if (magnificationChangedListener != null) {
      service.magnificationController.addListener(magnificationChangedListener)
    }
  }

  fun stopWatchingMagnification(service: AccessibilityService) {
    if (magnificationChangedListener != null) {
      service.magnificationController.removeListener(magnificationChangedListener)
    }
  }

  /** Update the coordinates from the service without adding a listener for changes. */
  fun updateCoordinates(service: AccessibilityService) {
    updateCoordinates(MagnificationCoordinateConfig.createConfig(service))
  }

  @VisibleForTesting
  fun updateCoordinates(config: MagnificationCoordinateConfig?) {
    this.config = config
  }

  /** Magnify given global rect to local coordinate. */
  fun localize(rectGlobal: Rect) {
    val from = displayGlobal ?: return
    val to = displayLocal ?: return

    CoordinateConverter.convert(from, to, rectGlobal)
  }

  /** Unmagnify given local rect to global coordinate. */
  fun globalize(rectLocal: Rect) {
    val from = displayLocal ?: return
    val to = displayGlobal ?: return

    CoordinateConverter.convert(from, to, rectLocal)
  }

  private fun applyConfig(scale: Float, centerX: Int, centerY: Int) {
    if (displayGlobal == null) {
      return
    }
    displayGlobal?.let {
      updateCoordinates(MagnificationCoordinateConfig(scale, Point(centerX, centerY), it))
    }
  }
}
