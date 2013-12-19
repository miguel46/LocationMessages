package pt.iscte.daam.project.LocationMessages;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class Menu extends Fragment {

	public Menu() {

	}

	/**
	 * Interface to communicate with the activity
	 * 
	 */
	public interface MenuCreatedListener {
		void onMenuCreatedListener(boolean created);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		// Inflate the layout for this fragment

		return inflater.inflate(R.layout.menu_layout, container, false);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		MenuCreatedListener onMenuCreatedListener = (MenuCreatedListener) getActivity();

		onMenuCreatedListener.onMenuCreatedListener(true);

	}

	
}
