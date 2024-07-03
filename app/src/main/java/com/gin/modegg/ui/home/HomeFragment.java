package com.gin.modegg.ui.home;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.gin.modegg.R;
import com.gin.modegg.databinding.FragmentHomeBinding;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.model.PlaceLikelihood;
import com.google.android.libraries.places.api.net.FindCurrentPlaceRequest;
import com.google.android.libraries.places.api.net.FindCurrentPlaceResponse;
import com.google.android.libraries.places.api.net.PlacesClient;

import java.util.Arrays;
import java.util.List;

public class HomeFragment extends Fragment implements OnMapReadyCallback {

    private FragmentHomeBinding binding;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private static final int REQUEST_CODE = 101;
    private GoogleMap mMap;
    private PlacesClient placesClient;
    private TextView tvNearbyPlaces;

    private static final String TAG = "HomeFragment";

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        HomeViewModel homeViewModel =
                new ViewModelProvider(this).get(HomeViewModel.class);

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        fusedLocationProviderClient =
                LocationServices.getFusedLocationProviderClient(requireActivity());

        Button btnComisarias = root.findViewById(R.id.btnComisarias);
        Button btnHospitales = root.findViewById(R.id.btnHospitales);
        Button btnLuegoMas = root.findViewById(R.id.btnLuegoMas);
        Button toggleButton = root.findViewById(R.id.btnActivModeGG);

        btnComisarias.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                findNearbyPlaces(Place.Type.POLICE);
            }
        });

        btnHospitales.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                findNearbyPlaces(Place.Type.HOSPITAL);
            }
        });

        btnLuegoMas.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openTaxiAppURL();
            }
        });

        toggleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleBatterySaverMode();
                toggleGPS();
            }
        });

        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        // Inicializar Places API
        Places.initialize(requireContext(), getString(R.string.google_maps_key));
        placesClient = Places.createClient(requireContext());

        return root;
    }

    private void openTaxiAppURL() {
        String url = "http://www.taxi-cusco-vip.com/";
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));

        try {
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(requireContext(), "No se puede abrir la URL", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Error opening URL: " + e.getMessage(), e);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void toggleBatterySaverMode() {
        try {
            Context context = getActivity();
            IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
            Intent batteryStatus = context.registerReceiver(null, ifilter);
            int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
            float batteryPct = level / (float) scale * 100;

            if (batteryPct < 60) {
                Intent intent = new Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS);
                startActivity(intent);
                Toast.makeText(context, "Llevando a la configuración de batería", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(context, "La batería está sobre 60% (Genial ;) )", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in toggleBatterySaverMode: " + e.getMessage(), e);
            Toast.makeText(requireContext(), "Error al cambiar el modo de ahorro de batería", Toast.LENGTH_SHORT).show();
        }
    }

    private void toggleGPS() {
        try {
            Context context = getActivity();
            LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
            boolean isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);

            if (isGPSEnabled) {
                Toast.makeText(context, "El GPS está encendido", Toast.LENGTH_SHORT).show();
            } else {
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivity(intent);
                Toast.makeText(context, "El GPS está apagado 'Enciéndalo por favor'", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in toggleGPS: " + e.getMessage(), e);
            Toast.makeText(requireContext(), "Error al cambiar el estado del GPS", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        getCurrentLocation();
    }

    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(
                getActivity(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission
                (getActivity(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions
                    (getActivity(), new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_CODE);
            return;
        }

        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setInterval(60000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setFastestInterval(5000);

        LocationCallback locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }

                for (Location location : locationResult.getLocations()) {
                    if (location != null) {
                        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                        mMap.addMarker(new MarkerOptions().position(latLng).title("Current Location"));
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
                    }
                }
            }
        };

        fusedLocationProviderClient.requestLocationUpdates
                (locationRequest, locationCallback, null);

        Task<Location> task = fusedLocationProviderClient.getLastLocation();
        task.addOnSuccessListener(new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                if (location != null) {
                    LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                    mMap.addMarker(new MarkerOptions().position(latLng).title("Current Location"));
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
                }
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                findNearbyPlaces(Place.Type.POLICE);
                findNearbyPlaces(Place.Type.HOSPITAL);
            } else {
                Toast.makeText(requireContext(), "Permiso de ubicación denegado", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void findNearbyPlaces(Place.Type placeType) {
        List<Place.Field> placeFields = Arrays.asList(Place.Field.NAME, Place.Field.LAT_LNG);

        FindCurrentPlaceRequest request = FindCurrentPlaceRequest.newInstance(placeFields);

        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(), new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_CODE);
            return;
        }

        // Ejecuta la solicitud para encontrar lugares cercanos
        Task<FindCurrentPlaceResponse> placeResponse = placesClient.findCurrentPlace(request);
        placeResponse.addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                FindCurrentPlaceResponse response = task.getResult();
                if (response != null) {
                    StringBuilder placesList = new StringBuilder("Lugares cercanos:\n\n");
                    for (PlaceLikelihood placeLikelihood : response.getPlaceLikelihoods()) {
                        Place place = placeLikelihood.getPlace();

                        if (place.getTypes().contains(placeType)) {
                            placesList.append(place.getName()).append("\n");

                            LatLng placeLatLng = place.getLatLng();
                            if (placeLatLng != null) {
                                mMap.addMarker(new MarkerOptions().position(placeLatLng).title(place.getName()));
                            }
                        }
                    }
                    tvNearbyPlaces.setText(placesList.toString());
                }

            } else {
                Exception exception = task.getException();
                if (exception instanceof ApiException) {
                    ApiException apiException = (ApiException) exception;
                    Status status = apiException.getStatus();
                    Toast.makeText(requireContext(), "Error al buscar lugares: " + status.getStatusMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });


    }
}
