package de.gdata.messaging.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import org.thoughtcrime.securesms.recipients.Recipient;
import org.thoughtcrime.securesms.recipients.RecipientFactory;
import org.thoughtcrime.securesms.util.JsonUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class GDataPreferences {

  public static final String INTENT_ACCESS_SERVER = "de.gdata.mobilesecurity.ACCESS_SERVER";
  public static final String ISFA_PACKAGE = "de.gdata.mobilesecurity";
  public static final String ISFA_PACKAGE_2 = "de.gdata.mobilesecurity2";
  private static final String VIEW_PAGER_LAST_PAGE = "VIEW_PAGER_LAST_PAGE";
  private static final String APPLICATION_FONT = "APPLICATION_FONT";
  private static final String PRIVACY_ACTIVATED = "PRIVACY_ACTIVATED";
  private static final String SAVED_HIDDEN_RECIPIENTS = "SAVED_HIDDEN_RECIPIENTS";
  private static final String SAVE_E164_NUMBER = "SAVE_E164_NUMBER";
  private static final String DELETED_MESSAGES = "DELETED_MESSAGES";


  private final SharedPreferences mPreferences;
  private final Context mContext;

  public GDataPreferences(Context context) {
    mPreferences = PreferenceManager.getDefaultSharedPreferences(context);
    mContext = context;
  }
  public void setViewPagerLastPage(int page) {
    mPreferences.edit().putInt(VIEW_PAGER_LAST_PAGE, page).commit();
  }
  public int getViewPagersLastPage() {
    return mPreferences.getInt(VIEW_PAGER_LAST_PAGE, 0);
  }

  public void setPrivacyActivated(boolean activated) {
    mPreferences.edit().putBoolean(PRIVACY_ACTIVATED, activated).commit();
  }

  public boolean isPrivacyActivated() {
    return mPreferences.getBoolean(PRIVACY_ACTIVATED, true);
  }

  public void setApplicationFont(String applicationFont) {
    mPreferences.edit().putString(APPLICATION_FONT, applicationFont).commit();
  }
  public void saveFilterGroupIdForContact(String phoneNo, long filterGroupId) {
    mPreferences.edit().putLong(phoneNo, filterGroupId).commit();
  }
  public long getFilterGroupIdForContact(String phoneNo) {
    return mPreferences.getLong(phoneNo, -1L);
  }
  public void saveHiddenRecipients(ArrayList<Recipient> hiddenRecipients) {
    ArrayList<Long> recIds = new ArrayList<Long>();
    for (Recipient recipient : hiddenRecipients) {
      recIds.add(recipient.getRecipientId());
    }
    try {
      mPreferences.edit().putString(SAVED_HIDDEN_RECIPIENTS, JsonUtils.toJson(recIds)).commit();
    } catch (IOException e) {
      Log.e("GDataPreferences", e.getMessage());
    }
  }
  public ArrayList<Recipient> getSavedHiddenRecipients() {
    ArrayList<Recipient> hiddenRecipients = null;
    try {
      ArrayList<Long> recipients = JsonUtils.fromJson(mPreferences.getString(SAVED_HIDDEN_RECIPIENTS, JsonUtils.toJson(new ArrayList<Long>())), ArrayList.class);

      hiddenRecipients = new ArrayList<Recipient>();
      for (Long recId : recipients) {
        hiddenRecipients.add(RecipientFactory.getRecipientForId(mContext, recId, false));
      }
    } catch (IOException e) {
      Log.e("GDataPreferences", e.getMessage());
    }
    return hiddenRecipients != null ? hiddenRecipients : new ArrayList<Recipient>();
  }
  public String getApplicationFont() {
    return mPreferences.getString(APPLICATION_FONT, "");
  }

  public void saveE164Number(String e164number) {
    mPreferences.edit().putString(SAVE_E164_NUMBER, e164number).commit();
  }
  public String getE164Number() {
    return mPreferences.getString(SAVE_E164_NUMBER, "");
  }
}

