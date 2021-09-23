package com.udacity.project4.locationreminders.savereminder.selectreminderlocation


import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.udacity.project4.BuildConfig
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSelectLocationBinding
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.android.ext.android.inject
import kotlin.math.roundToInt

class SelectLocationFragment : BaseFragment(), OnMapReadyCallback {

    private val TAG = "SelectLocationFragment"

    // Map related
    private lateinit var map: GoogleMap

    // Temporary location selection - only copied to ViewModel when save button is clicked
    var selectedPoi: PointOfInterest? = null

    //Use Koin to get the view model of the SaveReminder
    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var binding: FragmentSelectLocationBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_select_location, container, false)

        binding.viewModel = _viewModel
        binding.lifecycleOwner = this

        setHasOptionsMenu(true)
        setDisplayHomeAsUpEnabled(true)

        // Before user selected a location, the save button should be disabled
        binding.buttonSave.isEnabled = false
        binding.buttonSave.setOnClickListener { onLocationSelected() }

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment =
            getChildFragmentManager().findFragmentById(R.id.fragment_map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        return binding.root
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     *
     * This function will do the followings as required by the project specification
     * Add the map setup implementation
     * zoom to the user location after taking his permission
     * add style to the map
     * put a marker to location that the user selected
     */
    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap

        // We are not sure if we can get my location or not
        // To initialize the map move the LatLng to a place first
        val latitude = 51.4930762
        val longitude = -0.1487444
        val zoomLevel = 15f

        val homeLatLng = LatLng(latitude, longitude)
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(homeLatLng, zoomLevel))

        // Allow user to select a custom location or a POI
        setMapClick(map)
        setPoiClick(map)
        setMapStyle(map)

        enableMyLocation()
    }

    private fun setMapClick(map: GoogleMap) {
        map.setOnMapClickListener { latLng ->
            // We need one marker only - clear existing before creating one
            map.clear()
            map.addMarker(
                MarkerOptions()
                    .position(latLng)
                    .title(getString(R.string.dropped_pin))
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
            )

            // We may use geolocation lookup for a more meaningful place name
            // but for now we just try to have something simple as its name
            val customPoiName = "${latLng.latitude.roundToInt()}, ${latLng.longitude.roundToInt()}"
            selectedPoi = PointOfInterest(latLng, customPoiName, customPoiName)
            binding.buttonSave.isEnabled = true
        }
    }

    private fun setPoiClick(map: GoogleMap) {
        map.setOnPoiClickListener { poi ->
            // We need one marker only - clear existing before creating one
            map.clear()
            map.addMarker(
                MarkerOptions()
                    .position(poi.latLng)
                    .title(poi.name)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
            )

            selectedPoi = poi
            binding.buttonSave.isEnabled = true
        }
    }

    private fun setMapStyle(map: GoogleMap) {
        try {
            // Customize the styling of the base map using a JSON object defined
            // in a raw resource file.
            val success = map.setMapStyle(
                MapStyleOptions.loadRawResourceStyle(
                    requireContext(),
                    R.raw.map_styles
                )
            )

            if (!success) {
                Log.e(TAG, "Style parsing failed.")
            }
        } catch (ex: Resources.NotFoundException) {
            Log.e(TAG, "Can't find style. Error: ", ex)
        }
    }

    /*****
     * The process of asking for ACCESS)FINE_LOCATION permission:
     * We must need the user to grant us the fine location, even we would try to ask for that politely.
     * 1) When the fragment starts, it will try to request for the first time.
     *    - If granted then it would be perfect.
     *    - If denied, when "save" button is clicked, we will check again and make this compulsory
     * 2) User will be free to select a location or a POI no matter of the permission result
     * 3) When user clicks "Save", permission will be checked again.
     *    - If permission was previously rejected:
     *      User may go back without saving, or it must grant the permission in order to save.
     */
    private fun enableMyLocation() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            )
                    == PackageManager.PERMISSION_GRANTED -> {
                // You can use the API that requires the permission.
                map.isMyLocationEnabled = true

                // Try to move to the last known location,
                // Only if the user has not picked a location
                if (selectedPoi == null) {
                    val fusedLocationClient =
                        LocationServices.getFusedLocationProviderClient(requireActivity())
                    fusedLocationClient.lastLocation.addOnSuccessListener(requireActivity()) {
                        if (it != null) {
                            val lastKnownLatLng = LatLng(it.latitude, it.longitude)
                            map.moveCamera(
                                CameraUpdateFactory.newLatLngZoom(lastKnownLatLng, 15f)
                            )
                        }
                    }
                }
            }
            shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) -> {
                // In an educational UI, explain to the user why your app requires this
                // permission for a specific feature to behave as expected. In this UI,
                // include a "cancel" or "no thanks" button that allows the user to
                // continue using your app without granting the permission.
                showRationaleDialog()
            }
            else -> {
                // You can directly ask for the permission.
                // The registered ActivityResultCallback gets the result of this request.
                myLocationRequestPermissionLauncher.launch(
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            }
        }
    }

    private fun showRationaleDialog() {
        val builder: AlertDialog.Builder = AlertDialog.Builder(requireContext())
        builder.setTitle(R.string.location_required_error)
            .setMessage(R.string.permission_denied_explanation)
            .setPositiveButton(getString(R.string.understood)) { it, _ ->
                it.dismiss()
            }
            .setNegativeButton(R.string.settings) { _, _ ->
                startActivity(Intent().apply {
                    action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                    data = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                })
            }
        builder.create().show()
    }

    // Suggestion from mentor:
    // https://developer.android.com/training/permissions/requesting#allow-system-manage-request-code
    // Register the permissions callback, which handles the user's response to the
    // system permissions dialog. Save the return value, an instance of
    // ActivityResultLauncher. You can use either a val, as shown in this snippet,
    // or a lateinit var in your onAttach() or onCreate() method.
    val myLocationRequestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            Log.d(TAG, "myLocationRequestPermissionLauncher isGranted = {$isGranted}")
            if (isGranted) {
                // Permission is granted. Continue the action or workflow in your
                // app.
                enableMyLocation()
            } else {
                // Explain to the user that the feature is unavailable because the
                // features requires a permission that the user has denied. At the
                // same time, respect the user's decision. Don't link to system
                // settings in an effort to convince the user to change their
                // decision.
                showRationaleDialog()
            }
        }

    /**
     * When the user confirms on the selected location,
     * send back the selected location details to the view model
     * and navigate back to the previous fragment to save the reminder and add the geofence
     *
     * It is the most reasonable time to make sure user has granted the necessary permission.
     * If ACCESS_FINE_LOCATION is not granted, we do not let user to save this position.
     */
    private fun onLocationSelected() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            )
                    == PackageManager.PERMISSION_GRANTED -> {
                // You can use the API that requires the permission.
                _viewModel.latitude.value = selectedPoi!!.latLng.latitude
                _viewModel.longitude.value = selectedPoi!!.latLng.longitude
                _viewModel.reminderSelectedLocationStr.value = selectedPoi!!.name
                _viewModel.selectedPOI.value = selectedPoi

                _viewModel.navigationCommand.postValue(
                    NavigationCommand.Back
                )
            }
            shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) -> {
                // In an educational UI, explain to the user why your app requires this
                // permission for a specific feature to behave as expected. In this UI,
                // include a "cancel" or "no thanks" button that allows the user to
                // continue using your app without granting the permission.
                showRationaleDialog()
            }
            else -> {
                // You can directly ask for the permission.
                // The registered ActivityResultCallback gets the result of this request.
                saveRequestPermissionLauncher.launch(
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            }
        }
    }

    val saveRequestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            Log.d(TAG, "saveRequestPermissionLauncher isGranted = {$isGranted}")
            if (isGranted) {
                // Permission is granted. Continue the action or workflow in your
                // app.
                onLocationSelected()
            } else {
                // Explain to the user that the feature is unavailable because the
                // features requires a permission that the user has denied. At the
                // same time, respect the user's decision. Don't link to system
                // settings in an effort to convince the user to change their
                // decision.
                showRationaleDialog()
            }
        }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.map_options, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.normal_map -> {
            map.mapType = GoogleMap.MAP_TYPE_NORMAL
            true
        }
        R.id.hybrid_map -> {
            map.mapType = GoogleMap.MAP_TYPE_HYBRID
            true
        }
        R.id.satellite_map -> {
            map.mapType = GoogleMap.MAP_TYPE_SATELLITE
            true
        }
        R.id.terrain_map -> {
            map.mapType = GoogleMap.MAP_TYPE_TERRAIN
            true
        }
        else -> super.onOptionsItemSelected(item)
    }
}
