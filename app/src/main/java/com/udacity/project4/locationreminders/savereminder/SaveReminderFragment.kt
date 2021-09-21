package com.udacity.project4.locationreminders.savereminder

import android.Manifest
import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.PendingIntent
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.BuildConfig
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSaveReminderBinding
import com.udacity.project4.locationreminders.geofence.GeofenceBroadcastReceiver
import com.udacity.project4.locationreminders.geofence.GeofenceConstants.ACTION_GEOFENCE_EVENT
import com.udacity.project4.locationreminders.geofence.GeofenceConstants.GEOFENCE_RADIUS_IN_METERS
import com.udacity.project4.locationreminders.geofence.GeofenceConstants.REQUEST_CODE_BACKGROUND_PERMISSION
import com.udacity.project4.locationreminders.geofence.GeofenceConstants.REQUEST_TURN_DEVICE_LOCATION_ON
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.android.ext.android.inject

private const val TAG = "SaveReminderFragment"

class SaveReminderFragment : BaseFragment() {
    private val runningQOrLater = android.os.Build.VERSION.SDK_INT >=
            android.os.Build.VERSION_CODES.Q

    //Get the view model this time as a single to be shared with the another fragment
    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var binding: FragmentSaveReminderBinding

    lateinit var geofencingClient: GeofencingClient
    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(requireActivity(), GeofenceBroadcastReceiver::class.java)
        intent.action = ACTION_GEOFENCE_EVENT
        PendingIntent.getBroadcast(requireContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_save_reminder, container, false)
        binding.viewModel = _viewModel

        setDisplayHomeAsUpEnabled(true)

        geofencingClient = LocationServices.getGeofencingClient(requireActivity())
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.lifecycleOwner = viewLifecycleOwner
        binding.selectLocation.setOnClickListener {
            //  Navigate to another fragment to get the user location
            _viewModel.navigationCommand.value =
                NavigationCommand.To(SaveReminderFragmentDirections.actionSaveReminderFragmentToSelectLocationFragment())
        }

