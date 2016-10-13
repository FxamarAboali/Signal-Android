/**
 * Copyright (C) 2014 Open Whisper Systems
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
package org.thoughtcrime.securesms;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import org.thoughtcrime.securesms.crypto.MasterSecret;
import org.thoughtcrime.securesms.database.DatabaseFactory;
import org.thoughtcrime.securesms.database.ImageDatabase.ImageRecord;
import org.thoughtcrime.securesms.recipients.Recipient;
import org.thoughtcrime.securesms.recipients.Recipient.RecipientModifiedListener;
import org.thoughtcrime.securesms.recipients.RecipientFactory;
import org.thoughtcrime.securesms.util.DateUtils;
import org.thoughtcrime.securesms.util.DynamicLanguage;
import org.thoughtcrime.securesms.util.SaveAttachmentTask;
import org.thoughtcrime.securesms.util.SaveAttachmentTask.Attachment;

import java.util.ArrayList;
import java.util.List;

/**
 * Activity for displaying media attachments in-app
 */
public class MediaPreviewActivity extends PassphraseRequiredActionBarActivity
                                  implements RecipientModifiedListener, OnPageChangeListener {
  private final static String TAG = MediaPreviewActivity.class.getSimpleName();

  public static final String THREAD_ID_EXTRA = "thread_id";

  private final DynamicLanguage dynamicLanguage = new DynamicLanguage();

  private MasterSecret masterSecret;

  private MediaPreviewViewPager viewPager;
  private Uri                   mediaUri;
  private String                mediaType;
  private Recipient             recipient;
  private long                  threadId;
  private long                  date;
  private List<ImageRecord>     imageRecords;

  @Override
  protected void onCreate(Bundle bundle, @NonNull MasterSecret masterSecret) {
    this.masterSecret = masterSecret;
    this.setTheme(R.style.TextSecure_DarkTheme);
    dynamicLanguage.onCreate(this);

    setFullscreenIfPossible();
    getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                         WindowManager.LayoutParams.FLAG_FULLSCREEN);

    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    setContentView(R.layout.media_preview_activity);

    initializeResources();
    initializeMedia();
    initializeViewPager();
  }

  @TargetApi(VERSION_CODES.JELLY_BEAN)
  private void setFullscreenIfPossible() {
    if (VERSION.SDK_INT >= VERSION_CODES.JELLY_BEAN) {
      getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN);
    }
  }

  @Override
  public void onModified(Recipient recipient) {
    initializeActionBar();
  }

  private void initializeActionBar() {
    final CharSequence relativeTimeSpan;
    if (date > 0) {
      relativeTimeSpan = DateUtils.getRelativeTimeSpanString(date,
                                                             System.currentTimeMillis(),
                                                             DateUtils.MINUTE_IN_MILLIS);
    } else {
      relativeTimeSpan = null;
    }
    getSupportActionBar().setTitle(recipient == null ? getString(R.string.MediaPreviewActivity_you)
                                                     : recipient.toShortString());
    getSupportActionBar().setSubtitle(relativeTimeSpan);
  }

  @Override
  public void onResume() {
    super.onResume();
    dynamicLanguage.onResume(this);
    if (recipient != null) recipient.addListener(this);
    initializeViewPager();
  }

  @Override
  public void onPause() {
    super.onPause();
    if (recipient != null) recipient.removeListener(this);
    viewPager.removeOnPageChangeListener(this);
    cleanupMedia();
  }

  @Override
  protected void onNewIntent(Intent intent) {
    super.onNewIntent(intent);
    if (recipient != null) recipient.removeListener(this);
    setIntent(intent);
  }

  private void initializeResources() {
    this.threadId  = getIntent().getLongExtra(THREAD_ID_EXTRA, -1);
    this.mediaUri  = getIntent().getData();
    this.mediaType = getIntent().getType();
  }

  private void initializeMedia() {
    if (!isContentTypeSupported(mediaType)) {
      Log.w(TAG, "Unsupported media type sent to MediaPreviewActivity, finishing.");
      Toast.makeText(getApplicationContext(), R.string.MediaPreviewActivity_unssuported_media_type, Toast.LENGTH_LONG).show();
      finish();
    }
  }

  private void initializeViewPager() {
    this.imageRecords = getImageRecords(getApplicationContext(), threadId);
    this.viewPager    = (MediaPreviewViewPager) findViewById(R.id.viewPager);
    viewPager.setAdapter(new MediaPreviewAdapter(MediaPreviewActivity.this,masterSecret,imageRecords));
    viewPager.addOnPageChangeListener(this);

    int startPosition = getImagePosition(mediaUri);
    viewPager.setCurrentItem(startPosition);
    if (startPosition == 0) onPageSelected(0);
  }

  private void cleanupMedia() {
    viewPager.setAdapter(null);
  }

  private void saveToDisk() {
    SaveAttachmentTask.showWarningDialog(this, new DialogInterface.OnClickListener() {
      @Override
      public void onClick(DialogInterface dialogInterface, int i) {
        SaveAttachmentTask saveTask = new SaveAttachmentTask(MediaPreviewActivity.this, masterSecret);
        saveTask.execute(new Attachment(mediaUri, mediaType, date));
      }
    });
  }

  @Override
  public boolean onPrepareOptionsMenu(Menu menu) {
    super.onPrepareOptionsMenu(menu);

    menu.clear();
    MenuInflater inflater = this.getMenuInflater();
    inflater.inflate(R.menu.media_preview, menu);

    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    super.onOptionsItemSelected(item);

    switch (item.getItemId()) {
    case R.id.save:         saveToDisk(); return true;
    case android.R.id.home: finish();     return true;
    }

    return false;
  }

  @Override
  public void onPageScrollStateChanged(int state) {}

  @Override
  public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {}

  @Override
  public void onPageSelected(int position) {
    updateResources(position);
    initializeActionBar();
  }

  public static boolean isContentTypeSupported(final String contentType) {
    return contentType != null && contentType.startsWith("image/");
  }

  private int getImagePosition(Uri mediaUri) {
    int position = -1;
    for (int i = 0; i < imageRecords.size(); i++) {
      if (imageRecords.get(i).getAttachment().getDataUri().equals(mediaUri)) {
        position = i;
        break;
      }
    }
    if (position < 0) {
      Log.w(TAG, "Media not part of images for thread, finishing.");
      Toast.makeText(getApplicationContext(), R.string.MediaPreviewActivity_cant_display, Toast.LENGTH_LONG).show();
      finish();
    }
    return position;
  }

  private List<ImageRecord> getImageRecords(Context context, Long threadId) {
    List<ImageRecord> imageRecords = new ArrayList<>(0);
    Cursor imagesForThread = DatabaseFactory.getImageDatabase(context).getImagesForThread(threadId);
    if (imagesForThread.moveToLast()) {
      do {
        imageRecords.add(ImageRecord.from(imagesForThread));
      } while (imagesForThread.moveToPrevious());
    }
    return imageRecords;
  }

  private void updateResources(int position) {
    ImageRecord imageRecord = imageRecords.get(position);
    this.mediaUri           = imageRecord.getAttachment().getDataUri();
    this.mediaType          = imageRecord.getAttachment().getContentType();
    this.date               = imageRecord.getDate();

    if (recipient != null) recipient.removeListener(this);
    String newAddress = imageRecord.getAddress();
    if (newAddress == null) {
      recipient = null;
    } else {
      recipient = RecipientFactory.getRecipientsFromString(getApplicationContext(), newAddress, true).getPrimaryRecipient();
      if (recipient != null) recipient.addListener(this);
    }
  }
}
