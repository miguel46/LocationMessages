package pt.iscte.daam.project.LocationMessages;

import java.util.Arrays;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.Contacts;

import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;



public class Preferences extends PreferenceActivity {

	private String PREFS_NAME = "DB";
	private SharedPreferences settings;

	private static final int PICK_CONTACT_REQUEST = 0;

	private Preference sosContactNumber;

	private EditTextPreference sosMessageText;
	private EditTextPreference okMessageText;

	private ListPreference listPreference;

	//WE USE SOME DEPRECATED METHODS TO ALLOW COMPABILITY WITH THE DEVICES SINCE 2.3,
	//THE DEVICES UNDER 3.0 DO NOT SUPPORT PREFERENCEFRAGMENT
	@SuppressWarnings("deprecation")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		addPreferencesFromResource(R.xml.preferences);

		settings = getSharedPreferences(PREFS_NAME, 0);

		sosContactNumber = findPreference("sosContactNumber");

		sosMessageText = (EditTextPreference) findPreference("sosMessageText");
		okMessageText = (EditTextPreference) findPreference("okMessageText");

		listPreference = (ListPreference) findPreference("timer_interval");

		addListenersToComponents();

		restorePreferences();

	}

	@Override
	public void onStop() {
		super.onStop();
	}

	/**
	 * Add listeners to when the preferences are modified
	 */
	private void addListenersToComponents() {

		setListenerToIntervalBetweenMessages();
		setListenerToSOSContactNumber();
		setListenerToSOSMessageText();
		setListenerToOKMessageText();
	}

	/**
	 * Add listener to when the user change the ok message text
	 */
	private void setListenerToOKMessageText() {
		okMessageText
				.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

					@Override
					public boolean onPreferenceChange(Preference preference,
							Object newValue) {

						okMessageText.setSummary((CharSequence) newValue);

						return true;
					}
				});

	}

	/**
	 * Set a listener to when the user change the sos message text
	 */
	private void setListenerToSOSMessageText() {
		sosMessageText
				.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

					@Override
					public boolean onPreferenceChange(Preference preference,
							Object newValue) {

						sosMessageText.setSummary((CharSequence) newValue);

						return true;
					}
				});

	}

	/**
	 * Restore the preferences when the user restarts the application
	 */
	private void restorePreferences() {

		// RESTORE SOS CONTACT NUMBER
		sosContactNumber.setSummary(settings.getString("sosContactNumber",
				sosContactNumber.getSummary().toString()));

		// RESTORE SOSMESSAGE TEXT
		sosMessageText.setSummary(settings.getString("sosMessageText",
				(String) sosMessageText.getSummary()));

		// RESTORE OKMESSAGE TEXT
		okMessageText.setSummary(settings.getString("okMessageText",
				(String) okMessageText.getSummary()));

		int time = settings.getInt("timeInterval", 0);

		if (time != 0) {
			listPreference.setSummary("" + ((time / 60) + " minutes"));
			listPreference.setValueIndex(Arrays.asList(
					getResources().getStringArray(
							R.array.settings_time_phone_value)).indexOf(
					Integer.toString(time)));
		} else {
			listPreference.setValueIndex(0);
			listPreference
					.setSummary(""
							+ ((Integer.parseInt(listPreference.getValue()) / 60) + " minutes"));
		}
	}

	/**
	 * Starts the contact picker when the user click in the phone chooser
	 */
	private void setListenerToSOSContactNumber() {
		sosContactNumber
				.setOnPreferenceClickListener(new OnPreferenceClickListener() {

					@Override
					public boolean onPreferenceClick(Preference preference) {
						Intent contactPickerIntent = new Intent(
								Intent.ACTION_PICK, Contacts.CONTENT_URI);
						startActivityForResult(contactPickerIntent,
								PICK_CONTACT_REQUEST);
						return true;
					}
				});
	}

	/**
	 * Add a listener to when the user change the time interval between messages
	 * and sets the summary
	 */
	private void setListenerToIntervalBetweenMessages() {
		listPreference
				.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

					@Override
					public boolean onPreferenceChange(Preference preference,
							Object newValue) {

						String value = (String) newValue;

						listPreference.setSummary(""
								+ ((Integer.parseInt(value)) / 60) + " minutes");

						return true;
					}
				});

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	
		return super.onOptionsItemSelected(item);
	}

	

	@Override
	public void onActivityResult(int reqCode, int resultCode, Intent data) {
		super.onActivityResult(reqCode, resultCode, data);

	if (reqCode == PICK_CONTACT_REQUEST && resultCode == RESULT_OK) {

			Cursor cursor = null;
			String phone = "";
			try {

				Uri result = data.getData();

				// get the contact id from the Uri
				String id = result.getLastPathSegment();

				cursor = getContentResolver().query(Phone.CONTENT_URI, null,
						Phone.CONTACT_ID + "=?", new String[] { id }, null);

				int phoneIdx = cursor.getColumnIndex(Phone.DATA);

				if (cursor.moveToFirst()) {

					phone = cursor.getString(phoneIdx);

					((TextView) findViewById(R.id.phoneNumberSosFragment))
							.setText(phone);

				} else {

				}
			} catch (Exception e) {

			} finally {
				if (cursor != null) {
					cursor.close();
				}

				sosContactNumber.setSummary(phone);

			}
		}
	}

	@Override
	public void onPause() {
		super.onPause();

		//SAVE ALL THE USERS PREFERENCES
		SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
		SharedPreferences.Editor editor = settings.edit();

		editor.putString("sosContactNumber", sosContactNumber.getSummary()
				.toString());
		editor.putString("sosMessageText", sosMessageText.getSummary()
				.toString());
		editor.putString("okMessageText", okMessageText.getSummary().toString());
		if (listPreference.getValue() != null)
			editor.putInt("timeInterval",
					Integer.parseInt(listPreference.getValue()));

		editor.commit();

	}
}
