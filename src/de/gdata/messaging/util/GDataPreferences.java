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
import java.util.LinkedList;
import java.util.Set;

public class GDataPreferences {

  public static final String INTENT_ACCESS_SERVER = "de.gdata.mobilesecurity.ACCESS_SERVER";
  public static final String ISFA_PACKAGE = "de.gdata.mobilesecurity";
  public static final String ISFA_PACKAGE_2 = "de.gdata.mobilesecurity2";
  public static final String ISFA_PACKAGE_3 = "de.gdata.mobilesecurity2g";
  public static final String ISFA_PACKAGE_4 = "de.gdata.mobilesecurity2b";
  public static final String ISFA_PACKAGE_5 = "de.gdata.mobilesecurityorange";

  public static final String[] ISFA_PACKAGES = {ISFA_PACKAGE, ISFA_PACKAGE_2, ISFA_PACKAGE_3, ISFA_PACKAGE_4, ISFA_PACKAGE_5};

  private static final String VIEW_PAGER_LAST_PAGE = "VIEW_PAGER_LAST_PAGE";
  private static final String APPLICATION_FONT = "APPLICATION_FONT";
  private static final String PRIVACY_ACTIVATED = "PRIVACY_ACTIVATED";
  private static final String SAVED_HIDDEN_RECIPIENTS = "SAVED_HIDDEN_RECIPIENTS";
  private static final String SAVE_E164_NUMBER = "SAVE_E164_NUMBER";

  private static final String PROFILE_PICTURE_URI = "PROFILE_PICTURE_URI";
  private static final String PROFILE_STATUS = "PROFILE_STATUS";
  private static final String ACTIVE_CONTACTS = "ACTIVE_CONTACTS";


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

  public void setProfilePictureUri(String uri) {
    mPreferences.edit().putString(PROFILE_PICTURE_URI, uri).commit();
  }
  public String getProfilePictureUri() {
    return mPreferences.getString(PROFILE_PICTURE_URI, "");
  }

  public void setProfilePartId(String profileId, Long profilePartId) {
    mPreferences.edit().putLong("id:" + profileId, profilePartId).commit();
  }
  public void setProfilePartRow(String profileId, Long profilePartId) {
    mPreferences.edit().putLong("row:" + profileId, profilePartId).commit();
  }
  public Long getProfilePartId(String profileId) {
    return mPreferences.getLong("id:" +profileId, 0L);
  }
  public Long getProfilePartRow(String profileId) {
    return mPreferences.getLong("row:"+profileId, 0L);
  }
  public void setProfileStatus(String profileStatus) {
    mPreferences.edit().putString(PROFILE_STATUS, profileStatus).commit();
  }
  public String getProfileStatus() {
    return mPreferences.getString(PROFILE_STATUS, "");
  }
  public void setProfileStatusForProfileId(String profileId, String status) {
    mPreferences.edit().putString("status:" + profileId, status).commit();
  }
  public String getProfileStatusForProfileId(String profileId) {
    return mPreferences.getString("status:" +profileId, "");
  }
  public void setApplicationFont(String applicationFont) {
    mPreferences.edit().putString(APPLICATION_FONT, applicationFont).commit();
  }
  public void saveFilterGroupIdForContact(String phoneNo, long filterGroupId) {
    mPreferences.edit().putLong(phoneNo, filterGroupId).commit();
  }
  public boolean saveActiveContacts(String[] array) {
    mPreferences.edit().putInt(ACTIVE_CONTACTS + "_size", array.length).commit();
    for(int i=0;i<array.length;i++) {
      mPreferences.edit().putString(ACTIVE_CONTACTS + "_" + i, array[i]).commit();
    }
    return  mPreferences.edit().commit();
  }
  public String[] getActiveContacts() {
    int size = mPreferences.getInt(ACTIVE_CONTACTS + "_size", 0);
    String array[] = new String[size];
    for(int i=0;i<size;i++) {
      array[i] = mPreferences.getString(ACTIVE_CONTACTS + "_" + i, "");
    }
    return array;
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
      try {
        for (Long recId : recipients) {
          hiddenRecipients.add(RecipientFactory.getRecipientForId(mContext, recId, false));
        }
      } catch(ClassCastException ex) {
        Log.e("GDataPreferences", ex.getMessage());
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

