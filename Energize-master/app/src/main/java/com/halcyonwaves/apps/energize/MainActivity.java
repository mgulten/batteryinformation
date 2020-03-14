package com.halcyonwaves.apps.energize;

import static java.text.MessageFormat.format;

import android.app.Fragment;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ActivityInfo;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;

import com.halcyonwaves.apps.energize.dialogs.ChangeLogDialog;
import com.halcyonwaves.apps.energize.fragments.BatteryCapacityGraphFragment;
import com.halcyonwaves.apps.energize.fragments.OverviewFragment;
import com.halcyonwaves.apps.energize.fragments.TemperatureGraphFragment;
import com.halcyonwaves.apps.energize.services.MonitorBatteryStateService;
import java.util.List;

public class MainActivity extends AppCompatActivity
		implements NavigationView.OnNavigationItemSelectedListener {

	private static final String TAG = "MainActivity";

	private static final String LAST_FRAGMENT_BUNDLE_CONST = "LastFragmentPosition";

	private int lastSelectedFragment = -1;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);

		DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
		ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
				this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
		drawer.setDrawerListener(toggle);
		toggle.syncState();

		NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
		navigationView.setNavigationItemSelectedListener(this);

		// check if the service is running, if not start it
		if (!ApplicationCore.isServiceRunning(this, MonitorBatteryStateService.class.getName())) {
			Log.v(MainActivity.TAG, "Monitoring service is not running, starting it...");
			this.getApplicationContext().startService(new Intent(this.getApplicationContext(), MonitorBatteryStateService.class));
		}

		// show the changelog dialog
		ChangeLogDialog changeDlg = new ChangeLogDialog(this);
		changeDlg.show();

		// ensure the correct item will be displayed
		if ("com.halcyonwaves.apps.energize.fragments.BatteryCapacityGraphFragment".equals(getIntent().getAction())) {
			navigationView.getMenu().performIdentifierAction(R.id.nav_battery_graph, 0);
		} else if ("com.halcyonwaves.apps.energize.fragments.TemperatureGraphFragment".equals(getIntent().getAction())) {
			navigationView.getMenu().performIdentifierAction(R.id.nav_temperature_graph, 0);
		} else {
			navigationView.getMenu().performIdentifierAction(R.id.nav_overview, 0);
		}

		// if the app was not installed via the PlayStore, show a notice
		if (!wasInstalledViaPlaystore() && !playstoreNoteAlreadyDisplayed()) {
			Snackbar snackbar = Snackbar.make(findViewById(R.id.content_coordinator_layout), R.string.snackbar_not_installed_via_playstore_text, Snackbar.LENGTH_INDEFINITE);
			snackbar.setAction(R.string.snackbar_not_installed_via_playstore_action, new OnClickListener() {
				@Override
				public void onClick(View v) {
					AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();
					alertDialog.setTitle(R.string.alertdialog_not_via_playstore_title);
					alertDialog.setMessage(getString(R.string.alertdialog_not_via_playstore_text));
					alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.alertdialog_not_via_playstore_button_go_to_playstore),
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog, int which) {
									dialog.dismiss();
									MainActivity.this.startPlayStore();
									disableGooglePlayWarning();
								}
							});
					alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, getString(R.string.alertdialog_not_via_playstore_button_dismiss),
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog, int which) {
									dialog.dismiss();
									disableGooglePlayWarning();
								}
							});
					alertDialog.show();
				}
			});
			snackbar.show();
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		// store the last selected fragment
		outState.putInt(LAST_FRAGMENT_BUNDLE_CONST, lastSelectedFragment);

		// always call the superclass so it can save the view hierarchy state
		super.onSaveInstanceState(outState);
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		// check whether we're recreating a previously destroyed instance
		if (savedInstanceState != null) {
			final int restoreFragmentId = savedInstanceState.getInt(LAST_FRAGMENT_BUNDLE_CONST);
			selectItem(restoreFragmentId);
		}

		// always call the superclass so it can restore the view hierarchy state
		super.onRestoreInstanceState(savedInstanceState);
	}

	private void disableGooglePlayWarning() {
		final SharedPreferences appPreferences = PreferenceManager.getDefaultSharedPreferences(MainActivity.this.getApplicationContext());
		Editor prefEditor = appPreferences.edit();
		prefEditor.putInt(Consts.PREFERENCE_PLAYSTORE_NOTICE_DISPLAYED_LAST_TIME, 1);
		prefEditor.apply();
	}

	private void startPlayStore() {
		Intent rateIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + BuildConfig.APPLICATION_ID));
		boolean marketFound = false;

		final List<ResolveInfo> otherApps = this.getApplicationContext().getPackageManager().queryIntentActivities(rateIntent, 0);
		for (ResolveInfo otherApp : otherApps) {
			if (otherApp.activityInfo.applicationInfo.packageName.equals("com.android.vending")) {

				ActivityInfo otherAppActivity = otherApp.activityInfo;
				ComponentName componentName = new ComponentName(otherAppActivity.applicationInfo.packageName, otherAppActivity.name);
				rateIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				rateIntent.addFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
				rateIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				rateIntent.setComponent(componentName);
				this.startActivity(rateIntent);
				marketFound = true;
				break;

			}
		}

		// if GP not present on device, open web browser
		if (!marketFound) {
			Intent webIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + BuildConfig.APPLICATION_ID));
			this.startActivity(webIntent);
		}
	}

	private static String defaultIfNull(final String inputValue, final String defaultValue) {
		return (null == inputValue || "null".equals(inputValue)) ? defaultValue : inputValue;
	}

	private boolean playstoreNoteAlreadyDisplayed() {
		final SharedPreferences appPreferences = PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext());
		final int lastTimeDisplayed = appPreferences.getInt(Consts.PREFERENCE_PLAYSTORE_NOTICE_DISPLAYED_LAST_TIME, -1);

		return -1 != lastTimeDisplayed;
	}

	private boolean wasInstalledViaPlaystore() {
		final String installerPackageName = defaultIfNull(this.getPackageManager().getInstallerPackageName(this.getPackageName()), "Unknown");
		Log.i(TAG, format("The package was installed via: {0}", installerPackageName));
		return "com.android.vending".equals(installerPackageName);
	}

	@Override
	public void onBackPressed() {
		DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
		if (drawer.isDrawerOpen(GravityCompat.START)) {
			drawer.closeDrawer(GravityCompat.START);
		} else {
			super.onBackPressed();
		}
	}

	@Override
	public boolean onNavigationItemSelected(@NonNull MenuItem item) {
		// Handle navigation view item clicks here.
		int id = item.getItemId();

		if (id == R.id.nav_overview) {
			selectItem(0);
		} else if (id == R.id.nav_battery_graph) {
			selectItem(1);
		} else if (id == R.id.nav_temperature_graph) {
			selectItem(2);
		} else if (id == R.id.nav_settings) {
			Intent settingsIntent = new Intent(this, SettingsActivity.class);
			startActivity(settingsIntent);
		}

		// checking if the item is in checked state or not, if not set it to checked state.
		if (item.isChecked()) {
			item.setChecked(false);
		} else {
			item.setChecked(true);
		}

		DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
		drawer.closeDrawer(GravityCompat.START);
		return true;
	}

	private void selectItem(int position) {
		// check that the activity is using the layout version with the fragment_container FrameLayout (the one-pane layout)
		if (this.findViewById(R.id.fragment_container) != null) {

			// create a new Fragment to be placed in the activity layout
			Fragment firstFragment = null;
			switch (position) {
				case 1:
					firstFragment = new BatteryCapacityGraphFragment();
					break;
				case 2:
					firstFragment = new TemperatureGraphFragment();
					break;
				case 0:
				default:
					firstFragment = new OverviewFragment();
					break;
			}

			// in case this activity was started with special instructions from an  Intent, pass the Intent's extras to the fragment as arguments
			firstFragment.setArguments(this.getIntent().getExtras());

			// add the fragment to the 'fragment_container' FrameLayout
			this.getFragmentManager().beginTransaction().replace(R.id.fragment_container, firstFragment).commit();

			// store the last selected fragment
			lastSelectedFragment = position;
		}
	}
}
