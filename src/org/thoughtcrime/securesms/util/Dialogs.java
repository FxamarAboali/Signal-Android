/**
 * Copyright (C) 2011 Whisper Systems
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.thoughtcrime.securesms.util;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;

import org.thoughtcrime.securesms.R;

public class Dialogs {
  public static void showAlertDialog(Context context, String title, String message) {
    ContextThemeWrapper ctw = new ContextThemeWrapper(context, R.style.GSecure_Light_Dialog);
    AlertDialog.Builder dialog = new AlertDialog.Builder(ctw);
    dialog.setTitle(title);
    dialog.setMessage(message);
    dialog.setIcon(resolveIcon(context, R.attr.dialog_alert_icon));
    dialog.setPositiveButton(android.R.string.ok, null);
    dialog.show();
  }
  public static void showInfoDialog(Context context, String title, String message) {
    ContextThemeWrapper ctw = new ContextThemeWrapper(context, R.style.GSecure_Light_Dialog);
    AlertDialog.Builder dialog = new AlertDialog.Builder(ctw);
    dialog.setTitle(title);
    dialog.setMessage(message);
    dialog.setIcon(resolveIcon(context, R.attr.dialog_info_icon));
    dialog.setPositiveButton(android.R.string.ok, null);
    dialog.show();
  }

  public static Drawable resolveIcon(Context c, int iconAttr) {
    TypedValue out = new TypedValue();
    c.getTheme().resolveAttribute(iconAttr, out, true);
    return c.getResources().getDrawable(out.resourceId);
  }
}
