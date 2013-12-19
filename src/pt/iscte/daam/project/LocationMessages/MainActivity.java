package pt.iscte.daam.project.LocationMessages;

import java.util.Calendar;

import pt.iscte.daam.project.LocationMessages.DurationPickerDialog.DurationPickerDialogListener;
import pt.iscte.daam.project.LocationMessages.Menu.MenuCreatedListener;
import pt.iscte.daam.project.LocationMessages.Start.OnStartListener;
import pt.iscte.daam.project.LocationMessages.TimeListDialog.TimeListDialogListener;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.Cursor;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.Contacts;
import android.provider.Settings;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.NotificationCompat;
import android.telephony.PhoneNumberUtils;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends FragmentActivity implements
		TimeListDialogListener, DurationPickerDialogListener,
		MenuCreatedListener, OnStartListener {

	private Handler handler;

	private BroadcastReceiver sendBroadcastReceiver;
	private BroadcastReceiver deliveryBroadcastReceiver;
	private BroadcastReceiver receiveLocationMessages;
	private BroadcastReceiver alarmReceiver;

	private NotificationManager notificationManager;
	private Intent locationServiceIntent;
	private ServiceMessagesType lastLocationUpdate;

	protected SharedPreferences preferences;

	protected String PREFS_NAME = "DB";
	protected String locationProvider = LocationManager.GPS_PROVIDER;
	protected String sosContacNumber;
	protected static final String startFragmentTag = "startfragment";
	protected static final String configureRunFragmentTag = "configureRunFragment";
	protected static final String menuFragmentTag = "menuFragment";

	protected TextView connectionIndicator;
	protected ImageView gpsIcon;

	private Location location;

	private Fragment startFragment;
	private Fragment sosFragment;

	protected boolean terminateApp;
	protected boolean locationFix;
	protected boolean providerEnabled;
	protected boolean isTwoPaneLayout;

	protected int hoursDuration;
	protected int minutesDuration;

	protected static final int OLD_LOCATION_THRESHOLD = 1000 * 60 * 2;
	protected static final int PICK_CONTACT_REQUEST = 0;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (savedInstanceState == null) {

		Log.i("SAVED", "savedInstanceState : " + savedInstanceState);

		setContentView(R.layout.activity_main);

		receiveLocationMessages = new ReceiveLocationBroadcastMessages();
		alarmReceiver = new AlarmBroadcastReceiver();
		notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

		// INICIA A INTENT PARA O SERVIÇO QUE MONITORIZA A LCALIZAÇAO
		locationServiceIntent = new Intent(this, LocationService.class);
		startService(locationServiceIntent);

		// HANDLER PARA ENVIAR MENSAGENS PARA A THREAD DA UI
		handler = new Handler();

		hoursDuration = 0;
		minutesDuration = 0;

		locationFix = false;
		providerEnabled = false;
		terminateApp = false;

		preferences = getSharedPreferences(PREFS_NAME, 0);

		initializebroadcastReceivers();
		registerBroadcastReceiver();

			menuAdapter();
		}

	}

	/**
	 * Creates the menu, considering if the app is running on a tablet or in a
	 * handset
	 */
	private void menuAdapter() {
		// Check that the activity is using the layout version with
		// the fragment_container FrameLayout
		if (findViewById(R.id.fragment_container) != null) {

			isTwoPaneLayout = getResources().getBoolean(R.bool.has_two_panes);

			Fragment menu = new pt.iscte.daam.project.LocationMessages.Menu();

			if (!isTwoPaneLayout) {
				setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

				// Create an instance of ExampleFragment

				// Add the fragment to the 'fragment_container' FrameLayout
				getSupportFragmentManager().beginTransaction()
						.add(R.id.fragment_container, menu, menuFragmentTag)
						.commit();

			} else {
				setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
				getSupportFragmentManager()
						.beginTransaction()
						.add(R.id.fragment_menuFragment_container, menu,
								menuFragmentTag).commit();

				Fragment blank = new Blank();
				getSupportFragmentManager().beginTransaction()
						.add(R.id.fragment_container, blank).commit();

			}

		}

	}

	/**
	 * Registers the broadcast receivers
	 */
	private void registerBroadcastReceiver() {
		// ---when the SMS has been sent---
		registerReceiver(sendBroadcastReceiver, new IntentFilter("SMS_SENT"));

		// ---when the SMS has been delivered---
		registerReceiver(deliveryBroadcastReceiver, new IntentFilter(
				"SMS_DELIVERED"));

		// BROADCASTRECEIVER PARA AS MENSAGENS DO SERVICO DE LOCALIZACAO
		registerReceiver(receiveLocationMessages, new IntentFilter(
				"Location_Broadcast"));

		registerReceiver(alarmReceiver, new IntentFilter("SEND_SMS"));

	}

	/**
	 * Initialize both send and delivered broadcast receivers
	 */
	private void initializebroadcastReceivers() {

		initializeSendBroadcastReceiver();
		initializeDeliveredBroadcastReceiver();
	}

	/**
	 * Creates the broadcast receiver to sent messages
	 */
	private void initializeDeliveredBroadcastReceiver() {
		sendBroadcastReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context arg0, Intent arg1) {
				switch (getResultCode()) {
				case Activity.RESULT_OK:
					Toast.makeText(
							getBaseContext(),
							"Sms sent to "
									+ preferences.getString("sosContactNumber",
											""), Toast.LENGTH_SHORT).show();
					break;
				case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
					Toast.makeText(getBaseContext(), "Generic failure",
							Toast.LENGTH_SHORT).show();
					break;
				case SmsManager.RESULT_ERROR_NO_SERVICE:
					Toast.makeText(getBaseContext(), "No network connection",
							Toast.LENGTH_SHORT).show();
					break;
				case SmsManager.RESULT_ERROR_NULL_PDU:
					Toast.makeText(getBaseContext(), "Null PDU",
							Toast.LENGTH_SHORT).show();
					break;
				case SmsManager.RESULT_ERROR_RADIO_OFF:
					Toast.makeText(getBaseContext(), "Radio off",
							Toast.LENGTH_SHORT).show();
					break;
				}
			}
		};

	}

	/**
	 * Creates the broadcast receiver to delivered text messages
	 */
	private void initializeSendBroadcastReceiver() {
		deliveryBroadcastReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context arg0, Intent arg1) {

				switch (getResultCode()) {
				case RESULT_OK:
					Toast.makeText(
							getBaseContext(),
							"Sms delivered to "
									+ preferences.getString("sosContactNumber",
											""), Toast.LENGTH_SHORT).show();
					break;
				case RESULT_CANCELED:
					Toast.makeText(
							getBaseContext(),
							"Sms not delivered to "
									+ preferences.getString("sosContactNumber",
											""), Toast.LENGTH_SHORT).show();
					break;
				}
			}
		};

	}

	@Override
	protected void onStop() {
		super.onStop();

		// try {
		// IN CASE THE USER WANTS TO CLOSE THE APP
		if (terminateApp) {
			notificationManager.cancelAll();
			unregisterReceiver(sendBroadcastReceiver);
			unregisterReceiver(deliveryBroadcastReceiver);
			unregisterReceiver(receiveLocationMessages);
			unregisterReceiver(alarmReceiver);
			stopService(locationServiceIntent);

		} else {

			// THE APP GOES TO THE BACKGROUND
			if (getSupportFragmentManager().findFragmentByTag(startFragmentTag) != null
					&& getSupportFragmentManager().findFragmentByTag(
							startFragmentTag).isVisible())
				onGoingNotification();
		}
		

	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
	}

	@Override
	protected void onResume() {
		super.onResume();

		if (notificationManager != null)
			notificationManager.cancelAll();

		// IF A LOCATION UPDATE IS AVAILABLE AND THE FRAGMENT RETURNS TO THE
		// FOREGROUND, THE UI IS UPDATED
		if (lastLocationUpdate != null)
			updateUI(lastLocationUpdate);
	}

	// *************************************************************************************************************
	// *************************************************************************************************************
	// *************************************************************************************************************

	/**
	 * Verify if the handset has the features to start the app, specifically the
	 * gps If the GPS is turn off, it will be requested to the user to switch on
	 */
	private void verifyIfGPSIsOnline() {

		LocationManager manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

		if (!manager.isProviderEnabled(locationProvider)) {

			AlertDialog.Builder builder = new AlertDialog.Builder(this);

			builder.setTitle("Location Services");
			builder.setMessage("You need to enable your GPS!");
			builder.setPositiveButton("Ok",
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {

							startActivity(new Intent(
									Settings.ACTION_LOCATION_SOURCE_SETTINGS));
						}
					});
			builder.setNegativeButton("Cancel",
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {

							finish();
						}
					});

			builder.create().show();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.listofitems, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Fragment newFragment;
		FragmentTransaction transaction;

		switch (item.getItemId()) {
		case R.id.about:

			newFragment = new About();

			transaction = getSupportFragmentManager().beginTransaction();

			transaction.setCustomAnimations(R.anim.slide_in_right,
					R.anim.slide_out_left);

			transaction.replace(R.id.fragment_container, newFragment);
			transaction.addToBackStack(null);

			transaction.commit();

			return true;
		case R.id.help:

			newFragment = new Instructions();

			transaction = getSupportFragmentManager().beginTransaction();

			transaction.setCustomAnimations(R.anim.slide_in_right,
					R.anim.slide_out_left);

			transaction.replace(R.id.fragment_container, newFragment);
			transaction.addToBackStack(null);

			transaction.commit();

			return true;
		case R.id.settings:
			startActivity(new Intent(this, Preferences.class));
			return true;
		}

		return super.onOptionsItemSelected(item);
	}

	// **********************************************************************************************************************************
	// *********************************** MENU
	// ******************************************************************************
	// **********************************************************************************************************************************

	/**
	 * When the menu is created, the UI is updated and the hardware required is
	 * verified
	 */
	@Override
	public void onMenuCreatedListener(boolean created) {

		connectionIndicator = (TextView) findViewById(R.id.connectText);
		verifyHardware();
		if (lastLocationUpdate != null)
			updateUI(lastLocationUpdate);

	}

	/**
	 * When the user clicks on the start button in the menu
	 * 
	 * @param v
	 */
	public void onClickStart(View v) {

		// Starts the button animation
		Animation button_gradient = AnimationUtils.loadAnimation(this,
				R.anim.button_alpha);

		v.startAnimation(button_gradient);

		// A Bundle is created to pass the time interval, sos contact number to
		// the configure run fragment
		Bundle bundle = new Bundle();

		// CASO NAO SE TENHA ESCOLHIDO UM TIME BETWEEN SMS PREDEFINIDA É ENVIADO
		// 0
		if (preferences.contains("timeInterval"))
			bundle.putInt("timeInterval", preferences.getInt("timeInterval", 0));
		else
			bundle.putInt("timeInterval", 0);

		if (preferences.contains("sosContactNumber"))
			bundle.putString("sosContactNumber",
					preferences.getString("sosContactNumber", null));

		// Create new fragment and transaction
		Fragment newFragment = new ConfigureRun();

		newFragment.setArguments(bundle);

		showNewFragment(newFragment, configureRunFragmentTag);

	}

	// QUANDO SE ESCOLHE O SOS FRAGMENT NO MENU INICIAL
	public void onClickSos(View v) {

		Animation button_gradient = AnimationUtils.loadAnimation(this,
				R.anim.button_alpha);

		v.startAnimation(button_gradient);

		Bundle bundle = new Bundle();

		if (preferences.contains("sosContactNumber"))
			bundle.putString("sosContactNumber",
					preferences.getString("sosContactNumber", null));

		if (preferences.contains("sosMessageText"))
			bundle.putString("sosMessageText",
					preferences.getString("sosMessageText", null));

		// Create new fragment and transaction
		Fragment newFragment = new Sos();

		newFragment.setArguments(bundle);

		showNewFragment(newFragment, null);

	}

	private void showNewFragment(Fragment newFragment, String fragmenttag) {
		FragmentTransaction transaction = getSupportFragmentManager()
				.beginTransaction();

		transaction.setCustomAnimations(R.anim.slide_in_right,
				R.anim.slide_out_left);

		transaction.replace(R.id.fragment_container, newFragment, fragmenttag);
		transaction.addToBackStack(null);

		// Commit the transaction
		transaction.commit();

	}

	// QUANDO SE CARREGA NOS SETTINGS NO MENU INICIAL
	public void onClickSettings(View v) {

		Animation button_gradient = AnimationUtils.loadAnimation(this,
				R.anim.button_alpha);

		v.startAnimation(button_gradient);

		startActivity(new Intent(this, Preferences.class));

	}

	// ******************************************************************************************
	// ******************************************************************************************
	// ******************************************************************************************

	public void verifyHardware() {

		PackageManager packageManager = getPackageManager();

		if (packageManager.hasSystemFeature(PackageManager.FEATURE_LOCATION)) {

			verifyIfGPSIsOnline();
			providerEnabled = true;

		}
	}

	// ******************************************************************************************
	// ****************************** SOS FRAGMENT
	// **********************************
	// ******************************************************************************************
	// QUANDOS E CARREGA NO BOTAO NO SOS FRAGMENT PARA ESCOLHER UM CONTACTO
	public void onContactPicker(View v) {

		Animation button_gradient = AnimationUtils.loadAnimation(this,
				R.anim.button_alpha);

		v.startAnimation(button_gradient);

		Intent contactPickerIntent = new Intent(Intent.ACTION_PICK,
				Contacts.CONTENT_URI);
		startActivityForResult(contactPickerIntent, PICK_CONTACT_REQUEST);

	}

	// ACTIVITY FOR RESULT PARA A ESCOLHA DO CONTACTO
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

				Log.i(STORAGE_SERVICE, "PhoneIdx: " + phoneIdx);

				if (cursor.moveToFirst()) {

					phone = cursor.getString(phoneIdx);

					((TextView) findViewById(R.id.phoneNumberSosFragment))
							.setText(phone);

					sosContacNumber = phone;

					SharedPreferences.Editor editor = preferences.edit();

					editor.putString("sosContactNumber", phone);

					editor.commit();

				} else {

				}
			} catch (Exception e) {

			} finally {
				if (cursor != null) {
					cursor.close();
				}
			}
		}
	}

	// METODO QUANDO SE CLICA NO BOTAO PARA ENVIAR MENSAGEM NO SOSFRAGMENET
	public void onClickSosMessageButton(View v) {

		Animation button_gradient = AnimationUtils.loadAnimation(this,
				R.anim.button_alpha);

		v.startAnimation(button_gradient);

		TextView contactId = (TextView) findViewById(R.id.phoneNumberSosFragment);

		final String phoneNo = contactId.getText().toString();

	
		if (phoneNo != "") {

			// if the location is not fixed, a message will be sent
			// automatically when the location is received
			if (location == null || oldLocation()) {

				Runnable runnable = new Runnable() {

					@Override
					public void run() {

						// Handler to send the Toast to UI thread
						handler.post(new Runnable() {

							@Override
							public void run() {
								Toast.makeText(
										getApplicationContext(),
										"Fixing location, your message will be sent automatically!",
										Toast.LENGTH_LONG).show();
								return;
							}
						});

						do {
							// while the location is not received, ou se a que
							// existir ja for mto antiga

							try {
								Thread.sleep(500);
							} catch (InterruptedException e) {
								e.printStackTrace();
							}

						} while (location == null || oldLocation());

						sendSms(phoneNo, location, SmsType.SOSSMS);

						return;
					}
				};

				Thread newThread = new Thread(runnable);

				newThread.start();

			} else {

				sendSms(phoneNo, location, SmsType.SOSSMS);

			}
		} else if (phoneNo.equals("")) {

			Toast.makeText(getApplicationContext(),
					"You need to choose a contact!", Toast.LENGTH_SHORT).show();

		}

	}

	// verifica se a localização tem mais de dois minutos
	private boolean oldLocation() {
		if (location != null) {
			long timeDelta = location.getTime()
					- Calendar.getInstance().getTimeInMillis();

			boolean isSignificantlyOlder = timeDelta < -OLD_LOCATION_THRESHOLD;

			if (isSignificantlyOlder)
				return true;
			else
				return false;
		}
		return false;
	}

	private void sendSms(String phoneNumber, Location location, SmsType smsType) {


		try {
			if (location != null) {
				PendingIntent sentPendingIntent = PendingIntent.getBroadcast(
						this, 0, new Intent("SMS_SENT"), 0);

				PendingIntent deliveredPendingIntentI = PendingIntent
						.getBroadcast(this, 0, new Intent("SMS_DELIVERED"), 0);

				SmsManager smsManager = SmsManager.getDefault();

				String typeOfSms;

				if (smsType.equals(SmsType.SOSSMS))
					typeOfSms = preferences.getString("sosMessageText", "");
				else
					typeOfSms = preferences.getString("okMessageText", "");

				String sms = typeOfSms + "! Help me please!\n" + "Longitude: "
						+ location.getLongitude() + ", Latitude: "
						+ location.getLatitude()
						+ "\nGoogle Maps: http://maps.google.com/maps?z=18&q="
						+ location.getLatitude() + ","
						+ location.getLongitude();

				smsManager.sendTextMessage(phoneNumber, null, sms,
						sentPendingIntent, deliveredPendingIntentI);

			}
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	// ******************************************************************************************
	// *************************** InitialStart
	// ***************************************
	// ******************************************************************************************

	// quando se escolhe qual o tempo para o exercicio
	public void onClickMaxTime(View v) {

		Animation button_gradient = AnimationUtils.loadAnimation(this,
				R.anim.button_alpha);

		v.startAnimation(button_gradient);

		DialogFragment newFragment = new DurationPickerDialog();
		newFragment.show(getSupportFragmentManager(), "duration");

	}

	@Override
	public void onFinishDurationPickerEditDialog(int hourOfDay, int minute) {

		TextView textView = (TextView) findViewById(R.id.maxTimeIS);

		textView.setText(hourOfDay + "h" + minute + "min");

		hoursDuration = hourOfDay;
		minutesDuration = minute;

	}

	// Inicia fragment para o dialog que permite escolher o tempo para a
	// actualizaçao da localização

	public void onClickIntervalBetweenMessages(View v) {

		Animation button_gradient = AnimationUtils.loadAnimation(this,
				R.anim.button_alpha);

		v.startAnimation(button_gradient);

		DialogFragment newFragment = new TimeListDialog();
		newFragment.show(getSupportFragmentManager(), "timeList");

	}

	@Override
	public void onFinishTimeListEditDialog(String inputText) {

		// COMEÇAR DAQUI, GUARDO VALOR NA SHAREDPREFERENCES OU NUMA VARIAVEL
		// GLOBAL
		// PRECISO DE VERIFICAR SE O TEMPO E >0 E DEPOIS COMEÇAR A APLICAÇAO

		SharedPreferences.Editor editor = preferences.edit();

		editor.putInt("timeInterval", Integer.parseInt(inputText));

		editor.commit();

		// DURATION ESTA EM SEGUNDOS

		TextView textView = (TextView) findViewById(R.id.smsTime);

		textView.setText((Integer.parseInt(inputText) / 60) + "min");

	}

	// ******************************************************************************************
	// ******************************** INICIAL_START_FRAGMENT
	// *************************************
	// ******************************************************************************************

	// QUANDO O USER CARREGA NO START DO INICIAL_START_FRAGMENT
	// REPLACE ONE FRAGMENT BY OTHER
	public void onClickStartFragment(View v) {

		Animation button_gradient = AnimationUtils.loadAnimation(this,
				R.anim.button_alpha);

		v.startAnimation(button_gradient);

		if ((minutesDuration > 0 || hoursDuration > 0)) {
			if (sosContacNumber != null
					|| preferences.getString("sosContactNumber", null) != null) {

				startFragment = new Start();

				Bundle bundle = new Bundle();

				bundle.putInt("hoursTime", hoursDuration);
				bundle.putInt("minutesTime", minutesDuration);

				// PRECISO FZR RESET PARA QUE QUANDO RECOMECE ESTE FRAGMENT NAO
				// INICIE COM OS VALORES ANTIGOS
				hoursDuration = 0;
				minutesDuration = 0;
				startFragment.setArguments(bundle);

				showNewFragment(startFragment, startFragmentTag);

				enableMenuButtons(false);

			} else
				Toast.makeText(getApplicationContext(),
						"You need to select a contact number",
						Toast.LENGTH_SHORT).show();

		} else
			Toast.makeText(getApplicationContext(),
					"You need to select a duration to your activity",
					Toast.LENGTH_SHORT).show();

	}

	public void onClickSosButtonInStartFragment(View v) {

		Animation button_gradient = AnimationUtils.loadAnimation(this,
				R.anim.button_alpha);

		v.startAnimation(button_gradient);

		// CANCELA O ALARM DAS MENSAGENS
		cancelSmsAlarm();

		((Start) getSupportFragmentManager()
				.findFragmentByTag(startFragmentTag)).interruptTheMainThread();

		Bundle bundle = new Bundle();

		if (preferences.contains("sosContactNumber"))
			bundle.putString("sosContactNumber",
					preferences.getString("sosContactNumber", null));

		if (preferences.contains("sosMessageText"))
			bundle.putString("sosMessageText",
					preferences.getString("sosMessageText", null));

		// Create new fragment and transaction
		sosFragment = new Sos();

		sosFragment.setArguments(bundle);

		FragmentManager fragmentManager = getSupportFragmentManager();

		int entryCount = fragmentManager.getBackStackEntryCount();

		while (entryCount-- >= 0) {
			fragmentManager.popBackStack();
		}

		showNewFragment(sosFragment, null);
	}

	@Override
	public void onSmsToBeSent(SmsType smsType) {

		String phoneNumber = preferences.getString("sosContactNumber", null);
		if (!PhoneNumberUtils.isGlobalPhoneNumber(phoneNumber))
			phoneNumber = sosContacNumber;
		if (phoneNumber != null) {

			sendSms(phoneNumber, location, smsType);

		} else {
		}
	}

	private void notification() {
		NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(
				this).setSmallIcon(R.drawable.ic_launcher)
				.setContentTitle("SOS").setContentText("Finished!")
				.setTicker("Time's up").setAutoCancel(true);

		// Creates an explicit intent for an Activity in your app

		PendingIntent resultPendingIntent = PendingIntent.getActivity(this, 0,
				getIntent(), Notification.DEFAULT_VIBRATE);
		mBuilder.setContentIntent(resultPendingIntent);
		NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

		// mId allows you to update the notification later on.
		if (!isTwoPaneLayout)
			mBuilder.setVibrate(new long[] { 100, 200, 100, 500 });

		mNotificationManager.notify(3, mBuilder.build());

	}

	private void gpsFixNotification() {

		startGpsAnimation();
		if (!providerEnabled) {
			startGpsAnimation();

			connectionIndicator.setText("Not connected");

		} else if (providerEnabled || !locationFix) {
			startGpsAnimation();
			connectionIndicator.setText("Connected, fixing location");

		}
		if (locationFix) {
			stopGpsAnimation();
			connectionIndicator.setText("Connected, location fixed");

		}
	}

	void startGpsAnimation() {

		gpsIcon = (ImageView) findViewById(R.id.imageView4);
		if (gpsIcon != null) {
			Animation myFadeInAnimation = AnimationUtils.loadAnimation(this,
					R.anim.gps_icon_animation);
			gpsIcon.startAnimation(myFadeInAnimation);
		}
	}

	void stopGpsAnimation() {
		if (gpsIcon != null) {

			gpsIcon.clearAnimation();
		}
	}

	/**
	 * When the time is over the app launches a notification, enables menu
	 * button, in case of tablet, and cancel the messages alarm
	 */
	@Override
	public void onTimeFinished() {
		notification();

		enableMenuButtons(true);

		((AlarmBroadcastReceiver) alarmReceiver)
				.cancelAlarm(getApplicationContext());

	}

	/**
	 * Enables the SOS and Settings button, when the app is running in a tablet
	 * 
	 * @param command
	 */
	private void enableMenuButtons(boolean command) {
		if (isTwoPaneLayout) {

			LinearLayout sosLayout = (LinearLayout) findViewById(R.id.sosLayout);
			LinearLayout settings_button_layout = (LinearLayout) findViewById(R.id.settings_button_layout);

			if (command) {
				sosLayout.setClickable(command);
				settings_button_layout.setClickable(command);
			} else {
				sosLayout.setClickable(command);
				settings_button_layout.setClickable(command);
			}
		}
	}

	/**
	 * When the run starts the activity update the location and sets the sms
	 * alarm
	 */
	@Override
	public void onStartActivityCreated(int timeInterval) {

		if (lastLocationUpdate != null && !isTwoPaneLayout) {
			connectionIndicator = (TextView) findViewById(R.id.onePaneConnectTextStart);

			updateUI(lastLocationUpdate);

		} else {

			updateUI(lastLocationUpdate);

		}

		// *1000 to convert to milliseconds
		if (timeInterval > 0)
			((AlarmBroadcastReceiver) alarmReceiver).setAlarm(
					getApplicationContext(),
					(preferences.getInt("timeInterval", 0) * 1000),
					SmsType.NormalSMS);

	}

	@Override
	public void onGoingNotification() {

		// FROM GOOGLE DEVELOPERS
		NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(
				this).setSmallIcon(R.drawable.ic_launcher)
				.setContentTitle("SOS").setContentText("SOS Running!")
				.setTicker("SOS minimized, but running").setAutoCancel(true);

		mBuilder.setOngoing(true);

		// Creates an explicit intent for an Activity in your app

		PendingIntent resultPendingIntent = PendingIntent.getActivity(this, 0,
				getIntent(), Notification.DEFAULT_VIBRATE);
		mBuilder.setContentIntent(resultPendingIntent);
		NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		// mId allows you to update the notification later on.
		mNotificationManager.notify(3, mBuilder.build());

	}

	public class ReceiveLocationBroadcastMessages extends BroadcastReceiver {

		public ReceiveLocationBroadcastMessages() {
		}

		@Override
		public void onReceive(Context context, Intent intent) {

			if (intent.getExtras() != null) {
				if (intent.getExtras().containsKey("location")) {
					location = (Location) intent.getExtras().get("location");
					updateUI(ServiceMessagesType.LOCATION_UPDATE);

					Fragment startFrag = getSupportFragmentManager()
							.findFragmentByTag(startFragmentTag);

					if (startFrag != null) {

						((Start) startFrag).updateLocation(location);
					}

				} else if (intent.getExtras().containsKey("state")) {

					if (intent.getExtras().get("state")
							.equals(ServiceMessagesType.DISABLED)) {

						updateUI(ServiceMessagesType.DISABLED);
					} else if (intent.getExtras().get("state")
							.equals(ServiceMessagesType.ENABLED))

						updateUI(ServiceMessagesType.ENABLED);

				}

			}
		}

	}

	/**
	 * Updates the user interface, considering if the user is running in a
	 * tablet or a handset
	 * 
	 * @param locationUpdate
	 */
	private void updateUI(ServiceMessagesType locationUpdate) {

		lastLocationUpdate = locationUpdate;

		// IF the screen has the startFragment visible we should update the
		// loocation text views
		if (getSupportFragmentManager().findFragmentByTag(startFragmentTag) != null
				&& getSupportFragmentManager().findFragmentByTag(
						startFragmentTag).isVisible() == true) {

			switch (locationUpdate) {
			case LOCATION_UPDATE:
				if (location != null) {
					locationFix = true;

					gpsFixNotification();

					double longitude = location.getLongitude();

					double latitude = location.getLatitude();
					TextView longitudeTExt = (TextView) findViewById(R.id.longitude);

					TextView latitudeText = (TextView) findViewById(R.id.latitude);

					longitudeTExt.setText("" + longitude + "º");
					latitudeText.setText("" + latitude + "º");

				}
				break;
			case DISABLED:
				providerEnabled = false;
				locationFix = false;
				gpsFixNotification();

				break;

			case ENABLED:

				providerEnabled = true;
				gpsFixNotification();
				break;

			default:
				break;
			}

			// In case the menu fragment is visible, the location indicator is
			// the only to be updated
		} else if (getSupportFragmentManager().findFragmentByTag(
				menuFragmentTag) != null
				&& getSupportFragmentManager().findFragmentByTag(
						menuFragmentTag).isVisible() == true) {

			switch (locationUpdate) {
			case LOCATION_UPDATE:
				if (location != null) {

					locationFix = true;

					gpsFixNotification();

				}
				break;
			case DISABLED:

				providerEnabled = false;
				locationFix = false;
				gpsFixNotification();

				break;
			case ENABLED:

				providerEnabled = true;
				gpsFixNotification();
				break;

			default:
				break;
			}

		}
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {

		if (keyCode == KeyEvent.KEYCODE_BACK) {

			// && !two_pane_layout: this condition is needed because when we
			// have the two pane layout
			// the start fragment and menu fragment are both visible
			if (getSupportFragmentManager().findFragmentByTag(menuFragmentTag) != null
					&& getSupportFragmentManager().findFragmentByTag(
							menuFragmentTag).isVisible() == true
					&& !isTwoPaneLayout) {

				terminateApp = true;

			} else if (getSupportFragmentManager().findFragmentByTag(
					startFragmentTag) != null
					&& getSupportFragmentManager().findFragmentByTag(
							startFragmentTag).isVisible() == true) {

				alertDialogOnBackPressed();

			}

		}

		return super.onKeyDown(keyCode, event);
	}

	/**
	 * When the back button is pressed, when the start fragment is visible, a
	 * dialog is presented to the user
	 * 
	 */
	public void alertDialogOnBackPressed() {

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage("Do you want to exit?")
				.setPositiveButton("Yes",
						new DialogInterface.OnClickListener() {

							public void onClick(DialogInterface dialog, int id) {

								Start startFrag = (Start) getSupportFragmentManager()
										.findFragmentByTag(startFragmentTag);

								// SO WHEN THE BACK BUTTON IS PRESSED, WHEN THE
								// START FRAGMENT IS VISIBLE,
								// THE APP SHOULD CLOSE
								startFrag.interruptTheMainThread();

								cancelSmsAlarm();

								// ENABLE MENU BUTTONS
								enableMenuButtons(true);

								onBackPressed();

							}
						})
				.setNegativeButton("No", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						return;
					}
				});

		builder.create();
		builder.show();
	}

	/**
	 * Get last location
	 * 
	 * @return
	 */
	public Location getLocation() {
		return location;
	}

	@Override
	public void cancelSmsAlarm() {
		((AlarmBroadcastReceiver) alarmReceiver)
				.cancelAlarm(getApplicationContext());

	}

}
