package com.android.talkback.quickmenu;

import android.content.Context;
import com.google.android.accessibility.talkback.R;
import com.google.android.accessibility.talkback.actor.TalkBackUIActor;
import com.google.android.accessibility.talkback.quickmenu.QuickMenuOverlay;
import java.util.Map;

/** Provides {@link QuickMenuOverlay} for different {@link TalkBackUIActor.Type}s on Handset. */
public final class QuickMenuOverlayProvider {

  public static void provideQuickMenuOverlays(
      Context context, Map<TalkBackUIActor.Type, QuickMenuOverlay> typeToOverlay) {
    typeToOverlay.put(
        TalkBackUIActor.Type.SELECTOR_MENU_ITEM_OVERLAY_MULTI_FINGER,
        new QuickMenuOverlay(context, R.layout.quick_menu_item_overlay));
    typeToOverlay.put(
        TalkBackUIActor.Type.SELECTOR_MENU_ITEM_OVERLAY_SINGLE_FINGER,
        new QuickMenuOverlay(
            context, R.layout.quick_menu_item_overlay_without_multifinger_gesture));
    typeToOverlay.put(
        TalkBackUIActor.Type.SELECTOR_ITEM_ACTION_OVERLAY,
        new QuickMenuOverlay(context, R.layout.quick_menu_item_action_overlay));
    typeToOverlay.put(
        TalkBackUIActor.Type.GESTURE_OR_KEYBOARD_ACTION_OVERLAY,
        new QuickMenuOverlay(context, R.layout.quick_menu_item_action_overlay));
  }

  private QuickMenuOverlayProvider() {}
}
