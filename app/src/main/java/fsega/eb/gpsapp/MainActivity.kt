package fsega.eb.gpsapp

import android.os.Bundle
import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.widget.Button
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import android.location.Geocoder
import java.io.IOException

private lateinit var fusedLocationClient: FusedLocationProviderClient
private var currentLocation: Location? = null
private lateinit var mMap: GoogleMap
private lateinit var goToLosAngelesButton: Button
private lateinit var goToClujButton: Button
private lateinit var locationDetailsTextView: TextView

class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        goToLosAngelesButton = findViewById(R.id.goToLosAngelesButton)
        goToClujButton = findViewById(R.id.goToClujButton)
        locationDetailsTextView = findViewById(R.id.locationDetailsTextView)

        goToLosAngelesButton.setOnClickListener {
            changeCameraPositionToLosAngeles()
        }

        goToClujButton.setOnClickListener {
            changeCameraPositionToCluj()
        }

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.mapType = GoogleMap.MAP_TYPE_HYBRID

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
        ) {
            mMap.isMyLocationEnabled = true
            mMap.uiSettings.isZoomControlsEnabled = true
            addSampleMarkers()
            getUserLocationAndUpdate()

            // Show Cluj-Napoca as the default camera position
            showClujNapoca()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }

    private fun addSampleMarkers() {
        val losAngeles = LatLng(34.0549, -118.2426)
        mMap.addMarker(MarkerOptions().position(losAngeles).title("Los Angeles"))
        val clujNapoca = LatLng(46.7712, 23.6236)
        mMap.addMarker(MarkerOptions().position(clujNapoca).title("Cluj-Napoca"))
    }

    private fun showClujNapoca() {
        val clujNapoca = LatLng(46.7712, 23.6236) // Coordinates for Cluj-Napoca
        mMap.moveCamera(
            CameraUpdateFactory.newLatLngZoom(
                clujNapoca,
                11.5f
            )
        ) // Default zoom level for Cluj
    }

    private fun changeCameraPositionToLosAngeles() {
        val losAngeles = LatLng(34.0549, -118.2426)
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(losAngeles, 10f), 2500, null)
    }

    private fun changeCameraPositionToCluj() {
        val clujNapoca = LatLng(46.7712, 23.6236)
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(clujNapoca, 11.5f), 2500, null)
    }

    private fun getUserLocationAndUpdate() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            if (location != null) {
                currentLocation = location
                updateLocationDetails(location)
            }
        }
    }

    private fun updateLocationDetails(location: Location) {
        val latitude = location.latitude
        val longitude = location.longitude

        val clujNapocaLocation = Location("Cluj-Napoca")
        clujNapocaLocation.latitude = 46.7712
        clujNapocaLocation.longitude = 23.6236
        val distance = location.distanceTo(clujNapocaLocation) / 1000 // Distance in km

        val geocoder = Geocoder(this)
        try {
            val addressList = geocoder.getFromLocation(latitude, longitude, 1)
            if (addressList != null) {
                if (addressList.isNotEmpty()) {
                    val address = addressList?.get(0)
                    val fullAddress = address?.getAddressLine(0)
                    val city = address?.locality
                    val postalCode = address?.postalCode
                    val country = address?.countryName

                    locationDetailsTextView.text = """
                            Latitude: $latitude
                            Longitude: $longitude
                            Distance to Cluj-Napoca: $distance km
                            Address: $fullAddress
                            City: $city
                            Postal Code: $postalCode
                            Country: $country
                        """.trimIndent()
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
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
                        Manifest.permission.ACCESS_FINE_LOCATION
                    )
                    == PackageManager.PERMISSION_GRANTED
                ) {
                    mMap.isMyLocationEnabled = true
                    getUserLocationAndUpdate()
                }
            }
        }
    }
}
