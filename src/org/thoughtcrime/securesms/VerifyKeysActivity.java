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
package org.thoughtcrime.securesms;

import org.thoughtcrime.securesms.crypto.KeyUtil;
import org.thoughtcrime.securesms.crypto.MasterSecret;
import org.thoughtcrime.securesms.crypto.SerializableKey;
import org.thoughtcrime.securesms.database.SessionRecord;
import org.thoughtcrime.securesms.lang.BhoButton;
import org.thoughtcrime.securesms.lang.BhoTextView;
import org.thoughtcrime.securesms.recipients.Recipient;
import org.thoughtcrime.securesms.util.Hex;
import org.thoughtcrime.securesms.util.MemoryCleaner;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;

/**
 * Activity for verifying session keys.
 * 
 * @author Moxie Marlinspike
 *
 */
public class VerifyKeysActivity extends KeyScanningActivity {

  private byte[] yourFingerprintBytes;
  private byte[] theirFingerprintBytes;
	
  private BhoTextView yourFingerprint;
  private BhoTextView theirFingerprint;
  private BhoButton verifiedButton;
  private BhoButton abortButton;
  private BhoButton cancelButton;
  private BhoButton compareButton;
  private Recipient recipient;
  private MasterSecret masterSecret;
	
  @Override
  protected void onCreate(Bundle state) {
    super.onCreate(state);
    setContentView(R.layout.verify_keys_activity);
		
    initializeResources();
    initializeFingerprints();
    initializeCallbacks();
  }
	
  @Override
  protected void onDestroy() {
    MemoryCleaner.clean(masterSecret);
    super.onDestroy();
  }
  
  private void initializeCallbacks() {
    this.verifiedButton.setOnClickListener(new VerifiedListener());
    this.abortButton.setOnClickListener(new AbortListener());
    this.cancelButton.setOnClickListener(new CancelListener());
    this.compareButton.setOnClickListener(new CompareListener());
  }
	
  private void initializeResources() {
    this.recipient        = (Recipient)this.getIntent().getParcelableExtra("recipient");
    this.masterSecret     = (MasterSecret)this.getIntent().getParcelableExtra("master_secret");
    this.yourFingerprint  = (BhoTextView)findViewById(R.id.you_read);
    this.theirFingerprint = (BhoTextView)findViewById(R.id.friend_reads);
    this.verifiedButton   = (BhoButton)findViewById(R.id.verified_button);
    this.abortButton      = (BhoButton)findViewById(R.id.abort_button);
    this.cancelButton     = (BhoButton)findViewById(R.id.cancel_button);
    this.compareButton    = (BhoButton)findViewById(R.id.compare_button);
  }
	
  private void initializeFingerprints() {
    SessionRecord session      = new SessionRecord(this, masterSecret, recipient);
    this.yourFingerprintBytes  = session.getLocalFingerprint();
    this.theirFingerprintBytes = session.getRemoteFingerprint();
		
    this.yourFingerprint.setText(Hex.toString(yourFingerprintBytes));
    this.theirFingerprint.setText(Hex.toString(theirFingerprintBytes));
  }
	
  private class VerifiedListener implements OnClickListener {
    public void onClick(View v) {
      SessionRecord sessionRecord = new SessionRecord(VerifyKeysActivity.this, masterSecret, recipient);
      sessionRecord.setVerifiedSessionKey(true);
      sessionRecord.save();
      VerifyKeysActivity.this.finish();
    }
  }
	
  private class CancelListener implements OnClickListener {
    public void onClick(View v) {
      VerifyKeysActivity.this.finish();
    }
  }
	
  private class CompareListener implements View.OnClickListener {
    public void onClick(View v) {
      registerForContextMenu(compareButton);
      compareButton.showContextMenu();
    }
  }
	
  private class AbortListener implements OnClickListener {
    public void onClick(View v) {
      AlertDialog.Builder builder = new AlertDialog.Builder(VerifyKeysActivity.this);
      builder.setTitle(R.string.abort_secure_session_confirmation);
      builder.setIcon(android.R.drawable.ic_dialog_alert);
      builder.setCancelable(true);
      builder.setMessage(R.string.are_you_sure_that_you_want_to_abort_this_secure_session_);
      builder.setPositiveButton(R.string.yes, new AbortConfirmListener());
      builder.setNegativeButton(R.string.no, null);
      builder.show();
    }
  }

  private class AbortConfirmListener implements DialogInterface.OnClickListener {
    public void onClick(DialogInterface dialog, int which) {
      KeyUtil.abortSessionFor(VerifyKeysActivity.this, recipient);
      VerifyKeysActivity.this.finish();
    }
  }

  @Override
  protected String getDisplayString() {
    return getString(R.string.get_my_fingerprint_scanned);
  }
	
  @Override
  protected String getScanString() {
    return getString(R.string.scan_their_fingerprint);
  }

  @Override
    protected SerializableKey getIdentityKeyToCompare() {
    return new FingerprintKey(theirFingerprintBytes);
  }

  @Override
  protected SerializableKey getIdentityKeyToDisplay() {
    return new FingerprintKey(yourFingerprintBytes);
  }

  @Override
  protected String getNotVerifiedMessage() {
    return getString(R.string.warning_the_scanned_key_does_not_match);
  }

  @Override
  protected String getNotVerifiedTitle() {
    return getString(R.string.not_verified_);
  }

  @Override
  protected String getVerifiedMessage() {
    return getString(R.string.their_key_is_correct);
  }

  @Override
  protected String getVerifiedTitle() {
    return getString(R.string.verified_);
  }
	
  private class FingerprintKey implements SerializableKey {
    private final byte[] fingerprint;
		
    public FingerprintKey(byte[] fingerprint) {
      this.fingerprint = fingerprint;
    }
		
    public byte[] serialize() {
      return fingerprint;
    }
  }

}
