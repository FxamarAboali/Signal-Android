package org.thoughtcrime.securesms.contacts.avatars;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;

import com.amulyakhare.textdrawable.TextDrawable;
import com.amulyakhare.textdrawable.util.ColorGenerator;

import org.thoughtcrime.securesms.R;

public class GeneratedContactPhoto implements ContactPhoto {

  private final String name;

  GeneratedContactPhoto(@NonNull String name) {
    this.name  = name;
  }

  @Override
  public Drawable asDrawable(Context context, int color) {
    return asDrawable(context, color, false);
  }

  @Override
  public Drawable asDrawable(Context context, int color, boolean inverted) {
    int targetSize = context.getResources().getDimensionPixelSize(R.dimen.contact_photo_target_size);

    return TextDrawable.builder()
                       .beginConfig()
                       .width(targetSize)
                       .height(targetSize)
                       .textColor(inverted ? color : Color.WHITE)
                       .endConfig()
                       .buildRound(getCharacter(name), inverted ? Color.WHITE : color);
  }

  private String getCharacter(String name) {
    String cleanedName = name.replaceAll("[^\\p{L}\\p{Nd}]+", "");

    if (cleanedName.length() > 0) {
      return String.valueOf(cleanedName.charAt(0));
    } else {
      return "#";
    }
  }
}
