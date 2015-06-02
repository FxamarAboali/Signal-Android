package org.thoughtcrime.securesms.components.emoji;

import android.content.Context;
import android.graphics.drawable.Drawable.Callback;
import android.support.v7.widget.AppCompatEditText;
import android.util.AttributeSet;


public class EmojiEditText extends AppCompatEditText {
  private final Callback callback = new PostInvalidateCallback(this);

  public EmojiEditText(Context context) {
    super(context);
  }

  public EmojiEditText(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  public EmojiEditText(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
  }

  @Override public void setText(CharSequence text, BufferType type) {
    super.setText(EmojiProvider.getInstance(getContext()).emojify(text, EmojiProvider.EMOJI_SMALL, new PostInvalidateCallback(this)),
                  BufferType.SPANNABLE);
  }

  public void insertEmoji(int codePoint) {
    final int          start = getSelectionStart();
    final int          end   = getSelectionEnd();
    final char[]       chars = Character.toChars(codePoint);
    final CharSequence text  = EmojiProvider.getInstance(getContext()).emojify(new String(chars),
                                                                               EmojiProvider.EMOJI_SMALL,
                                                                               callback);

    getText().replace(Math.min(start, end), Math.max(start, end), text);
    setSelection(end + chars.length);
  }
}
