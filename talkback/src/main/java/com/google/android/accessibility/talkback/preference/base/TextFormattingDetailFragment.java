/*
 * Copyright 2025 Google Inc.
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
package com.google.android.accessibility.talkback.preference.base;

import com.google.android.accessibility.talkback.R;

/** Panel holding a set of text formatting details preferences. */
public class TextFormattingDetailFragment extends TalkbackBaseFragment {

  public TextFormattingDetailFragment() {
    super(R.xml.text_formatting_detail_preferences);
  }

  @Override
  public CharSequence getTitle() {
    return getText(R.string.title_text_formatting_options);
  }

  @Override
  public void onResume() {
    super.onResume();
    String announcementModeTitle =
        TextFormattingAnnouncementModeFragment.getAnnouncementModeTitle(getContext());
    findPreference(getString(R.string.pref_text_formatting_announcement_mode_key))
        .setSummary(announcementModeTitle);
  }
}
