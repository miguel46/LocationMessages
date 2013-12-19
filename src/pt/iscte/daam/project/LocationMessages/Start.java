package pt.iscte.daam.project.LocationMessages;

import java.util.Timer;
import java.util.TimerTask;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.location.Location;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.os.Vibrator;
import android.support.v4.app.Fragment;
import android.support.v4.app.NavUtils;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Chronometer;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

public class Start extends Fragment {

	Chronometer chronometer;
	long elapsedTime=0;
	private String PREFS_NAME = "DB";
	SharedPreferences preferences;
	int timeInterval;
	long duration;
	TextView connectionIndicator;
	Thread startMainThread;
	boolean isToInterruptTheMainThread;
	Timer timerForSendingMessages;
	private int hoursTime;
	private int minutesTime;
	private MediaPlayer mediaPlayer;
	Timer warningTimer;
	Handler handler;
	Vibrator vibrator;
	OnStartListener onStartEventsListener;
	private boolean isToExitTheFragment;
	Location location;

	// MINIMUM TIME TO THE WARNING MECHANISM, WHEN THE RUN ENDS, IS ACTIVE
	//THIS TIME IS IN SECONDS
	private static final long  WARNING_TIME_TRESHOLD = 5 * 60;

	
	// MINIMUM_TIME_BETWEEN_LOCATIONS IN MILLISECONDS
	private static final long MINIMUM_TIME_BETWEEN_LOCATIONS = 20 * 60 * 1000;

	// MINIMUN_DISTANCE_BETWEEN_LOCATIONS IN METERS
	private static final int MINIMUN_DISTANCE_BETWEEN_LOCATIONS = 100;

	//PERIODICITY OF TIMER TASK IN THE WARNING MECHANISM
	private long WARNING_TIMER_TASK_TIME = 1000;

	
	private AlertDialog timesOverDialog;
	
	/**
	 * Listener to communicate with the activity Allows to inform when a message
	 * is to be sent, when the time finishes, when the start activity is created
	 * and when a notification is requested to show
	 */
	public interface OnStartListener {
		void onSmsToBeSent(SmsType smsType);

		void onTimeFinished();

		void onStartActivityCreated(int timeInterval);

		void onGoingNotification();

		void cancelSmsAlarm();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		// Inflate the layout for this fragment

		return inflater.inflate(R.layout.start_layout, container, false);
	}
	

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        setRetainInstance(true);

		isToExitTheFragment = false;
		isToInterruptTheMainThread = false;

		handler = new Handler(getActivity().getMainLooper());

		onStartEventsListener = (OnStartListener) getActivity();

		preferences = getActivity().getSharedPreferences(PREFS_NAME, 0);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		// SETS THE ELEMENTS IN THE LAYOUT
		TextView maxTimeText = (TextView) getActivity().findViewById(
				R.id.maxTimeTextView);

		TextView smsTimeText = (TextView) getActivity().findViewById(
				R.id.smsTimeTextView);

		RelativeLayout linearLayout = (RelativeLayout) getActivity()
				.findViewById(R.id.endButtonLayoutStartFragment);

		chronometer = (Chronometer) getActivity().findViewById(
				R.id.Chronometer01);

		mediaPlayer = MediaPlayer.create(getActivity(), R.raw.alarmsound);

		Bundle bundle = getArguments();

		hoursTime = bundle.getInt("hoursTime");
		minutesTime = bundle.getInt("minutesTime");

		timeInterval = preferences.getInt("timeInterval", 0);

		duration = (hoursTime * 3600 + minutesTime * 60) * 1000;

		endButtonLayoutStartListener(linearLayout);

		maxTimeText.setText(hoursTime + "h" + minutesTime + "m");

		//DIVIDED FOR 60 TO GET THE NUMBER IN SECONDS
		smsTimeText.setText(Double.valueOf(Math.ceil((timeInterval / 60))).intValue() + " min");

		onStartEventsListener.onStartActivityCreated(timeInterval);

		chronometer.start();

