package com.google.android.accessibility.talkback

import android.util.Log
import com.google.android.accessibility.utils.Logger
import com.google.android.libraries.accessibility.utils.log.LogUtils

/**
 * Represent a component for the [android.app.Service.dump]
 *
 * @property command the command provided by the arguments from the dump request
 * @property description the brief description used in the help for this command
 * @property condition the condition deciding whether to invoke the [action]. Default condition is
 *   provided by [conditionDefault]
 * @property action the action executed when the condition is matched
 */
data class DumpComponent
@JvmOverloads
constructor(
  val command: String,
  val description: String,
  val condition: ConditionCheckerFunction = conditionDefault,
  val action: (dumpLogger: Logger) -> Unit,
) {
  fun conditionToString(): String {
    return when (condition) {
      conditionDefault -> "[Default]"
      conditionDefaultVerboseOnly -> "[Default(Verbose)]"
      else -> ""
    }
  }
}

object TalkBackServiceDumpHelper {
  /** Run through the [dumpComponents] if its condition match the given [argsSet]. */
  @JvmStatic
  fun dump(dumpLogger: Logger, argsSet: Set<String>, dumpComponents: List<DumpComponent>) {
    for (component in dumpComponents) {
      if (component.condition(argsSet, component.command)) {
        component.action(dumpLogger)
      }
    }
  }
}

/**
 * Dumps components based on `argsSet`: either an empty `argsSet` or specified components if
 * `argsSet` contains `componentName`.
 */
val conditionDefault: ConditionCheckerFunction = { argsSet, componentName ->
  argsSet == null || argsSet.isEmpty() || argsSet.contains(componentName)
}

/** Dumps the components conditionally if `argsSet` contains `componentName`. */
val conditionGivenArgs: ConditionCheckerFunction = { argsSet, componentName ->
  argsSet != null && argsSet.contains(componentName)
}

/**
 * Dumps the components given the condition the same as [conditionDefault] and the condition that
 * the logger is in the verbose mode.
 */
val conditionDefaultVerboseOnly: ConditionCheckerFunction = { argsSet, componentName ->
  conditionDefault(argsSet, componentName) && LogUtils.shouldLog(Log.VERBOSE)
}

/**
 * Checks the condition of the dump request parameters `argsSet` and the given `componentName` to
 * decide whether [DumpComponent.action] should be invoked or not.
 */
typealias ConditionCheckerFunction = (argsSet: Set<String>?, componentName: String) -> Boolean
