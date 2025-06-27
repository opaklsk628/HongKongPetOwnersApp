package com.example.hongkongpetownersapp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.hongkongpetownersapp.databinding.FragmentPetParksBinding;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PetParksFragment extends Fragment implements OnMapReadyCallback {

    private static final String TAG = "PetParksFragment";
    private static final int SPEECH_REQUEST_CODE = 100;
    private FragmentPetParksBinding binding;
    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private Location currentLocation;
    private List<Marker> markers = new ArrayList<>();

    // Request location permissions
    private final ActivityResultLauncher<String[]> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                Boolean fineLocationGranted = result.getOrDefault(
                        Manifest.permission.ACCESS_FINE_LOCATION, false);
                Boolean coarseLocationGranted = result.getOrDefault(
                        Manifest.permission.ACCESS_COARSE_LOCATION, false);

                if (fineLocationGranted != null && fineLocationGranted) {
                    // Precise location access granted
                    getLocationAndLoadMap();
                } else if (coarseLocationGranted != null && coarseLocationGranted) {
                    // Only approximate location access granted
                    getLocationAndLoadMap();
                } else {
                    // No location access granted
                    Toast.makeText(getContext(),
                            "Location permission is required to show nearby parks",
                            Toast.LENGTH_LONG).show();
                }
            });

    // Voice recognition result launcher
    private final ActivityResultLauncher<Intent> voiceRecognitionLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == getActivity().RESULT_OK && result.getData() != null) {
                    // Get voice recognition results
                    ArrayList<String> matches = result.getData()
                            .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);

                    if (matches != null && !matches.isEmpty()) {
                        // Use the first match as search text
                        String voiceInput = matches.get(0);
                        binding.editSearch.setText(voiceInput);

                        // Show clear button since we have text
                        binding.buttonClear.setVisibility(View.VISIBLE);

                        // Automatically search
                        searchLocation(voiceInput);
                    }
                }
            });

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        binding = FragmentPetParksBinding.inflate(inflater, container, false);

        // Initialize location client
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        return binding.getRoot();
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize map
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        // Check location permissions
        checkLocationPermissionAndLoad();

        // Setup search functionality
        setupSearchBox();

        // Setup voice search button
        setupVoiceSearch();
    }

    private void setupVoiceSearch() {
        // Set voice button click listener
        binding.buttonVoice.setOnClickListener(v -> {
            startVoiceRecognition();
        });
    }

    private void startVoiceRecognition() {
        // Check if voice recognition is available
        if (!getActivity().getPackageManager()
                .hasSystemFeature(PackageManager.FEATURE_MICROPHONE)) {
            Toast.makeText(getContext(),
                    "Your device does not support voice recognition",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        // Create voice recognition intent
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);

        // Set language to English and Chinese
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-HK");
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "en-HK");
        intent.putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, "en-HK");

        // Set prompt message
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT,
                "Please say location or park name");

        // Maximum results to return
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5);

        try {
            // Start voice recognition activity
            voiceRecognitionLauncher.launch(intent);
        } catch (Exception e) {
            Toast.makeText(getContext(),
                    "Voice recognition not available",
                    Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Voice recognition error", e);
        }
    }

    private void setupSearchBox() {
        // Setup clear button click listener
        binding.buttonClear.setOnClickListener(v -> {
            binding.editSearch.setText("");
            binding.buttonClear.setVisibility(View.GONE);
        });

        // Add text change listener for search
        binding.editSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                // Show/hide clear button based on text
                if (s.length() > 0) {
                    binding.buttonClear.setVisibility(View.VISIBLE);
                } else {
                    binding.buttonClear.setVisibility(View.GONE);
                }

                // Check if markers exist
                if (markers.isEmpty() && mMap != null) {
                    loadPetParks();
                }

                // Filter parks based on search text
                String searchText = s.toString().toLowerCase().trim();
                filterMarkersBySearchText(searchText);
            }
        });

        // Handle search button click
        binding.buttonSearch.setOnClickListener(v -> {
            String searchText = binding.editSearch.getText().toString().trim();
            if (!searchText.isEmpty()) {
                searchLocation(searchText);
            }
        });
    }

    private void searchLocation(String locationName) {
        // Use Geocoder to find location
        Geocoder geocoder = new Geocoder(requireContext(), Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocationName(locationName + ", Hong Kong", 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                LatLng location = new LatLng(address.getLatitude(), address.getLongitude());

                // Move camera to the searched location
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(location, 14));

                // Check if markers exist, if not, reload them
                if (markers.isEmpty()) {
                    loadPetParks();
                }

                // Filter markers based on search text
                filterMarkersBySearchText(locationName.toLowerCase().trim());

            } else {
                Toast.makeText(getContext(), "Location not found", Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            Log.e(TAG, "Geocoder error", e);
            Toast.makeText(getContext(), "Search error", Toast.LENGTH_SHORT).show();
        }
    }

    private void filterMarkersBySearchText(String searchText) {
        if (searchText.isEmpty()) {
            // Show all markers
            for (Marker marker : markers) {
                marker.setVisible(true);
            }
        } else {
            // Filter markers based on search text
            int visibleCount = 0;
            for (Marker marker : markers) {
                String title = marker.getTitle().toLowerCase();
                boolean isVisible = title.contains(searchText);
                marker.setVisible(isVisible);
                if (isVisible) {
                    visibleCount++;
                }
            }

            // Show count of matching parks
            if (visibleCount > 0) {
                Toast.makeText(getContext(),
                        "Found " + visibleCount + " related pet parks",
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void checkLocationPermissionAndLoad() {
        if (ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            // Permission already granted
            getLocationAndLoadMap();
        } else {
            // Request permission
            requestPermissionLauncher.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            });
        }
    }

    private void getLocationAndLoadMap() {
        try {
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(location -> {
                        if (location != null) {
                            currentLocation = location;
                            if (mMap != null) {
                                setupMapWithLocation();
                            }
                        } else {
                            // Default to Hong Kong Central if location not available
                            currentLocation = new Location("");
                            currentLocation.setLatitude(22.2796);
                            currentLocation.setLongitude(114.1619);
                            if (mMap != null) {
                                setupMapWithLocation();
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error getting location", e);
                        // Default to Hong Kong Central
                        currentLocation = new Location("");
                        currentLocation.setLatitude(22.2796);
                        currentLocation.setLongitude(114.1619);
                        if (mMap != null) {
                            setupMapWithLocation();
                        }
                    });
        } catch (SecurityException e) {
            Log.e(TAG, "Location permission denied", e);
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        // Enable map UI controls
        mMap.getUiSettings().setZoomControlsEnabled(false);
        mMap.getUiSettings().setCompassEnabled(false);
        mMap.getUiSettings().setMapToolbarEnabled(true);

        // Enable my location if permission granted
        if (ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
            mMap.getUiSettings().setMyLocationButtonEnabled(true);

            // Move location button to bottom right
            View mapView = getChildFragmentManager().findFragmentById(R.id.map).getView();
            if (mapView != null && mapView.findViewById(Integer.parseInt("1")) != null) {
                // Get the view containing the location button
                View locationButton = ((View) mapView.findViewById(Integer.parseInt("1")).getParent()).findViewById(Integer.parseInt("2"));

                // Create layout params for bottom right
                RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(
                        RelativeLayout.LayoutParams.WRAP_CONTENT,
                        RelativeLayout.LayoutParams.WRAP_CONTENT);

                // Position at bottom right
                layoutParams.addRule(RelativeLayout.ALIGN_PARENT_END);
                layoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);

                // Set margins (right: 20dp, bottom: 100dp to avoid overlapping with zoom controls)
                layoutParams.setMargins(0, 0, 20, 100);

                locationButton.setLayoutParams(layoutParams);
            }
        }

        // Set custom info window adapter to handle multi-line text
        mMap.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {
            @Override
            public View getInfoWindow(Marker marker) {
                return null; // Use default window frame
            }

            @Override
            public View getInfoContents(Marker marker) {
                // Inflate custom info window layout
                View view = LayoutInflater.from(getContext()).inflate(
                        android.R.layout.simple_list_item_2, null);

                android.widget.TextView title = view.findViewById(android.R.id.text1);
                android.widget.TextView snippet = view.findViewById(android.R.id.text2);

                title.setText(marker.getTitle());
                title.setTextColor(Color.BLACK);
                title.setTextSize(16);

                snippet.setText(marker.getSnippet());
                snippet.setTextColor(Color.DKGRAY);
                snippet.setTextSize(14);
                snippet.setMaxLines(3);

                return view;
            }
        });

        // Add map click listener to clear search when clicking on map
        mMap.setOnMapClickListener(latLng -> {
            // Clear search box
            binding.editSearch.setText("");
            // Show all markers
            filterMarkersBySearchText("");
        });

        // If location already available, setup map
        if (currentLocation != null) {
            setupMapWithLocation();
        }
    }

    private void setupMapWithLocation() {
        if (mMap == null || currentLocation == null) return;

        // Move camera to current location
        LatLng myLocation = new LatLng(currentLocation.getLatitude(),
                currentLocation.getLongitude());
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(myLocation, 12));

        // Load pet parks
        loadPetParks();
    }

    // Create custom deep sea blue marker
    private BitmapDescriptor getDeepSeaBlueMarker() {
        // Create a bitmap for the marker
        int size = 40;
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint();

        // Deep sea blue color
        paint.setColor(Color.parseColor("#003366"));
        paint.setStyle(Paint.Style.FILL);

        // Draw circle
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint);

        // Draw center dot
        paint.setColor(Color.WHITE);
        canvas.drawCircle(size / 2f, size / 2f, size / 6f, paint);

        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }

    private void loadPetParks() {
        // Show loading
        binding.progressBar.setVisibility(View.VISIBLE);

        // Clear existing markers
        mMap.clear();
        markers.clear();

        // Get all pet parks in Hong Kong
        List<PetPark> petParks = getAllHongKongPetParks();

        // Get custom deep sea blue marker
        BitmapDescriptor deepSeaBlueMarker = getDeepSeaBlueMarker();

        // Add markers for each park
        for (PetPark park : petParks) {
            MarkerOptions markerOptions = new MarkerOptions()
                    .position(park.getLocation())
                    .title(park.getName())
                    .snippet(park.getFacilitiesString())
                    .icon(deepSeaBlueMarker);

            Marker marker = mMap.addMarker(markerOptions);
            markers.add(marker);
        }

        // Hide loading
        binding.progressBar.setVisibility(View.GONE);

        // Show park count
        Toast.makeText(getContext(),
                "Found " + petParks.size() + " pet parks",
                Toast.LENGTH_SHORT).show();
    }

    // Get comprehensive list of pet parks in Hong Kong with updated coordinates
    private List<PetPark> getAllHongKongPetParks() {
        List<PetPark> parks = new ArrayList<>();

        // Hong Kong Island - Central and Western District
        parks.add(new PetPark("Cotton Tree Drive Garden", "Hong Kong Island", "Central & Western",
                new LatLng(22.2860, 114.1480), true, true, false));
        parks.add(new PetPark("Central & Western District Promenade - Sheung Wan Section", "Hong Kong Island", "Central & Western",
                new LatLng(22.2872, 114.1482), false, true, false));
        parks.add(new PetPark("Central & Western District Promenade - Central Section", "Hong Kong Island", "Central & Western",
                new LatLng(22.2818, 114.1578), true, true, true));
        parks.add(new PetPark("Central & Western District Promenade – Western Wholesale Food Market Section", "Hong Kong Island", "Central & Western",
                new LatLng(22.2905, 114.1410), true, true, true));
        parks.add(new PetPark("Central Pier Promenade", "Hong Kong Island", "Central & Western",
                new LatLng(22.2845, 114.1610), false, true, false));
        parks.add(new PetPark("Conduit Road Children's Playground", "Hong Kong Island", "Central & Western",
                new LatLng(22.2726, 114.1482), true, true, false));
        parks.add(new PetPark("Peak Road Garden", "Hong Kong Island", "Central & Western",
                new LatLng(22.2595, 114.1445), true, true, false));
        parks.add(new PetPark("Pier No. 2 Garden", "Hong Kong Island", "Central & Western",
                new LatLng(22.2842, 114.1605), false, true, false));
        parks.add(new PetPark("Pier No. 3 Garden", "Hong Kong Island", "Central & Western",
                new LatLng(22.2844, 114.1615), false, true, false));
        parks.add(new PetPark("Sun Yat Sen Memorial Park", "Hong Kong Island", "Central & Western",
                new LatLng(22.2863, 114.1438), true, true, false));
        parks.add(new PetPark("Sai Wan Park", "Hong Kong Island", "Central & Western",
                new LatLng(22.2888, 114.1400), true, true, false));
        parks.add(new PetPark("Wing Lee Street Rest Garden", "Hong Kong Island", "Central & Western",
                new LatLng(22.2831, 114.1506), false, true, false));
        parks.add(new PetPark("Wing Lee Street Rest Area", "Hong Kong Island", "Central & Western",
                new LatLng(22.2833, 114.1508), false, true, false));
        parks.add(new PetPark("Peak Garden", "Hong Kong Island", "Central & Western",
                new LatLng(22.2711, 114.1491), false, false, true));

        // Hong Kong Island - Eastern District
        parks.add(new PetPark("Chai Wan Park", "Hong Kong Island", "Eastern",
                new LatLng(22.2633, 114.2398), true, true, true));
        parks.add(new PetPark("Fu Hong Street Rest Area", "Hong Kong Island", "Eastern",
                new LatLng(22.2651, 114.2372), true, true, false));
        parks.add(new PetPark("North Point Ferry Pier Square Waterfront Garden", "Hong Kong Island", "Eastern",
                new LatLng(22.2918, 114.2034), false, false, false));
        parks.add(new PetPark("North Point Waterfront Garden (Phase 1) - Promenade", "Hong Kong Island", "Eastern",
                new LatLng(22.2930, 114.2004), true, true, false));
        parks.add(new PetPark("Oil Street Rest Area", "Hong Kong Island", "Eastern",
                new LatLng(22.2913, 114.1935), false, false, false));
        parks.add(new PetPark("Quarry Bay Park", "Hong Kong Island", "Eastern",
                new LatLng(22.2887, 114.2111), true, true, true));
        parks.add(new PetPark("Siu Sai Wan Waterfront Garden", "Hong Kong Island", "Eastern",
                new LatLng(22.2633, 114.2487), true, true, false));

        // Hong Kong Island - Southern District
        parks.add(new PetPark("Aberdeen Waterfront Park", "Hong Kong Island", "Southern",
                new LatLng(22.2492, 114.1548), false, true, true));
        parks.add(new PetPark("Ap Lei Chau Park", "Hong Kong Island", "Southern",
                new LatLng(22.2415, 114.1533), false, true, true));
        parks.add(new PetPark("Ap Lei Chau Promenade", "Hong Kong Island", "Southern",
                new LatLng(22.2424, 114.1542), false, true, true));
        parks.add(new PetPark("Heung Yip Road Rest Area", "Hong Kong Island", "Southern",
                new LatLng(22.2484, 114.1573), false, true, false));
        parks.add(new PetPark("Hung Shing Street Rest Garden", "Hong Kong Island", "Southern",
                new LatLng(22.2427, 114.1531), false, true, false));
        parks.add(new PetPark("Kwan Hoi Path Rest Area", "Hong Kong Island", "Southern",
                new LatLng(22.2446, 114.1554), false, true, false));
        parks.add(new PetPark("Shek O Headland Picnic Site", "Hong Kong Island", "Southern",
                new LatLng(22.2172, 114.2566), false, true, false));
        parks.add(new PetPark("Stanley New Street/Stanley Village Road Rest Area", "Hong Kong Island", "Southern",
                new LatLng(22.2186, 114.2115), false, true, false));
        parks.add(new PetPark("Stanley Waterfront Promenade", "Hong Kong Island", "Southern",
                new LatLng(22.2184, 114.2124), false, true, false));
        parks.add(new PetPark("Stanley Village Road Garden", "Hong Kong Island", "Southern",
                new LatLng(22.2189, 114.2113), false, true, false));
        parks.add(new PetPark("San Pak Kan Rest Area", "Hong Kong Island", "Southern",
                new LatLng(22.2193, 114.2105), false, true, false));

        // Hong Kong Island - Wan Chai District
        parks.add(new PetPark("Bowen Road Garden", "Hong Kong Island", "Wan Chai",
                new LatLng(22.2685, 114.1736), false, true, false));
        parks.add(new PetPark("Bowen Road Park", "Hong Kong Island", "Wan Chai",
                new LatLng(22.2683, 114.1738), false, true, false));
        parks.add(new PetPark("Bowen Road Temporary Rest Area", "Hong Kong Island", "Wan Chai",
                new LatLng(22.2687, 114.1732), false, true, false));
        parks.add(new PetPark("Tamar Park (Phase 1)", "Hong Kong Island", "Wan Chai",
                new LatLng(22.2869, 114.1873), false, true, false));
        parks.add(new PetPark("Lotus Palace Garden", "Hong Kong Island", "Wan Chai",
                new LatLng(22.2771, 114.1771), false, true, false));
        parks.add(new PetPark("Lotus Palace East Street Rest Area", "Hong Kong Island", "Wan Chai",
                new LatLng(22.2768, 114.1775), false, true, false));
        parks.add(new PetPark("Sing Woo Road Rest Garden", "Hong Kong Island", "Wan Chai",
                new LatLng(22.2780, 114.1760), false, true, false));
        parks.add(new PetPark("Tai Tam Reservoir Road Rest Area", "Hong Kong Island", "Wan Chai",
                new LatLng(22.2665, 114.1970), false, true, false));
        parks.add(new PetPark("Causeway Bay Garden", "Hong Kong Island", "Wan Chai",
                new LatLng(22.2795, 114.1892), false, true, false));
        parks.add(new PetPark("Victoria Park - Hill Pavilion", "Hong Kong Island", "Wan Chai",
                new LatLng(22.2824, 114.1897), true, true, true));
        parks.add(new PetPark("Wan Chai Gap Park", "Hong Kong Island", "Wan Chai",
                new LatLng(22.2651, 114.1715), false, true, false));
        parks.add(new PetPark("Wan Chai Temporary Waterfront Garden", "Hong Kong Island", "Wan Chai",
                new LatLng(22.2818, 114.1768), false, true, false));
        parks.add(new PetPark("Ventris Road Rest Garden", "Hong Kong Island", "Wan Chai",
                new LatLng(22.2666, 114.1847), false, true, false));
        parks.add(new PetPark("Wong Nai Chung Road Rest Garden", "Hong Kong Island", "Wan Chai",
                new LatLng(22.2707, 114.1887), false, true, false));

        // Kowloon - Kowloon City District
        parks.add(new PetPark("Kai Tak Sky Garden", "Kowloon", "Kowloon City",
                new LatLng(22.3286, 114.1908), false, true, false));
        parks.add(new PetPark("Lok Fu Park", "Kowloon", "Kowloon City",
                new LatLng(22.3379, 114.1872), false, true, false));
        parks.add(new PetPark("Lung Cheung Road Park", "Kowloon", "Kowloon City",
                new LatLng(22.3355, 114.1843), false, true, true));

        // Kowloon - Kwun Tong District
        parks.add(new PetPark("Cha Kwo Ling Waterfront Park", "Kowloon", "Kwun Tong",
                new LatLng(22.3076, 114.2340), true, true, false));
        parks.add(new PetPark("Hiu Kwong Street Rest Garden", "Kowloon", "Kwun Tong",
                new LatLng(22.3218, 114.2142), false, true, false));
        parks.add(new PetPark("Kwun Tong Waterfront Garden", "Kowloon", "Kwun Tong",
                new LatLng(22.3094, 114.2246), true, true, false));
        parks.add(new PetPark("Kwun Tong Road Rest Garden", "Kowloon", "Kwun Tong",
                new LatLng(22.3125, 114.2234), true, true, false));
        parks.add(new PetPark("Lei Yue Mun Rest Garden", "Kowloon", "Kwun Tong",
                new LatLng(22.2921, 114.2381), false, true, false));
        parks.add(new PetPark("Lei Yue Mun Typhoon Shelter Breakwater Rest Area", "Kowloon", "Kwun Tong",
                new LatLng(22.2924, 114.2385), false, true, false));
        parks.add(new PetPark("Lei Yue Mun Waterfront Rest Area", "Kowloon", "Kwun Tong",
                new LatLng(22.2919, 114.2378), false, true, false));
        parks.add(new PetPark("Sam Ka Tsuen Playground", "Kowloon", "Kwun Tong",
                new LatLng(22.2917, 114.2372), true, true, false));
        parks.add(new PetPark("Ting Fu Street Rest Area", "Kowloon", "Kwun Tong",
                new LatLng(22.3143, 114.2203), false, true, false));
        parks.add(new PetPark("Tsui Ping River Garden", "Kowloon", "Kwun Tong",
                new LatLng(22.3161, 114.2227), false, true, true));
        parks.add(new PetPark("Yuet Wah Street Playground", "Kowloon", "Kwun Tong",
                new LatLng(22.3216, 114.2243), true, true, false));

        // Kowloon - Sham Shui Po District
        parks.add(new PetPark("Cheung Sha Wan Waterfront Garden", "Kowloon", "Sham Shui Po",
                new LatLng(22.3349, 114.1402), false, true, false));
        parks.add(new PetPark("Cheung Sha Wan Road/Cheung Shun Street Playground", "Kowloon", "Sham Shui Po",
                new LatLng(22.3341, 114.1488), false, true, false));
        parks.add(new PetPark("Lai Chi Kok Park", "Kowloon", "Sham Shui Po",
                new LatLng(22.3369, 114.1479), true, true, false));
        parks.add(new PetPark("Lei Cheng Uk Swimming Pool Rest Garden", "Kowloon", "Sham Shui Po",
                new LatLng(22.3358, 114.1553), false, true, false));
        parks.add(new PetPark("Lung Cheung Road Lookout", "Kowloon", "Sham Shui Po",
                new LatLng(22.3382, 114.1618), false, true, false));
        parks.add(new PetPark("Tan Kwai Road Rest Garden", "Kowloon", "Sham Shui Po",
                new LatLng(22.3371, 114.1542), false, true, false));
        parks.add(new PetPark("Shek Kip Mei Park", "Kowloon", "Sham Shui Po",
                new LatLng(22.3341, 114.1666), true, true, true));
        parks.add(new PetPark("Upper Lei Uk Garden", "Kowloon", "Sham Shui Po",
                new LatLng(22.3332, 114.1562), false, true, false));
        parks.add(new PetPark("Tong Yan Street Hillside Rest Area", "Kowloon", "Sham Shui Po",
                new LatLng(22.3345, 114.1575), false, true, false));
        parks.add(new PetPark("Cheuk Kiu Street Rest Area", "Kowloon", "Sham Shui Po",
                new LatLng(22.3323, 114.1587), false, true, false));
        parks.add(new PetPark("Wing Hong Street Rest Garden", "Kowloon", "Sham Shui Po",
                new LatLng(22.3312, 114.1593), false, true, false));

        // Kowloon - Wong Tai Sin District
        parks.add(new PetPark("Kam Fung Street Rest Area", "Kowloon", "Wong Tai Sin",
                new LatLng(22.3423, 114.1962), false, true, false));
        parks.add(new PetPark("King Fook Street Rest Area", "Kowloon", "Wong Tai Sin",
                new LatLng(22.3427, 114.1948), false, true, false));
        parks.add(new PetPark("Lion Rock Park", "Kowloon", "Wong Tai Sin",
                new LatLng(22.3526, 114.1869), true, true, false));
        parks.add(new PetPark("Lung Cheung Road Bauhinia Garden", "Kowloon", "Wong Tai Sin",
                new LatLng(22.3382, 114.1897), false, true, false));
        parks.add(new PetPark("Ngau Chi Wan Village Playground", "Kowloon", "Wong Tai Sin",
                new LatLng(22.3368, 114.2098), false, true, false));
        parks.add(new PetPark("Ngau Chi Wan Village Rest Area", "Kowloon", "Wong Tai Sin",
                new LatLng(22.3363, 114.2102), false, true, false));
        parks.add(new PetPark("San Po Kong Interchange Rest Garden", "Kowloon", "Wong Tai Sin",
                new LatLng(22.3353, 114.1977), false, true, false));
        parks.add(new PetPark("Tsz Wan Shan Road Rest Area", "Kowloon", "Wong Tai Sin",
                new LatLng(22.3482, 114.2003), false, true, false));
        parks.add(new PetPark("Wing Ting Road Rest Area", "Kowloon", "Wong Tai Sin",
                new LatLng(22.3378, 114.2093), false, true, false));

        // Kowloon - Yau Tsim Mong District
        parks.add(new PetPark("Aggregated Fish Road Rest Garden", "Kowloon", "Yau Tsim Mong",
                new LatLng(22.3043, 114.1728), false, true, false));
        parks.add(new PetPark("Haiphong Road Children's Playground", "Kowloon", "Yau Tsim Mong",
                new LatLng(22.2993, 114.1731), false, true, false));
        parks.add(new PetPark("Hong Tat Path Garden", "Kowloon", "Yau Tsim Mong",
                new LatLng(22.3012, 114.1718), false, true, false));
        parks.add(new PetPark("Luen Wan Street Rest Area", "Kowloon", "Yau Tsim Mong",
                new LatLng(22.3083, 114.1712), false, true, false));
        parks.add(new PetPark("Tsim Sha Tsui Waterfront Garden", "Kowloon", "Yau Tsim Mong",
                new LatLng(22.2936, 114.1693), true, true, false));
        parks.add(new PetPark("Waterloo Road／Ferry Street Rest Area", "Kowloon", "Yau Tsim Mong",
                new LatLng(22.3098, 114.1703), false, true, false));
        parks.add(new PetPark("Waterloo Road/Wylie Road Rest Area", "Kowloon", "Yau Tsim Mong",
                new LatLng(22.3108, 114.1708), false, true, false));
        parks.add(new PetPark("Wylie Road Temporary Rest Area", "Kowloon", "Yau Tsim Mong",
                new LatLng(22.3103, 114.1713), false, true, false));

        // New Territories - Islands District
        parks.add(new PetPark("Shek Kok Road Garden", "New Territories", "Islands",
                new LatLng(22.2639, 113.9131), false, true, false));
        parks.add(new PetPark("Mui Wo Galaxy Garden (Zone 2)", "New Territories", "Islands",
                new LatLng(22.2655, 113.9951), true, true, false));
        parks.add(new PetPark("Cheung Chau Sports Hall Adjacent Recreation Land", "New Territories", "Islands",
                new LatLng(22.2097, 114.0282), true, true, false));
        parks.add(new PetPark("Ping Lee Road Rest Area", "New Territories", "Islands",
                new LatLng(22.2103, 114.0254), false, true, true));
        parks.add(new PetPark("Sok Kwu Wan Playground", "New Territories", "Islands",
                new LatLng(22.1948, 114.1323), false, true, false));
        parks.add(new PetPark("Tai O Playground", "New Territories", "Islands",
                new LatLng(22.2543, 113.8643), false, true, false));
        parks.add(new PetPark("Tung Chung North Park (Zone A)", "New Territories", "Islands",
                new LatLng(22.2893, 113.9453), true, true, true));

        // New Territories - Kwai Tsing District
        parks.add(new PetPark("Cheung Fai Road Waterfront Garden", "New Territories", "Kwai Tsing",
                new LatLng(22.3681, 114.1102), false, true, false));
        parks.add(new PetPark("Jockey Club Kwai Shing Circuit Rest Area", "New Territories", "Kwai Tsing",
                new LatLng(22.3633, 114.1247), false, true, false));
        parks.add(new PetPark("Kwai Hei Street Rest Area", "New Territories", "Kwai Tsing",
                new LatLng(22.3642, 114.1232), false, true, false));
        parks.add(new PetPark("Kwai Tak Street Rest Garden", "New Territories", "Kwai Tsing",
                new LatLng(22.3638, 114.1238), false, true, false));
        parks.add(new PetPark("Shek Pai Street Garden", "New Territories", "Kwai Tsing",
                new LatLng(22.3623, 114.1258), false, true, false));
        parks.add(new PetPark("Tai Wo Hau Road Playground", "New Territories", "Kwai Tsing",
                new LatLng(22.3713, 114.1223), false, true, true));
        parks.add(new PetPark("Tsing King Road Garden", "New Territories", "Kwai Tsing",
                new LatLng(22.3583, 114.1047), false, true, false));
        parks.add(new PetPark("Tsing Yi Waterfront Park - Hoi Sham Garden Section", "New Territories", "Kwai Tsing",
                new LatLng(22.3521, 114.1032), true, true, false));
        parks.add(new PetPark("Tsing Yi West Road Park", "New Territories", "Kwai Tsing",
                new LatLng(22.3493, 114.1013), false, true, false));

        // New Territories - North District
        parks.add(new PetPark("Fanling Railway Station Rest Area", "New Territories", "North",
                new LatLng(22.4926, 114.1395), false, true, true));
        parks.add(new PetPark("Luen Wo Hui Playground", "New Territories", "North",
                new LatLng(22.4972, 114.1417), true, true, false));
        parks.add(new PetPark("Ma Cheuk Ling Children's Playground", "New Territories", "North",
                new LatLng(22.4953, 114.1402), false, true, false));
        parks.add(new PetPark("On Lok Village Rest Area (No. 4)", "New Territories", "North",
                new LatLng(22.5003, 114.1447), false, true, false));
        parks.add(new PetPark("Pak Wo Road Playground", "New Territories", "North",
                new LatLng(22.5018, 114.1383), true, true, true));
        parks.add(new PetPark("Sha Tau Kok Waterfront Rest Area", "New Territories", "North",
                new LatLng(22.5413, 114.2293), false, true, false));
        parks.add(new PetPark("Tin Ping Road Garden", "New Territories", "North",
                new LatLng(22.4978, 114.1463), false, true, false));
        parks.add(new PetPark("Wo Keng Shan Road Garden", "New Territories", "North",
                new LatLng(22.4891, 114.1427), false, true, false));

        // New Territories - Sai Kung District
        parks.add(new PetPark("Fu Man Garden", "New Territories", "Sai Kung",
                new LatLng(22.3823, 114.2667), false, true, false));
        parks.add(new PetPark("Hang Hau Garden", "New Territories", "Sai Kung",
                new LatLng(22.3173, 114.2653), false, true, false));
        parks.add(new PetPark("Hong Kong Velodrome Park", "New Territories", "Sai Kung",
                new LatLng(22.3188, 114.2582), true, true, false));
        parks.add(new PetPark("Lam Shing Road Rest Area", "New Territories", "Sai Kung",
                new LatLng(22.3198, 114.2597), false, true, false));
        parks.add(new PetPark("Man Yee Playground", "New Territories", "Sai Kung",
                new LatLng(22.3852, 114.2703), false, true, false));
        parks.add(new PetPark("Pui Shing Garden", "New Territories", "Sai Kung",
                new LatLng(22.3183, 114.2623), false, true, false));
        parks.add(new PetPark("Sai Kung Waterfront Park", "New Territories", "Sai Kung",
                new LatLng(22.3821, 114.2721), false, true, false));
        parks.add(new PetPark("Sha Tsui Playground", "New Territories", "Sai Kung",
                new LatLng(22.3828, 114.2712), false, true, false));
        parks.add(new PetPark("Tai Mong Tsai Picnic Site and Pavilion", "New Territories", "Sai Kung",
                new LatLng(22.4111, 114.3235), false, true, false));
        parks.add(new PetPark("Tiu Keng Leng Garden", "New Territories", "Sai Kung",
                new LatLng(22.3073, 114.2543), false, true, false));
        parks.add(new PetPark("Tseung Kwan O Waterfront Park", "New Territories", "Sai Kung",
                new LatLng(22.3085, 114.2607), false, true, false));
        parks.add(new PetPark("Tseung Lam Highway Garden", "New Territories", "Sai Kung",
                new LatLng(22.3102, 114.2553), false, true, false));
        parks.add(new PetPark("Wo Mei Rest Garden", "New Territories", "Sai Kung",
                new LatLng(22.3093, 114.2573), false, true, false));

        // New Territories - Sha Tin District
        parks.add(new PetPark("Che Kung Temple Road Playground", "New Territories", "Sha Tin",
                new LatLng(22.3743, 114.1837), false, true, true));
        parks.add(new PetPark("Ma On Shan Waterfront Promenade", "New Territories", "Sha Tin",
                new LatLng(22.4211, 114.2311), true, true, false));
        parks.add(new PetPark("Ma On Shan West Sha Road Garden", "New Territories", "Sha Tin",
                new LatLng(22.4223, 114.2297), false, true, false));
        parks.add(new PetPark("Mei Tin Road Rest Area", "New Territories", "Sha Tin",
                new LatLng(22.3753, 114.1917), false, true, false));
        parks.add(new PetPark("Mui Tsz Lam Road Garden", "New Territories", "Sha Tin",
                new LatLng(22.3763, 114.1947), false, true, false));
        parks.add(new PetPark("Pak Shek Kok Promenade - Sha Tin Section", "New Territories", "Sha Tin",
                new LatLng(22.4203, 114.2097), false, true, true));
        parks.add(new PetPark("Sha Tin Park", "New Territories", "Sha Tin",
                new LatLng(22.3788, 114.1869), false, true, false));
        parks.add(new PetPark("Shek Mun Playground", "New Territories", "Sha Tin",
                new LatLng(22.3883, 114.2093), false, true, true));
        parks.add(new PetPark("Shing Mun River First Waterfront Garden", "New Territories", "Sha Tin",
                new LatLng(22.3813, 114.1893), false, true, false));
        parks.add(new PetPark("Shing Mun River Second Waterfront Garden", "New Territories", "Sha Tin",
                new LatLng(22.3823, 114.1897), false, true, false));
        parks.add(new PetPark("Shing Mun River Third Waterfront Garden", "New Territories", "Sha Tin",
                new LatLng(22.3833, 114.1903), false, true, true));
        parks.add(new PetPark("Tai Shui Hang First, Second, Third and Fourth Shelter Pavilions", "New Territories", "Sha Tin",
                new LatLng(22.4083, 114.2177), false, true, false));
        parks.add(new PetPark("Wong Nai Tau Garden", "New Territories", "Sha Tin",
                new LatLng(22.3943, 114.2053), false, true, false));

        // New Territories - Tai Po District
        parks.add(new PetPark("Ha Hang Village Rest Area", "New Territories", "Tai Po",
                new LatLng(22.4423, 114.1677), false, true, false));
        parks.add(new PetPark("Mui Shue Hang Playground", "New Territories", "Tai Po",
                new LatLng(22.4483, 114.1767), false, true, true));
        parks.add(new PetPark("Pak Shek Kok Promenade - Tai Po Section", "New Territories", "Tai Po",
                new LatLng(22.4583, 114.1893), false, true, false));
        parks.add(new PetPark("Tai Po Kau Garden", "New Territories", "Tai Po",
                new LatLng(22.4223, 114.1643), false, true, false));
        parks.add(new PetPark("Tai Po Kau Park", "New Territories", "Tai Po",
                new LatLng(22.4213, 114.1647), false, true, true));
        parks.add(new PetPark("Tai Po Waterfront Park", "New Territories", "Tai Po",
                new LatLng(22.4531, 114.1717), true, true, false));
        parks.add(new PetPark("Tat Wan Road Rest Area", "New Territories", "Tai Po",
                new LatLng(22.4513, 114.1687), false, true, false));
        parks.add(new PetPark("Ting Tai Road Children's Playground", "New Territories", "Tai Po",
                new LatLng(22.4493, 114.1603), false, true, false));
        parks.add(new PetPark("Tolo Harbour Garden", "New Territories", "Tai Po",
                new LatLng(22.4553, 114.1747), false, true, false));
        parks.add(new PetPark("Tung Tsz Shan Road Garden", "New Territories", "Tai Po",
                new LatLng(22.4473, 114.1823), false, true, false));
        parks.add(new PetPark("Yuen Chau Tsai Park", "New Territories", "Tai Po",
                new LatLng(22.4465, 114.1703), false, true, true));

        // New Territories - Tsuen Wan District
        parks.add(new PetPark("Tsuen Wan Waterfront Cycling Hub", "New Territories", "Tsuen Wan",
                new LatLng(22.3673, 114.1167), true, true, true));
        parks.add(new PetPark("Circular Park", "New Territories", "Tsuen Wan",
                new LatLng(22.3733, 114.1133), false, false, true));
        parks.add(new PetPark("Hoi On Road Playground", "New Territories", "Tsuen Wan",
                new LatLng(22.3683, 114.1147), true, false, false));
        parks.add(new PetPark("Kwok Shui Road Park", "New Territories", "Tsuen Wan",
                new LatLng(22.3703, 114.1197), true, true, true));
        parks.add(new PetPark("Sam Tsz Street Playground", "New Territories", "Tsuen Wan",
                new LatLng(22.3713, 114.1183), true, true, true));
        parks.add(new PetPark("Tsuen King Circuit Garden", "New Territories", "Tsuen Wan",
                new LatLng(22.3753, 114.1153), true, false, false));
        parks.add(new PetPark("Tsuen Wan Park", "New Territories", "Tsuen Wan",
                new LatLng(22.3661, 114.1174), false, true, true));
        parks.add(new PetPark("Tsuen Wan Waterfront Park", "New Territories", "Tsuen Wan",
                new LatLng(22.3673, 114.1152), false, true, true));
        parks.add(new PetPark("Wai Tsuen Road Rest Garden", "New Territories", "Tsuen Wan",
                new LatLng(22.3683, 114.1193), false, false, false));
        parks.add(new PetPark("Wo Yi Hop Road Garden", "New Territories", "Tsuen Wan",
                new LatLng(22.3787, 114.1083), false, true, false));

        // New Territories - Tuen Mun District
        parks.add(new PetPark("Old Coffee Bay Rest Area", "New Territories", "Tuen Mun",
                new LatLng(22.3583, 113.9643), false, true, false));
        parks.add(new PetPark("Kei Lun Wai Public Park", "New Territories", "Tuen Mun",
                new LatLng(22.3813, 113.9747), false, true, false));
        parks.add(new PetPark("San On Rest Area", "New Territories", "Tuen Mun",
                new LatLng(22.3873, 113.9683), false, true, false));
        parks.add(new PetPark("So Kwun Wat Rest Area", "New Territories", "Tuen Mun",
                new LatLng(22.3743, 113.9937), false, true, false));
        parks.add(new PetPark("Tseng Tau Sheung Tsuen Rest Area", "New Territories", "Tuen Mun",
                new LatLng(22.3893, 113.9663), false, true, false));
        parks.add(new PetPark("Tsing Pik Rest Area", "New Territories", "Tuen Mun",
                new LatLng(22.3763, 113.9713), false, true, false));
        parks.add(new PetPark("Wah Fat Playground", "New Territories", "Tuen Mun",
                new LatLng(22.3855, 113.9725), true, true, true));
        parks.add(new PetPark("Wu Shan Garden", "New Territories", "Tuen Mun",
                new LatLng(22.3853, 113.9777), false, true, false));

        // New Territories - Yuen Long District
        parks.add(new PetPark("Ping Pak Lane Park", "New Territories", "Yuen Long",
                new LatLng(22.4433, 114.0237), false, true, false));
        parks.add(new PetPark("Sheung Tsuen Park", "New Territories", "Yuen Long",
                new LatLng(22.4383, 114.0763), false, true, false));
        parks.add(new PetPark("Tin Ho Road Playground", "New Territories", "Yuen Long",
                new LatLng(22.4493, 113.9973), false, false, true));
        parks.add(new PetPark("Tin Tsz Garden", "New Territories", "Yuen Long",
                new LatLng(22.4683, 114.0037), false, true, false));
        parks.add(new PetPark("Tong Yan San Tsuen Playground", "New Territories", "Yuen Long",
                new LatLng(22.4453, 114.0283), false, true, false));
        parks.add(new PetPark("Wang Tai Shan Playground", "New Territories", "Yuen Long",
                new LatLng(22.4423, 114.0347), false, true, false));
        parks.add(new PetPark("Wang Yip Nam Street Rest Garden", "New Territories", "Yuen Long",
                new LatLng(22.4473, 114.0333), false, false, false));

        return parks;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}