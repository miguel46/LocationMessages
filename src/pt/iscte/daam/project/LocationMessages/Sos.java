package pt.iscte.daam.project.LocationMessages;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.telephony.PhoneNumberUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

public class Sos extends Fragment {

	SharedPreferences preferences;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {

		return inflater.inflate(R.layout.sos_layout, container, false);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		Bundle bundle = this.getArguments();

		TextView phoneNTextView = (TextView) getActivity().findViewById(
				R.id.phoneNumberSosFragment);

		TextView smsText = (TextView) getActivity().findViewById(R.id.smsText);

		if (bundle.containsKey("sosContactNumber")) {

			String phoneNumber = bundle.getString("sosContactNumber");

			if (PhoneNumberUtils.isGlobalPhoneNumber(phoneNumber))
				phoneNTextView.setText(bundle.getString("sosContactNumber"));
			else
				Toast.makeText(getActivity(), "Choose a phone number",
						Toast.LENGTH_LONG).show();
		}

		if (bundle.containsKey("sosMessageText")) {

			smsText.setText(bundle.getString("sosMessageText") + "\n"
					+ smsText.getText());
		}
	}

	@Override
	public void onStart() {
		super.onStart();

	}

	@Override
	public void onPause() {
		super.onPause();
		onStop();

	}

	@Override
	public void onStop() {
		super.onStop();
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();

	}
}