        binding.saveReminder.setOnClickListener { presaveCheck() }
    }

    override fun onDestroy() {
        super.onDestroy()
        //make sure to clear the view model after destroy, as it's a single view model.
        _viewModel.onClear()
    }

    private fun presaveCheck() {
        val title = _viewModel.reminderTitle.value
        val description = _viewModel.reminderDescription.value
        val location = _viewModel.reminderSelectedLocationStr.value
        val latitude = _viewModel.latitude.value
        val longitude = _viewModel.longitude.value
        val reminder = ReminderDataItem(title, description, location, latitude, longitude)

        if (_viewModel.validateEnteredData(reminder)) {
            // Under SelectRemainderLocation, we have already forced user to grant fine location permission when saving the location
            // Here when user clicks Save, again we need the user to grant background location in order to save this remainder
            if (foregroundBackgroundLocationPermissionApproved()) {
                Log.d(TAG, "presaveCheck - backgroundLocationPermissionApproved")
                saveReminderAndStartGeofence(reminder)
            } else {
                Log.d(TAG, "presaveCheck - requestBackgroundLocationPermissions")
                requestForegroundBackgroundLocationPermissions()
            }
        }
    }

    private fun saveReminderAndStartGeofence(reminder: ReminderDataItem) {
        // Use the user entered reminder details to:
        //  1) add a geofencing request
        //  2) save the reminder to the local db
        _viewModel.saveReminder(reminder)
        startGeofence(reminder)
    }

    private fun startGeofence(reminder: ReminderDataItem, resolve: Boolean = true) {
        val locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_LOW_POWER
        }
        val locationSettingRequestsBuilder =
            LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
        val settingsClient = LocationServices.getSettingsClient(requireContext())
        val locationSettingsResponseTask =
            settingsClient.checkLocationSettings(locationSettingRequestsBuilder.build())

        locationSettingsResponseTask.addOnFailureListener { exception ->
            if (exception is ResolvableApiException && resolve) {
                try {
                    exception.startResolutionForResult(
                        requireActivity(),
                        REQUEST_TURN_DEVICE_LOCATION_ON
                    )
                } catch (sendEx: IntentSender.SendIntentException) {
                    Log.d(TAG, "Error getting location settings resolution: " + sendEx.message)
                }
            } else {
                Snackbar.make(
                    this.requireView(),
                    R.string.permission_denied_explanation_background, Snackbar.LENGTH_INDEFINITE
                ).setAction(android.R.string.ok) {
                    requestForegroundBackgroundLocationPermissions()
                }.show()
            }
        }
        locationSettingsResponseTask.addOnCompleteListener {
            if (it.isSuccessful) {
                Log.d(TAG, "proceed to add geofence")
                addGeofence(reminder)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun addGeofence(reminder: ReminderDataItem) {
        if (reminder.longitude != null && reminder.latitude != null) {
            val geofence = Geofence.Builder()
                .setRequestId(reminder.id)
                .setCircularRegion(
                    reminder.latitude!!, reminder.longitude!!,
                    GEOFENCE_RADIUS_IN_METERS
                )
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
                .build()

            // Build the geofence request
            val geofencingRequest = GeofencingRequest.Builder()
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                .addGeofence(geofence)
                .build()
            geofencingClient.addGeofences(geofencingRequest, geofencePendingIntent)?.run {
                addOnSuccessListener {
                    Log.d(TAG, "Added geofence ${geofence.requestId}")
                }
                addOnFailureListener {
                    Toast.makeText(
                        requireActivity(), R.string.geofences_not_added,
                        Toast.LENGTH_SHORT
                    ).show()
                    if ((it.message != null)) {
                        Log.w(TAG, it.message.toString())
                    }
                }
            }

        }
        _viewModel.validateAndSaveReminder(reminder)
    }

    /*
     *  Permission related codes
     *  Determines whether the app has the appropriate permissions across Android 10+ and all other
     *  Android versions.
     *
     *  Foreground location (ACCESS_FINE_LOCATION) has been checked and forced user to grant under SelectLocationFragment
     *  if here it fails, mostly it means user is on AndroidQ and only granted foreground location
     */
    @TargetApi(29)
    private fun foregroundBackgroundLocationPermissionApproved(): Boolean {
        val foregroundPermissionApproved = (PackageManager.PERMISSION_GRANTED ==
                ActivityCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.ACCESS_FINE_LOCATION
                ))
        val backgroundPermissionApproved =
            if (runningQOrLater) {
                PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(
                    requireContext(), Manifest.permission.ACCESS_BACKGROUND_LOCATION
                )
            } else {
                true
            }
        return foregroundPermissionApproved && backgroundPermissionApproved
    }

    /*
    *  Requests ACCESS_FINE_LOCATION and (on Android 10+ (Q) ACCESS_BACKGROUND_LOCATION.
    */
    @TargetApi(29)
    private fun requestForegroundBackgroundLocationPermissions() {
        if (foregroundBackgroundLocationPermissionApproved())
            return

        // If not runningQOrLater, it should have been returned.
        // This conditional statement can be redundant but check again just to be safe
        // Since we have already requested for foreground permission when choosing location
        // If we run into this problem, it means user chose to grant location while the app is running already
        // For this we must redirect user back to the settings screen to make change
        if (runningQOrLater) {
            Snackbar.make(
                this.requireView(),
                R.string.permission_denied_explanation,
                Snackbar.LENGTH_INDEFINITE
            )
                .setAction(R.string.settings) {
                    startActivity(Intent().apply {
                        action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                        data = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    })
                }.show()
        }
    }

    /*
   * On Android 10+ (Q) we need to have the background permission.
   * Since we call this as a part of the save action, if granted we continue to save
   * Otherwise we showRationalDialog or show a snackbar
   */
    @TargetApi(29)
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        // Check if location permissions are granted and if so enable the
        // location data layer.
        if (requestCode == REQUEST_CODE_BACKGROUND_PERMISSION && grantResults.isNotEmpty()) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                presaveCheck()
            } else if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
                Log.d(TAG, "onRequestPermissionResult - Denied. Show Dialog again")
                requestForegroundBackgroundLocationPermissions()
            }
        }
        Log.d(TAG, "onRequestPermissionResult - Unchecked")
    }
}
