package pt.iscte.daam.project.LocationMessages;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.NavUtils;
import android.telephony.PhoneNumberUtils;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class ConfigureRun extends Fragment {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);


	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		// Inflate the layout for this fragment

		return inflater
				.inflate(R.layout.inicial_start_layout, container, false);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		Bundle bundle = getArguments();

		if (bundle != null) {

			TextView smsTime = (TextView) getActivity().findViewById(
					R.id.smsTime);

			TextView phoneNumberSosFragment = (TextView) getActivity()
					.findViewById(R.id.phoneNumberSosFragment);
			
			
			if (bundle.containsKey("timeInterval")) 
				smsTime.setText((bundle.getInt("timeInterval") / 60) + " min");
			

			if (bundle.containsKey("sosContactNumber")
					&& (bundle.getString("sosContactNumber") != null)
					&& (PhoneNumberUtils.isGlobalPhoneNumber(bundle
							.getString("sosContactNumber")))) {
				phoneNumberSosFragment.setText(bundle
						.getString("sosContactNumber"));
			}

		}

	}

	@Override
	public void onPause() {
		super.onPause();


	}

	@Override
	public void onStop() {
		super.onStop();

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
