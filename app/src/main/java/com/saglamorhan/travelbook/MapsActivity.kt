package com.saglamorhan.travelbook

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import java.util.*

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var locationManager : LocationManager
    private lateinit var locationListener : LocationListener

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onBackPressed() {
        super.onBackPressed()

        val intentToMain = Intent(this,MainActivity::class.java)
        startActivity(intentToMain)
        finish()
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.setOnMapLongClickListener(myListener)

        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        locationListener = object : LocationListener{
            override fun onLocationChanged(location: Location?) {

                if (location != null){

                    val sharedPreferences = this@MapsActivity.getSharedPreferences("com.saglamorhan.travelbook",Context.MODE_PRIVATE)
                    val firstTimeCheck = sharedPreferences.getBoolean("notFirstTime",false)
                    if (!firstTimeCheck){
                        mMap.clear()
                        val newUserlocation = LatLng(location.latitude,location.longitude)
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(newUserlocation,15f))
                        sharedPreferences.edit().putBoolean("notFirstTime",true).apply()
                    }

                }
            }

            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {

            }

            override fun onProviderEnabled(provider: String?) {

            }

            override fun onProviderDisabled(provider: String?) {

            }

        }
        if (ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),1)
        }else{
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,2,2f,locationListener)

            val intent = intent
            val info = intent.getStringExtra("info")

            if (info.equals("new")){
                val lastLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                if (lastLocation != null){
                    val lastLocationLatLng = LatLng(lastLocation.latitude,lastLocation.longitude)
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(lastLocationLatLng,15f))
                }
            }else{
                mMap.clear()
                val selectedPlace = intent.getSerializableExtra("selectedPlace") as Place
                val selectedLoaction = LatLng(selectedPlace.latitude!!,selectedPlace.longitude!!)
                mMap.addMarker(MarkerOptions().title(selectedPlace.address).position(selectedLoaction))
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(selectedLoaction,15f))

            }


        }
    }

    val myListener = object :GoogleMap.OnMapLongClickListener{
        override fun onMapLongClick(p0: LatLng?) {

            val geocoder = Geocoder(this@MapsActivity, Locale.getDefault())
            var address=""

            if (p0 !=null){
                try {
                    val addressList = geocoder.getFromLocation(p0.latitude,p0.longitude,1)

                    if (addressList != null && addressList.size >0){
                        if (addressList[0].thoroughfare != null){
                            address+= addressList[0].thoroughfare
                            if (addressList[0].subThoroughfare != null){
                                address+= addressList[0].subThoroughfare
                            }
                        }
                    }else{
                        address = "New Place"
                    }
                }catch (e:Exception){
                    e.printStackTrace()
                }
                mMap.clear()
                mMap.addMarker(MarkerOptions().position(p0).title(address))

                val newPlace = Place(address,p0.latitude,p0.longitude)

                val dialog = AlertDialog.Builder(this@MapsActivity)
                dialog.setCancelable(false)
                dialog.setTitle("Are you sure?")
                dialog.setMessage(newPlace.address)
                dialog.setPositiveButton("Yes"){dialog,which ->
                    //SQLite SAVE

                    try {

                        val database=openOrCreateDatabase("Places",Context.MODE_PRIVATE,null)
                        database.execSQL("CREATE TABLE IF NOT EXISTS places (address VARCHAR, latitude DOUBLE, longitude DOUBLE)")
                        val toCompile = "INSERT INTO places (address, latitude,longitude) VALUES (?, ?, ?)"
                        val sqListeStatement = database.compileStatement(toCompile)
                        sqListeStatement.bindString(1,newPlace.address)
                        sqListeStatement.bindDouble(2,newPlace.latitude!!)
                        sqListeStatement.bindDouble(3, newPlace.longitude!!)
                        sqListeStatement.execute()

                    }catch (e:Exception){
                        e.printStackTrace()
                    }
                    Toast.makeText(this@MapsActivity,"New Place Created",Toast.LENGTH_LONG).show()
                }.setNegativeButton("No"){dialog,which->
                    Toast.makeText(this@MapsActivity,"Canceled!",Toast.LENGTH_LONG).show()

                }
                dialog.show()
            }
        }

    }
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == 1){
            if (grantResults.size >0){
                if (ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION) ==
                    PackageManager.PERMISSION_GRANTED){
                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,2,2f,locationListener)
                }
            }
        }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }
}