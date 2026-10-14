/*
 * Copyright (C) 2025 Google Inc.
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

package com.google.android.accessibility.talkback.trainingcommon.content;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import androidx.annotation.StringRes;
import com.google.android.accessibility.talkback.R;
import com.google.android.accessibility.talkback.trainingcommon.TrainingIpcClient.ServiceData;

/** A {@link WebView} content in a training page. */
public class WebViewContentConfig extends PageContentConfig {

  @StringRes private final int textResId;

  public WebViewContentConfig(@StringRes int textResId) {
    this.textResId = textResId;
  }

  @Override
  public View createView(
      LayoutInflater inflater, ViewGroup container, Context context, ServiceData data) {
    View view = inflater.inflate(R.layout.tutorial_content_webview, container, false);
    WebView webView = view.findViewById(R.id.tutorial_web_view);
    webView.getSettings().setJavaScriptEnabled(true);

    String htmlContent =
        String.format(
            """
            <!DOCTYPE html>
            <html>
            <head>
              <meta name="viewport" content="width=device-width, initial-scale=1">
              <style>
                body {
                  font-family: 'Roboto', sans-serif;
                  padding: 16px;
                  margin: 0;
                  background-color: #ffffff !important;
                  color: #202124 !important;
                }
                h1, h2, h3, h4, h5, h6 {
                  color: #202124 !important;
                  font-weight: 500;
                }
                a {
                  color: #0b57d0 !important;
                }
                div {
                  margin-bottom: 16px;
                }
                div > h4, div > p, div > ul {
                  margin-top: 0;
                  margin-bottom: 0;
                }
                .radio-group {
                  display: flex;
                  flex-direction: row;
                  gap: 24px;
                }
                .radio-option {
                  display: flex;
                  align-items: center;
                  gap: 8px;
                }
                .full-width-textarea {
                  width: 100%%; /* Escaped percent sign */
                  box-sizing: border-box;
                  border: 1px solid #ccc;
                  border-radius: 4px;
                  padding: 8px;
                  font-family: sans-serif;
                  font-size: 16px;
                }
                .table-container {
                  border: 1px solid #DADCE0;
                  border-radius: 8px;
                  overflow: hidden;
                  margin-bottom: 16px;
                }
                table {
                  width: 100%%; /* Escaped percent sign */
                  border-collapse: collapse;
                  margin: 0;
                  padding: 0;
                }
                th, td {
                  text-align: left;
                  padding: 16px;
                  border: none;
                }
                th {
                  font-weight: 500;
                }
                thead th {
                  border-bottom: 1px solid #E8EAED;
                }
                tbody tr:not(:last-child) th, tbody tr:not(:last-child) td {
                  border-bottom: 1px solid #E8EAED;
                }
                button {
                  display: inline-flex;
                  align-items: center;
                  justify-content: center;
                  min-width: 64px;
                  height: 40px;
                  padding: 0 24px;
                  border: 1px solid #79747E;
                  border-radius: 20px;
                  background-color: #FFFFFF;
                  color: #6750A4;
                  font-family: 'Roboto', sans-serif;
                  font-size: 14px;
                  font-weight: 500;
                  text-align: center;
                  cursor: pointer;
                  -webkit-tap-highlight-color: transparent;
                  outline: none;
                }
                button:active {
                  background-color: rgba(103, 80, 164, 0.12);
                }
                button:hover {
                  background-color: rgba(103, 80, 164, 0.08);
                }
                @media (prefers-color-scheme: dark) {
                  body {
                    background-color: #202124;
                    color: #e8eaed;
                  }
                  h1, h2, h3, h4, h5, h6 {
                    color: #e8eaed;
                  }
                  a {
                    color: #8ab4f8;
                  }
                  table, th, td {
                    border: 1px solid white;
                  }
                }
              </style>
            </head>
            <body>
              %s
            </body>
            </html>
            """,
            context.getString(textResId));

    webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null);
    return view;
  }
}
