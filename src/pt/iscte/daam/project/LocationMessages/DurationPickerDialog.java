package pt.iscte.daam.project.LocationMessages;

import android.app.Dialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.widget.TimePicker;

public class DurationPickerDialog extends DialogFragment implements
		TimePickerDialog.OnTimeSetListener {

	/**
	 * Interface to communicate with the main activity, to indicate that user
	 * picked the duration
	 * 
	 */
	public interface DurationPickerDialogListener {
		void onFinishDurationPickerEditDialog(int arg1, int arg2);
	}

	public DurationPickerDialog() {
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		return new TimePickerDialog(getActivity(), this, 0, 0, true);

	}

	@Override
	public void onTimeSet(TimePicker arg0, int arg1, int arg2) {
		DurationPickerDialogListener activity = (DurationPickerDialogListener) getActivity();
		activity.onFinishDurationPickerEditDialog(arg1, arg2);

		this.dismiss();

	}

}
