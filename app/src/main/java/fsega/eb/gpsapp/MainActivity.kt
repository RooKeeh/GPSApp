package fsega.eb.gpsapp

import android.Manifest
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import java.io.IOException

private lateinit var fusedLocationClient: FusedLocationProviderClient
private lateinit var mMap: GoogleMap
private lateinit var toggleModeButton: Button
private lateinit var goToLosAngelesButton: Button
private lateinit var goToClujButton: Button
private lateinit var locationDetailsTextView: TextView

private var firstPoint: LatLng? = null
private var secondPoint: LatLng? = null
private var isDistanceMode: Boolean = false

class MainActivity : AppCompatActivity(), OnMapReadyCallback, GoogleMap.OnMapClickListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        toggleModeButton = findViewById(R.id.toggleModeButton)
        goToLosAngelesButton = findViewById(R.id.goToLosAngelesButton)
        goToClujButton = findViewById(R.id.goToClujButton)
        locationDetailsTextView = findViewById(R.id.locationDetailsTextView)

        toggleModeButton.setOnClickListener {
            isDistanceMode = !isDistanceMode
            val modeText =
                if (isDistanceMode) "Switch to Details Mode" else "Switch to Distance Mode"
            toggleModeButton.text = modeText
            resetMapAndPoints()
        }

        goToLosAngelesButton.setOnClickListener {
            navigateToLocation(LatLng(37.4221, -122.0853), "Los Angeles")
        }

        goToClujButton.setOnClickListener {
            navigateToLocation(LatLng(46.7732, 23.6214), "Cluj-Napoca")
        }

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.mapType = GoogleMap.MAP_TYPE_NORMAL

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
        ) {
            mMap.isMyLocationEnabled = true
            mMap.uiSettings.isZoomControlsEnabled = true
            mMap.setOnMapClickListener(this)
            addPresetMarkers()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }

    override fun onMapClick(latLng: LatLng) {
        if (isDistanceMode) {
            handleDistanceMode(latLng)
        } else {
            handleDetailsMode(latLng)
        }
    }

    private fun navigateToLocation(latLng: LatLng, locationName: String) {
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 12f))
        mMap.addMarker(MarkerOptions().position(latLng).title(locationName))
        locationDetailsTextView.text = "Navigated to $locationName"
    }

    private fun handleDetailsMode(latLng: LatLng) {
        mMap.clear()
        addPresetMarkers()
        mMap.addMarker(MarkerOptions().position(latLng).title("Waypoint"))

        val latitude = latLng.latitude
        val longitude = latLng.longitude
        val geocoder = Geocoder(this)

        try {
            val addressList = geocoder.getFromLocation(latitude, longitude, 1)
            val address =
                if (addressList?.isNotEmpty() == true) addressList[0].getAddressLine(0) else "Unknown location"

            locationDetailsTextView.text = """
            Latitude: $latitude
            Longitude: $longitude
            Address: $address
        """.trimIndent()

        } catch (e: IOException) {
            e.printStackTrace()
            locationDetailsTextView.text = """
            Latitude: $latitude
            Longitude: $longitude
            Address: Not available
        """.trimIndent()
        }
    }

    private fun handleDistanceMode(latLng: LatLng) {
        if (firstPoint != null && secondPoint != null) {
            resetMapAndPoints()
        }

        mMap.addMarker(MarkerOptions().position(latLng).title("Waypoint"))

        if (firstPoint == null) {
            firstPoint = latLng
            locationDetailsTextView.text =
                "First point selected:\nLatitude: ${latLng.latitude}, Longitude: ${latLng.longitude}"
        } else {
            secondPoint = latLng
            calculateAndDisplayDistance(firstPoint!!, secondPoint!!)
            drawLineBetweenPoints(firstPoint!!, secondPoint!!)
        }
    }

    private fun calculateAndDisplayDistance(point1: LatLng, point2: LatLng) {
        val location1 = Location("Point 1").apply {
            latitude = point1.latitude
            longitude = point1.longitude
        }

        val location2 = Location("Point 2").apply {
            latitude = point2.latitude
            longitude = point2.longitude
        }

        val distance = location1.distanceTo(location2) / 1000

        locationDetailsTextView.text = """
            First Point: Latitude ${point1.latitude}, Longitude ${point1.longitude}
            Second Point: Latitude ${point2.latitude}, Longitude ${point2.longitude}
            Distance: ${"%.2f".format(distance)} km
        """.trimIndent()
    }

    private fun drawLineBetweenPoints(point1: LatLng, point2: LatLng) {
        val polylineOptions = PolylineOptions()
            .add(point1)
            .add(point2)
            .color(ContextCompat.getColor(this, R.color.teal_700))
            .width(5f)
        mMap.addPolyline(polylineOptions)
    }

    private fun resetMapAndPoints() {
        firstPoint = null
        secondPoint = null
        mMap.clear()
        addPresetMarkers()
        locationDetailsTextView.text = if (isDistanceMode) {
            "Tap two points to calculate the distance."
        } else {
            "Tap a point to get location details."
        }
    }

    private fun addPresetMarkers() {
        mMap.addMarker(MarkerOptions().position(LatLng(37.4221, -122.0853)).title("Los Angeles"))
        mMap.addMarker(MarkerOptions().position(LatLng(46.7732, 23.6214)).title("Cluj-Napoca"))
    }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                if (ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_FINE_LOCATION,
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    mMap.isMyLocationEnabled = true
                }
            }
        }
    }
}
