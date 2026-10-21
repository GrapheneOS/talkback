package com.google.android.accessibility.utils.performance

/**
 * Stores information about [AccessibilityEvent][android.view.accessibility.AccessibilityEvent] and
 * its corresponding node.
 */
data class AccessibilityAttributes(
  val eventFromIndex: Int = -1,
  val eventToIndex: Int = -1,
  val itemCount: Int = -1,
  val eventMovementGranularity: Int = -1,
  val nodeIsTextEntryKey: Boolean = false,
  val nodeHasInputFocus: Boolean = false,
  val nodeIsInIMEWindow: Boolean = false,
  val nodeIsEditable: Boolean = false,
  val nodeHashCode: Long = -1,
) {
  constructor(
    nodeIsTextEntryKey: Boolean,
    nodeIsInIMEWindow: Boolean,
    nodeHashCode: Long,
  ) : this(
    eventFromIndex = -1,
    eventToIndex = -1,
    itemCount = -1,
    eventMovementGranularity = 0,
    nodeIsTextEntryKey,
    nodeHasInputFocus = false,
    nodeIsInIMEWindow,
    nodeIsEditable = false,
    nodeHashCode,
  )
}