		mainThread.start();
	}

	Thread mainThread = new Thread(new Runnable() {

		@Override
		public void run() {
			// WHILE THE CLOCK IS COUNTING
			// IF WE FORCE THE APP TO CLOSE, ON BACKBUTTON, EXIT OR SOSMESSAGE
			// THE THREAD STOPS
			while (elapsedTime < duration && !isToInterruptTheMainThread) {

				elapsedTime = (SystemClock.elapsedRealtime() - chronometer
						.getBase());

				if (((MainActivity) getActivity()) != null) {

					updateLocation(((MainActivity) getActivity()).getLocation());
				}

				try {
					Thread.sleep(700);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}

			stopRun();
			return;
		}

	});

	/**
	 * Stops the events in the runner thread if is to finish the app, or goes to background without stop anything
	 */
	private void stopRun() {

		chronometer.stop();

		// IF THE APP NOT GO TO THE BACKGROUND, THE isToInterruptTheMainThread
		// is set to true when we want the
		// APP IN BACKGROUND
		if (!isToInterruptTheMainThread) {

			mediaPlayer.setLooping(true);
			mediaPlayer.start();

			vibratorPattern();

			isToInterruptTheMainThread = true;

			if (!isToExitTheFragment) {
				onStartEventsListener.onTimeFinished();
				onTimeFinish();
			}
		}
	}

	/**
	 * Verify is the current location is critical or not
	 * @param recentLocation
	 */
	public void updateLocation(Location recentLocation) {

		if (recentLocation != null) {
			if (location == null)
				location = recentLocation;
			else {

				if (recentLocation.distanceTo(location) < MINIMUN_DISTANCE_BETWEEN_LOCATIONS
						&& (recentLocation.getTime() - location.getTime()) > MINIMUM_TIME_BETWEEN_LOCATIONS) {

					handler.post(new Runnable() {

						@Override
						public void run() {
							Toast.makeText(
									getActivity(),
									"SOS You do not move during "
											+ MINIMUM_TIME_BETWEEN_LOCATIONS
											+ "  milliseconds",
									Toast.LENGTH_LONG).show();

						}
					});

					onStartEventsListener.onSmsToBeSent(SmsType.SOSSMS);
					interruptTheMainThread();
					isToExitTheFragment = true;
				}
			}
		}

	}

	/**
	 * SOS vibration pattern
	 */
	private void vibratorPattern() {

		vibrator = (Vibrator) getActivity().getApplicationContext()
				.getSystemService(Context.VIBRATOR_SERVICE);
		int vibrate = 500;
		int vibration_gap = 200;
		
		long[] pattern = { 0, vibrate, vibration_gap, vibrate, vibrate, vibration_gap };

		vibrator.vibrate(pattern, 0);

	}

	/**
	 * Listener to finish the app in the end button layout
	 * 
	 * @param linearLayout
	 */
	private void endButtonLayoutStartListener(RelativeLayout linearLayout) {
		linearLayout.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {


				AlertDialog.Builder builder = new AlertDialog.Builder(
						getActivity());
				builder.setMessage("Do you want to exit?")
						.setPositiveButton("Yes",
								new DialogInterface.OnClickListener() {

									public void onClick(DialogInterface dialog,
											int id) {
										isToExitTheFragment = true;
										getActivity().onBackPressed();
										
										onStartEventsListener.cancelSmsAlarm();

									}
								})
						.setNegativeButton("No",
								new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog,
											int id) {
										return;
									}
								});

				builder.create();
				builder.show();
			}
		});

	}

	/**
	 * Start the warning mechanisms when the clock stops
	 */
	private void onTimeFinish() {

		warningTimer = new Timer();

		startWarningTimer();

		createAlertDialog();

	}

	/**
	 * Shows a dialog to the user to check if he is fine
	 */
	private void createAlertDialog() {
		this.handler.post(new Runnable() {

			@Override
			public void run() {
				
				
				
				AlertDialog.Builder builder = new AlertDialog.Builder(
						getActivity());
				builder.setMessage("Time's over").setPositiveButton("I'm ok!",
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {

								cancelWarningMechanisms();
								isToExitTheFragment = true;
								getActivity().onBackPressed();
							}
						});
				
				builder.create();
				timesOverDialog=builder.show();
				
			}

		});
	}

	/**
	 * Starts a timer while the alert dialog is showed, if the user not answer
	 * to this alarm a SOS message is send
	 */
	private void startWarningTimer() {
		warningTimer.scheduleAtFixedRate(new TimerTask() {
			int counter = 0;

			@Override
			public void run() {
				if (counter < WARNING_TIME_TRESHOLD) {
					counter++;
				} else {

					OnStartListener smsTimerTaskListener = (OnStartListener) getActivity();
					smsTimerTaskListener.onSmsToBeSent(SmsType.SOSSMS);

					handler.post(new Runnable() {

						@Override
						public void run() {
							
							timesOverDialog.dismiss();
							
							AlertDialog.Builder builder = new AlertDialog.Builder(
									getActivity());
							
							builder.setMessage("YOU DO NOT ANSWER TO THE WARNING ALARM").setPositiveButton("Ok",
									new DialogInterface.OnClickListener() {
										public void onClick(DialogInterface dialog, int id) {

											cancelWarningMechanisms();
											isToExitTheFragment = true;
											getActivity().onBackPressed();
										}
									});

							
							builder.create();
							builder.show();
							
						}
					});

					cancelWarningMechanisms();
					return;
				}
			}
			//THIS TIMERTASK OCCURS EVERY SECOND
		}, 0, WARNING_TIMER_TASK_TIME );

	}
	
	public void interruptTheMainThread(){
		isToInterruptTheMainThread = true;
	}

	/**
	 * Stops the warning timer, the alarm sound and the vibrator
	 */
	private void cancelWarningMechanisms() {
		warningTimer.cancel();
		mediaPlayer.stop();
		vibrator.cancel();

	}

	@Override
	public void onPause() {
		super.onPause();

	}

	@Override
	public void onStop() {
		super.onStop();

		if (isToExitTheFragment) {
			interruptTheMainThread();
			
			duration = 0;
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
	
			NavUtils.navigateUpTo(getActivity(), null);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
}
