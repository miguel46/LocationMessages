package pt.iscte.daam.project.LocationMessages;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;

public class TimeListDialog extends DialogFragment implements OnClickListener {

	public interface TimeListDialogListener {
		void onFinishTimeListEditDialog(String inputText);
	}


	public TimeListDialog() {
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {

		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setTitle("Set time interval between messages");

		builder.setItems(R.array.settings_time_human_value, this);
		
		
		return builder.create();
	}

	@Override
	public void onClick(DialogInterface dialog, int which) {

		TimeListDialogListener activity = (TimeListDialogListener) getActivity();

		String[] strings = getResources().getStringArray(R.array.settings_time_phone_value);
		
		activity.onFinishTimeListEditDialog(strings[which]);

		this.dismiss();
	}

}
