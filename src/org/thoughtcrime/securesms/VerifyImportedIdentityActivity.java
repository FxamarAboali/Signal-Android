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

import org.thoughtcrime.securesms.crypto.IdentityKey;
import org.thoughtcrime.securesms.crypto.InvalidKeyException;
import org.thoughtcrime.securesms.crypto.MasterSecret;
import org.thoughtcrime.securesms.database.DatabaseFactory;
import org.thoughtcrime.securesms.lang.BhoButton;
import org.thoughtcrime.securesms.lang.BhoEditText;
import org.thoughtcrime.securesms.lang.BhoTextView;
import org.thoughtcrime.securesms.util.Dialogs;
import org.thoughtcrime.securesms.util.MemoryCleaner;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Activity for verifying identities keys as they are imported.
 * 
 * @author Moxie Marlinspike
 */

public class VerifyImportedIdentityActivity extends KeyScanningActivity {
		
  private MasterSecret masterSecret;
  private String contactName;
  private IdentityKey identityKey;
  private BhoEditText identityName;
  private BhoTextView identityFingerprint;
	
  private BhoButton compareButton;
  private BhoButton verifiedButton;
  private BhoButton cancelButton;
	
  @Override
  public void onCreate(Bundle state) {
    super.onCreate(state);
    setContentView(R.layout.verify_imported_identity_activity);
		
    initializeResources();
    initializeFingerprints();
    initializeListeners();
  }
  
  @Override
  protected void onDestroy() {
    MemoryCleaner.clean(masterSecret);
    super.onDestroy();
  }
	
  private void initializeListeners() {
    verifiedButton.setOnClickListener(new VerifiedButtonListener());
    cancelButton.setOnClickListener(new CancelButtonListener());
    compareButton.setOnClickListener(new CompareButtonListener());
  }
	
  private void initializeFingerprints() {
    if (contactName != null)
      identityName.setText(contactName);
    identityFingerprint.setText(identityKey.getFingerprint());
  }
	
  private void initializeResources() {
    masterSecret        = (MasterSecret)this.getIntent().getParcelableExtra("master_secret");
    identityFingerprint = (BhoTextView)findViewById(R.id.imported_identity);
    identityName        = (BhoEditText)findViewById(R.id.identity_name);
    identityKey         = (IdentityKey)this.getIntent().getParcelableExtra("identity_key");
    contactName         = (String)this.getIntent().getStringExtra("contact_name");
    verifiedButton      = (BhoButton)findViewById(R.id.verified_button);
    cancelButton        = (BhoButton)findViewById(R.id.cancel_button);
    compareButton       = (BhoButton)findViewById(R.id.compare_button);
  }
			
  private class CancelButtonListener implements View.OnClickListener {
    public void onClick(View v) {
      finish();
    }
  }
	
  private class CompareButtonListener implements View.OnClickListener {
    public void onClick(View v) {
      registerForContextMenu(compareButton);
      compareButton.showContextMenu();
    }
  }
		
  private class VerifiedButtonListener implements View.OnClickListener {
    public void onClick(View v) {
      if (identityName.getText() == null || identityName.getText().length() == 0) {
        Toast.makeText(VerifyImportedIdentityActivity.this, R.string.you_must_specify_a_name_for_this_contact_, Toast.LENGTH_LONG);
        return;
      }
			
      AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(VerifyImportedIdentityActivity.this);
      dialogBuilder.setTitle(R.string.save_identity_key_);
      dialogBuilder.setIcon(android.R.drawable.ic_dialog_info);
      dialogBuilder.setMessage(getString(R.string.are_you_sure_that_you_would_like_to_mark_this_as_a_valid_identity_key_,identityName.getText()) + 
      getString(R.string.you_should_only_do_this_if_you_have_actually_verified_the_fingerprint_));
      dialogBuilder.setCancelable(true);
      dialogBuilder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface arg0, int arg1) {
          try {
            DatabaseFactory.getIdentityDatabase(VerifyImportedIdentityActivity.this).saveIdentity(masterSecret, identityKey, identityName.getText().toString());
          } catch (InvalidKeyException ike) {
            Log.w("VerifiedButtonListener", ike);
            Dialogs.displayAlert(VerifyImportedIdentityActivity.this, "Error saving identity key!", 
                                 getString(R.string.this_identity_key_or_an_identity_key_with_the_same_name_already_exists), 
                                 android.R.drawable.ic_dialog_alert);
            return;
          }

          finish();
        }
      });
      dialogBuilder.setNegativeButton(R.string.no, null);
      dialogBuilder.show();
    }
  }
	
  @Override
  protected String getScanString() {
    return getString(R.string.scan_to_compare);
  }

  @Override
  protected String getDisplayString() {
    return getString(R.string.get_scanned_to_compare);
  }

  @Override
  protected IdentityKey getIdentityKeyToCompare() {
    return identityKey;
  }

  @Override
  protected IdentityKey getIdentityKeyToDisplay() {
    return identityKey;
  }

  @Override
  protected String getNotVerifiedMessage() {
    return  getString(R.string.warning_the_scanned_key_does_not_match_);
  }

  @Override
  protected String getNotVerifiedTitle() {
    return getString(R.string.not_verified_);
  }

  @Override
  protected String getVerifiedMessage() {
    return getString(R.string.the_scanned_key_matches_);
  }

  @Override
  protected String getVerifiedTitle() {
    return getString(R.string.verified_);
  }
}
