package com.udacity.project4.locationreminders.savereminder.selectreminderlocation


import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.*
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.navigation.fragment.findNavController
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.BuildConfig
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.databinding.FragmentSelectLocationBinding
import com.udacity.project4.locationreminders.geofence.GeofenceConstants.REQUEST_CODE_LOCATION_PERMISSION
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
     *      - If shouldShowRequestPermissionRationale == true, then show Rationale and ask again
     *      - If shouldShowRequestPermissionRationale == false, a snackbar is shown,
     *        bringing the user to the App settings dialog.
     *    - User may go back without saving, or it must grant the permission in order to save.
     */
    private fun isPermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) === PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermissionOrShowRationale(shouldGoSetupIfFailed: Boolean) {
        // Permission is not granted, and we need to decide if we have to explain further

        // If user has denied previously, this will return true
        if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
            showRationaleDialog()
        } else {
            // Try to request it in the original way
            Log.d(TAG, "requestPermissionOrShowRationale - Should not show Rationale")
            if (shouldGoSetupIfFailed) {
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
            } else {
                // Try to request again. Note that if user chose Don't ask again, then
                // onRequestPermissionsResult won't be called.
                ActivityCompat.requestPermissions(
                    requireActivity(),
                    arrayOf<String>(Manifest.permission.ACCESS_FINE_LOCATION),
                    REQUEST_CODE_LOCATION_PERMISSION
                )
            }
        }
    }

    private fun showRationaleDialog() {
        val builder: AlertDialog.Builder = AlertDialog.Builder(requireContext())
        builder.setTitle(R.string.location_required_error)
            .setMessage(R.string.permission_denied_explanation)
            .setPositiveButton(R.string.OK) { _, _ ->
                ActivityCompat.requestPermissions(
                    requireActivity(),
                    arrayOf<String>(Manifest.permission.ACCESS_FINE_LOCATION),
                    REQUEST_CODE_LOCATION_PERMISSION
                )
            }
        builder.create().show()
    }

    @SuppressLint("MissingPermission")
    private fun enableMyLocation() {
        if (isPermissionGranted()) {
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
        } else {
            requestPermissionOrShowRationale(false)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        // Check if location permissions are granted and if so enable the
        // location data layer.
        if (requestCode == REQUEST_CODE_LOCATION_PERMISSION && grantResults.isNotEmpty()) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                enableMyLocation()
            } else if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
                Log.d(TAG, "onRequest PermissionResult - Denied. Show Dialog again")
                showRationaleDialog()
            }
        }
        Log.d(TAG, "onRequest PermissionResult - Unchecked")
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
        if (isPermissionGranted()) {
            _viewModel.latitude.value = selectedPoi!!.latLng.latitude
            _viewModel.longitude.value = selectedPoi!!.latLng.longitude
            _viewModel.reminderSelectedLocationStr.value = selectedPoi!!.name
            _viewModel.selectedPOI.value = selectedPoi

            findNavController().popBackStack()
        } else {
            requestPermissionOrShowRationale(true)
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
