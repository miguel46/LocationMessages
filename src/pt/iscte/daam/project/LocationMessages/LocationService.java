package pt.iscte.daam.project.LocationMessages;

//import java.util.Collection;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
//import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.os.IBinder;

public class LocationService extends Service {

	MyLocationListener myLocationListener;
	GpsStatus.Listener myGpsStatusListener;
	LocationManager locationManager;
	String locationProvider;
	final static int TIME_BETWEEN_LOCATION_UPDATES = 100;
	final static int DISTANCE_BETWEEN_LOCATION_UPDATES = 1;

	public LocationService() {
	}

	@Override
	public void onCreate() {

		this.locationProvider = LocationManager.GPS_PROVIDER;

		myLocationListener = new MyLocationListener();
		myGpsStatusListener = new MyGpsStatusListener();
		locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

	}

	@Override
	public void onDestroy() {

		locationManager.removeUpdates(myLocationListener);
	}

	@Override
	public void onStart(Intent intent, int startid) {

		if (locationManager.isProviderEnabled(locationProvider)) {

			sendBroadcast(new Intent("Location_Broadcast").putExtra("state",
					ServiceMessagesType.ENABLED));
		} else {
			sendBroadcast(new Intent("Location_Broadcast").putExtra("state",
					ServiceMessagesType.DISABLED));

		}

		// REQUEST LOCATION UPDATES BETWEEN THE SPECIFIED INTERVALS
		locationManager.requestLocationUpdates(locationProvider,
				TIME_BETWEEN_LOCATION_UPDATES,
				DISTANCE_BETWEEN_LOCATION_UPDATES, myLocationListener);

	}

	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}


	private class MyGpsStatusListener implements GpsStatus.Listener {

		//int numberOfSatellites = 0;

		@Override
		public void onGpsStatusChanged(int event) {

			if (event == GpsStatus.GPS_EVENT_SATELLITE_STATUS) {


			}
		}
	}

	public class MyLocationListener implements LocationListener {

		@Override
		public void onLocationChanged(Location location) {

			if (location != null) {
				sendBroadcast(new Intent("Location_Broadcast").putExtra(
						"location", location));
			}
		}

		@Override
		public void onProviderDisabled(String provider) {

			sendBroadcast(new Intent("Location_Broadcast").putExtra("state",
					ServiceMessagesType.DISABLED));

		}

		@Override
		public void onProviderEnabled(String provider) {

			sendBroadcast(new Intent("Location_Broadcast").putExtra("state",
					ServiceMessagesType.ENABLED));
		}

		@Override
		public void onStatusChanged(String provider, int status, Bundle extras) {

			switch (status) {
			case LocationProvider.TEMPORARILY_UNAVAILABLE:

//				sendBroadcast(new Intent("Location_Broadcast").putExtra(
//						"state", ServiceMessagesType.DISABLED));
				break;
			}
		}
	}
}
