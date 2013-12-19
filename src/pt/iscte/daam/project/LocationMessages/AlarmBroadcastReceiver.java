package pt.iscte.daam.project.LocationMessages;

import pt.iscte.daam.project.LocationMessages.Start.OnStartListener;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;


public class AlarmBroadcastReceiver extends BroadcastReceiver {

	private OnStartListener onStartEventsListener;

	@Override
	public void onReceive(Context context, Intent intent) {

		onStartEventsListener = (OnStartListener) context;

		if (intent.getAction().equals("SEND_SMS")) {
			onStartEventsListener.onSmsToBeSent(SmsType.NormalSMS);
		}
	}

	/**
	 * Set a alarm to send sms between a specified time interval
	 * @param context
	 * @param duration
	 * @param normalsms
	 */
	public void setAlarm(Context context, long duration, SmsType normalsms) {
		
		
		AlarmManager am = (AlarmManager) context
				.getSystemService(Context.ALARM_SERVICE);
		
		PendingIntent pi = PendingIntent.getBroadcast(context, 0, new Intent("SEND_SMS"), 0);
		
		//ALARM TO SEND SMS BETWEEN A TIME INTERVAL
		am.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), duration, pi);
	}

	/**
	 * Cancel the active alarm, that is used to send sms
	 * @param context
	 */
	public void cancelAlarm(Context context) {

		PendingIntent sender = PendingIntent.getBroadcast(context, 0, new Intent("SEND_SMS"), 0);
		AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		alarmManager.cancel(sender);
	}
}