/*
 * Copyright (C) 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.android.accessibility.gemineye.screenoverview.json

import com.google.gson.GsonBuilder
import com.google.gson.JsonParseException

/** Parses a ScreenOverview from a JSON string */
class Parser {

  @Throws(ParseException::class)
  fun parseScreenOverview(json: String): ScreenOverview {
    try {
      return gson.fromJson(json, ScreenOverview::class.java)
    } catch (e: JsonParseException) {
      throw ParseException(e)
    }
  }

  @Throws(ParseException::class)
  fun parseScreenQueryAnswer(json: String): ScreenQueryAnswer {
    try {
      return gson.fromJson(json, ScreenQueryAnswer::class.java)
    } catch (e: JsonParseException) {
      throw ParseException(e)
    }
  }

  /** Serializes a ScreenQueryAnswer to a JSON string */
  fun serialize(screenQueryAnswer: ScreenQueryAnswer): String {
    return gson.toJson(screenQueryAnswer)
  }

  /** Serializes a ScreenOverview to a JSON string */
  fun serialize(screenOverview: ScreenOverview): String {
    return gson.toJson(screenOverview)
  }

  companion object {
    private val gson = GsonBuilder().create()
  }
}

/** The response failed to parse */
class ParseException(cause: Throwable) : Exception(cause)
