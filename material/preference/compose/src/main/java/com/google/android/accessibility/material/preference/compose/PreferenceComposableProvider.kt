package com.google.android.accessibility.material.preference.compose

import androidx.compose.runtime.Composable

/** An interface that provides preference composable for rendering. */
interface PreferenceComposableProvider {

  /** Provides a preference composable for current state. */
  fun providePreferenceComposable(): @Composable () -> Unit
}
