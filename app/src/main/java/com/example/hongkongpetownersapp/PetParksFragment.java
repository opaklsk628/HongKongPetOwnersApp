package com.example.hongkongpetownersapp;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
    }

    private void setupSearchBox() {
        // Add text change listener for search
        binding.editSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                // Filter parks based on search text
                String searchText = s.toString().toLowerCase().trim();
                if (searchText.isEmpty()) {
                    // Show all markers
                    for (Marker marker : markers) {
                        marker.setVisible(true);
                    }
                } else {
                    // Filter markers
                    for (Marker marker : markers) {
                        String title = marker.getTitle().toLowerCase();
                        marker.setVisible(title.contains(searchText));
                    }
                }
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
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(location, 14));
            } else {
                Toast.makeText(getContext(), "Location not found", Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            Log.e(TAG, "Geocoder error", e);
            Toast.makeText(getContext(), "Search error", Toast.LENGTH_SHORT).show();
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
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setCompassEnabled(true);
        mMap.getUiSettings().setMapToolbarEnabled(true);

        // Enable my location if permission granted
        if (ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
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
                "Found " + petParks.size() + " pet parks in Hong Kong",
                Toast.LENGTH_SHORT).show();
    }

    // Get comprehensive list of pet parks in Hong Kong with updated coordinates
    private List<PetPark> getAllHongKongPetParks() {
        List<PetPark> parks = new ArrayList<>();

        // Hong Kong Island - Central and Western District
        parks.add(new PetPark("加多近街花園", "香港島", "中西區",
                new LatLng(22.2860, 114.1480), true, true, false));
        parks.add(new PetPark("中西區海濱長廊－上環段", "香港島", "中西區",
                new LatLng(22.2872, 114.1482), false, true, false));
        parks.add(new PetPark("中西區海濱長廊 - 中環段", "香港島", "中西區",
                new LatLng(22.2818, 114.1578), true, true, true));
        parks.add(new PetPark("中西區海濱長廊 – 西區副食品批發市場段", "香港島", "中西區",
                new LatLng(22.2905, 114.1410), true, true, true));
        parks.add(new PetPark("中環碼頭海濱長廊", "香港島", "中西區",
                new LatLng(22.2845, 114.1610), false, true, false));
        parks.add(new PetPark("干德道兒童遊樂場", "香港島", "中西區",
                new LatLng(22.2726, 114.1482), true, true, false));
        parks.add(new PetPark("山頂道花園", "香港島", "中西區",
                new LatLng(22.2595, 114.1445), true, true, false));
        parks.add(new PetPark("二號碼頭花園", "香港島", "中西區",
                new LatLng(22.2842, 114.1605), false, true, false));
        parks.add(new PetPark("三號碼頭花園", "香港島", "中西區",
                new LatLng(22.2844, 114.1615), false, true, false));
        // Updated coordinates for Sun Yat Sen Memorial Park
        parks.add(new PetPark("中山紀念公園", "香港島", "中西區",
                new LatLng(22.2863, 114.1438), true, true, false));
        parks.add(new PetPark("城西公園", "香港島", "中西區",
                new LatLng(22.2888, 114.1400), true, true, false));
        parks.add(new PetPark("永利街休憩花園", "香港島", "中西區",
                new LatLng(22.2831, 114.1506), false, true, false));
        parks.add(new PetPark("永利街休憩處", "香港島", "中西區",
                new LatLng(22.2833, 114.1508), false, true, false));
        parks.add(new PetPark("山頂花園", "香港島", "中西區",
                new LatLng(22.2711, 114.1491), false, false, true));

        // Hong Kong Island - Eastern District
        parks.add(new PetPark("柴灣公園", "香港島", "東區",
                new LatLng(22.2633, 114.2398), true, true, true));
        parks.add(new PetPark("富康街休憩處", "香港島", "東區",
                new LatLng(22.2651, 114.2372), true, true, false));
        parks.add(new PetPark("北角渡海輪碼頭廣場海濱花園", "香港島", "東區",
                new LatLng(22.2918, 114.2034), false, false, false));
        parks.add(new PetPark("北角海濱花園(一期) - 海濱長廊", "香港島", "東區",
                new LatLng(22.2930, 114.2004), true, true, false));
        parks.add(new PetPark("油街休憩處", "香港島", "東區",
                new LatLng(22.2913, 114.1935), false, false, false));
        parks.add(new PetPark("鰂魚涌公園", "香港島", "東區",
                new LatLng(22.2887, 114.2111), true, true, true));
        parks.add(new PetPark("小西灣海濱花園", "香港島", "東區",
                new LatLng(22.2633, 114.2487), true, true, false));

        // Hong Kong Island - Southern District
        parks.add(new PetPark("香港仔海濱公園", "香港島", "南區",
                new LatLng(22.2492, 114.1548), false, true, true));
        parks.add(new PetPark("鴨脷洲公園", "香港島", "南區",
                new LatLng(22.2415, 114.1533), false, true, true));
        parks.add(new PetPark("鴨脷洲海濱長廊", "香港島", "南區",
                new LatLng(22.2424, 114.1542), false, true, true));
        parks.add(new PetPark("香葉道休憩處", "香港島", "南區",
                new LatLng(22.2484, 114.1573), false, true, false));
        parks.add(new PetPark("洪聖街休憩花園", "香港島", "南區",
                new LatLng(22.2427, 114.1531), false, true, false));
        parks.add(new PetPark("觀海徑休憩處", "香港島", "南區",
                new LatLng(22.2446, 114.1554), false, true, false));
        parks.add(new PetPark("石澳海角郊遊區", "香港島", "南區",
                new LatLng(22.2172, 114.2566), false, true, false));
        parks.add(new PetPark("赤柱新街/赤柱村道休憩處", "香港島", "南區",
                new LatLng(22.2186, 114.2115), false, true, false));
        parks.add(new PetPark("赤柱海濱長廊", "香港島", "南區",
                new LatLng(22.2184, 114.2124), false, true, false));
        parks.add(new PetPark("赤柱村道花園", "香港島", "南區",
                new LatLng(22.2189, 114.2113), false, true, false));
        parks.add(new PetPark("新八間休憩處", "香港島", "南區",
                new LatLng(22.2193, 114.2105), false, true, false));
        // Removed 瀑布灣公園 (Waterfall Bay Park) as requested

        // Hong Kong Island - Wan Chai District
        parks.add(new PetPark("寶雲道花園", "香港島", "灣仔區",
                new LatLng(22.2685, 114.1736), false, true, false));
        parks.add(new PetPark("寶雲道公園", "香港島", "灣仔區",
                new LatLng(22.2683, 114.1738), false, true, false));
        parks.add(new PetPark("寶雲道臨時休憩處", "香港島", "灣仔區",
                new LatLng(22.2687, 114.1732), false, true, false));
        parks.add(new PetPark("東岸公園 (第一期)", "香港島", "灣仔區",
                new LatLng(22.2869, 114.1873), false, true, false));
        parks.add(new PetPark("蓮花宮花園", "香港島", "灣仔區",
                new LatLng(22.2771, 114.1771), false, true, false));
        parks.add(new PetPark("蓮花宮東街休憩處", "香港島", "灣仔區",
                new LatLng(22.2768, 114.1775), false, true, false));
        parks.add(new PetPark("成和道休憩花園", "香港島", "灣仔區",
                new LatLng(22.2780, 114.1760), false, true, false));
        parks.add(new PetPark("大潭水塘道休憩處", "香港島", "灣仔區",
                new LatLng(22.2665, 114.1970), false, true, false));
        parks.add(new PetPark("銅鑼灣花園", "香港島", "灣仔區",
                new LatLng(22.2795, 114.1892), false, true, false));
        parks.add(new PetPark("維多利亞公園 - 山丘涼亭", "香港島", "灣仔區",
                new LatLng(22.2824, 114.1897), true, true, true));
        parks.add(new PetPark("灣仔峽公園", "香港島", "灣仔區",
                new LatLng(22.2651, 114.1715), false, true, false));
        parks.add(new PetPark("灣仔臨時海濱花園", "香港島", "灣仔區",
                new LatLng(22.2818, 114.1768), false, true, false));
        parks.add(new PetPark("威非路道休憩花園", "香港島", "灣仔區",
                new LatLng(22.2666, 114.1847), false, true, false));
        parks.add(new PetPark("黃泥涌道休憩花園大", "香港島", "灣仔區",
                new LatLng(22.2707, 114.1887), false, true, false));

        // Kowloon - Kowloon City District
        parks.add(new PetPark("啟德空中花園", "九龍", "九龍城區",
                new LatLng(22.3286, 114.1908), false, true, false));
        parks.add(new PetPark("樂富公園", "九龍", "九龍城區",
                new LatLng(22.3379, 114.1872), false, true, false));
        parks.add(new PetPark("龍翔道公園", "九龍", "九龍城區",
                new LatLng(22.3355, 114.1843), false, true, true));

        // Kowloon - Kwun Tong District
        parks.add(new PetPark("茶果嶺海濱公園", "九龍", "觀塘區",
                new LatLng(22.3076, 114.2340), true, true, false));
        parks.add(new PetPark("曉光街休憩花園", "九龍", "觀塘區",
                new LatLng(22.3218, 114.2142), false, true, false));
        parks.add(new PetPark("觀塘海濱花園", "九龍", "觀塘區",
                new LatLng(22.3094, 114.2246), true, true, false));
        parks.add(new PetPark("觀塘道休憩花園", "九龍", "觀塘區",
                new LatLng(22.3125, 114.2234), true, true, false));
        parks.add(new PetPark("鯉魚門休憩花園", "九龍", "觀塘區",
                new LatLng(22.2921, 114.2381), false, true, false));
        parks.add(new PetPark("鯉魚門避風塘防波堤休憩處", "九龍", "觀塘區",
                new LatLng(22.2924, 114.2385), false, true, false));
        parks.add(new PetPark("鯉魚門海濱休憩處", "九龍", "觀塘區",
                new LatLng(22.2919, 114.2378), false, true, false));
        parks.add(new PetPark("三家村遊樂場", "九龍", "觀塘區",
                new LatLng(22.2917, 114.2372), true, true, false));
        parks.add(new PetPark("定富街休憩處", "九龍", "觀塘區",
                new LatLng(22.3143, 114.2203), false, true, false));
        parks.add(new PetPark("翠屏河花園", "九龍", "觀塘區",
                new LatLng(22.3161, 114.2227), false, true, true));
        parks.add(new PetPark("月華街遊樂場", "九龍", "觀塘區",
                new LatLng(22.3216, 114.2243), true, true, false));

        // Kowloon - Sham Shui Po District
        parks.add(new PetPark("長沙灣海濱花園", "九龍", "深水埗區",
                new LatLng(22.3349, 114.1402), false, true, false));
        parks.add(new PetPark("長沙灣道/長順街遊樂場", "九龍", "深水埗區",
                new LatLng(22.3341, 114.1488), false, true, false));
        parks.add(new PetPark("荔枝角公園", "九龍", "深水埗區",
                new LatLng(22.3369, 114.1479), true, true, false));
        parks.add(new PetPark("李鄭屋泳池休憩花園", "九龍", "深水埗區",
                new LatLng(22.3358, 114.1553), false, true, false));
        parks.add(new PetPark("龍翔道眺望處", "九龍", "深水埗區",
                new LatLng(22.3382, 114.1618), false, true, false));
        parks.add(new PetPark("丹桂路休憩花園", "九龍", "深水埗區",
                new LatLng(22.3371, 114.1542), false, true, false));
        parks.add(new PetPark("石硤尾公園", "九龍", "深水埗區",
                new LatLng(22.3341, 114.1666), true, true, true));
        parks.add(new PetPark("上李屋花園", "九龍", "深水埗區",
                new LatLng(22.3332, 114.1562), false, true, false));
        parks.add(new PetPark("棠蔭街山邊休憩處", "九龍", "深水埗區",
                new LatLng(22.3345, 114.1575), false, true, false));
        parks.add(new PetPark("雀橋街休憩處", "九龍", "深水埗區",
                new LatLng(22.3323, 114.1587), false, true, false));
        parks.add(new PetPark("永康街休憩花園", "九龍", "深水埗區",
                new LatLng(22.3312, 114.1593), false, true, false));

        // Kowloon - Wong Tai Sin District
        parks.add(new PetPark("金鳳街休憩處", "九龍", "黃大仙區",
                new LatLng(22.3423, 114.1962), false, true, false));
        parks.add(new PetPark("景福街休憩處", "九龍", "黃大仙區",
                new LatLng(22.3427, 114.1948), false, true, false));
        parks.add(new PetPark("獅子山公園", "九龍", "黃大仙區",
                new LatLng(22.3526, 114.1869), true, true, false));
        parks.add(new PetPark("龍翔道洋紫荊花園", "九龍", "黃大仙區",
                new LatLng(22.3382, 114.1897), false, true, false));
        parks.add(new PetPark("牛池灣村遊樂場", "九龍", "黃大仙區",
                new LatLng(22.3368, 114.2098), false, true, false));
        parks.add(new PetPark("牛池灣村休憩處", "九龍", "黃大仙區",
                new LatLng(22.3363, 114.2102), false, true, false));
        parks.add(new PetPark("新蒲崗交匯處休憩花園", "九龍", "黃大仙區",
                new LatLng(22.3353, 114.1977), false, true, false));
        parks.add(new PetPark("慈雲山道休憩處", "九龍", "黃大仙區",
                new LatLng(22.3482, 114.2003), false, true, false));
        parks.add(new PetPark("永定道休憩處", "九龍", "黃大仙區",
                new LatLng(22.3378, 114.2093), false, true, false));

        // Kowloon - Yau Tsim Mong District
        parks.add(new PetPark("聚魚道休憩花園", "九龍", "油尖旺區",
                new LatLng(22.3043, 114.1728), false, true, false));
        parks.add(new PetPark("海防道兒童遊樂場", "九龍", "油尖旺區",
                new LatLng(22.2993, 114.1731), false, true, false));
        parks.add(new PetPark("康達徑花園", "九龍", "油尖旺區",
                new LatLng(22.3012, 114.1718), false, true, false));
        parks.add(new PetPark("聯運街休憩處", "九龍", "油尖旺區",
                new LatLng(22.3083, 114.1712), false, true, false));
        parks.add(new PetPark("尖沙咀海濱花園", "九龍", "油尖旺區",
                new LatLng(22.2936, 114.1693), true, true, false));
        parks.add(new PetPark("窩打老道／渡船街休憩處", "九龍", "油尖旺區",
                new LatLng(22.3098, 114.1703), false, true, false));
        parks.add(new PetPark("窩打老道/衛理道休憩處", "九龍", "油尖旺區",
                new LatLng(22.3108, 114.1708), false, true, false));
        parks.add(new PetPark("衛理道臨時休憩處", "九龍", "油尖旺區",
                new LatLng(22.3103, 114.1713), false, true, false));

        // New Territories - Islands District
        parks.add(new PetPark("思高路花園", "新界", "離島區",
                new LatLng(22.2639, 113.9131), false, true, false));
        parks.add(new PetPark("梅窩銀河花園 (2區)", "新界", "離島區",
                new LatLng(22.2655, 113.9951), true, true, false));
        parks.add(new PetPark("長洲體育館旁休憩用地", "新界", "離島區",
                new LatLng(22.2097, 114.0282), true, true, false));
        parks.add(new PetPark("坪利路休憩處", "新界", "離島區",
                new LatLng(22.2103, 114.0254), false, true, true));
        parks.add(new PetPark("索罟灣遊樂場", "新界", "離島區",
                new LatLng(22.1948, 114.1323), false, true, false));
        parks.add(new PetPark("大嶼山大澳遊樂場", "新界", "離島區",
                new LatLng(22.2543, 113.8643), false, true, false));
        parks.add(new PetPark("東涌北公園 (A區)", "新界", "離島區",
                new LatLng(22.2893, 113.9453), true, true, true));

        // New Territories - Kwai Tsing District
        parks.add(new PetPark("長輝路海濱花園", "新界", "葵青區",
                new LatLng(22.3681, 114.1102), false, true, false));
        parks.add(new PetPark("賽馬會葵盛圍休憩處", "新界", "葵青區",
                new LatLng(22.3633, 114.1247), false, true, false));
        parks.add(new PetPark("葵喜街休憩處", "新界", "葵青區",
                new LatLng(22.3642, 114.1232), false, true, false));
        parks.add(new PetPark("葵德街休憩花園", "新界", "葵青區",
                new LatLng(22.3638, 114.1238), false, true, false));
        parks.add(new PetPark("石貝街花園", "新界", "葵青區",
                new LatLng(22.3623, 114.1258), false, true, false));
        parks.add(new PetPark("大窩口道遊樂場", "新界", "葵青區",
                new LatLng(22.3713, 114.1223), false, true, true));
        parks.add(new PetPark("青敬路花園", "新界", "葵青區",
                new LatLng(22.3583, 114.1047), false, true, false));
        parks.add(new PetPark("青衣海濱公園 - 海欣花園段", "新界", "葵青區",
                new LatLng(22.3521, 114.1032), true, true, false));
        parks.add(new PetPark("青衣西路公園", "新界", "葵青區",
                new LatLng(22.3493, 114.1013), false, true, false));

        // New Territories - North District
        parks.add(new PetPark("粉嶺火車站休憩處", "新界", "北區",
                new LatLng(22.4926, 114.1395), false, true, true));
        parks.add(new PetPark("聯和墟遊樂場", "新界", "北區",
                new LatLng(22.4972, 114.1417), true, true, false));
        parks.add(new PetPark("麻雀嶺兒童遊樂場", "新界", "北區",
                new LatLng(22.4953, 114.1402), false, true, false));
        parks.add(new PetPark("安樂村休憩處(四號)", "新界", "北區",
                new LatLng(22.5003, 114.1447), false, true, false));
        parks.add(new PetPark("百和路遊樂場", "新界", "北區",
                new LatLng(22.5018, 114.1383), true, true, true));
        parks.add(new PetPark("沙頭角海濱休憩處", "新界", "北區",
                new LatLng(22.5413, 114.2293), false, true, false));
        parks.add(new PetPark("天平路花園", "新界", "北區",
                new LatLng(22.4978, 114.1463), false, true, false));
        parks.add(new PetPark("禾徑山路花園", "新界", "北區",
                new LatLng(22.4891, 114.1427), false, true, false));

        // New Territories - Sai Kung District
        parks.add(new PetPark("福民花園", "新界", "西貢區",
                new LatLng(22.3823, 114.2667), false, true, false));
        parks.add(new PetPark("坑口花園", "新界", "西貢區",
                new LatLng(22.3173, 114.2653), false, true, false));
        parks.add(new PetPark("香港單車館公園", "新界", "西貢區",
                new LatLng(22.3188, 114.2582), true, true, false));
        parks.add(new PetPark("林盛路休憩處", "新界", "西貢區",
                new LatLng(22.3198, 114.2597), false, true, false));
        parks.add(new PetPark("萬宜遊樂場", "新界", "西貢區",
                new LatLng(22.3852, 114.2703), false, true, false));
        parks.add(new PetPark("培成花園", "新界", "西貢區",
                new LatLng(22.3183, 114.2623), false, true, false));
        parks.add(new PetPark("西貢海濱公園", "新界", "西貢區",
                new LatLng(22.3821, 114.2721), false, true, false));
        parks.add(new PetPark("沙咀遊樂場", "新界", "西貢區",
                new LatLng(22.3828, 114.2712), false, true, false));
        parks.add(new PetPark("大網仔郊遊區及亭", "新界", "西貢區",
                new LatLng(22.4111, 114.3235), false, true, false));
        parks.add(new PetPark("調景嶺花園", "新界", "西貢區",
                new LatLng(22.3073, 114.2543), false, true, false));
        parks.add(new PetPark("將軍澳海濱公園", "新界", "西貢區",
                new LatLng(22.3085, 114.2607), false, true, false));
        parks.add(new PetPark("將藍公路花園", "新界", "西貢區",
                new LatLng(22.3102, 114.2553), false, true, false));
        parks.add(new PetPark("窩尾休憩花園", "新界", "西貢區",
                new LatLng(22.3093, 114.2573), false, true, false));

        // New Territories - Sha Tin District
        parks.add(new PetPark("車公廟路遊樂場", "新界", "沙田區",
                new LatLng(22.3743, 114.1837), false, true, true));
        parks.add(new PetPark("馬鞍山海濱長廊", "新界", "沙田區",
                new LatLng(22.4211, 114.2311), true, true, false));
        parks.add(new PetPark("馬鞍山西沙路花園", "新界", "沙田區",
                new LatLng(22.4223, 114.2297), false, true, false));
        parks.add(new PetPark("美田路休憩處", "新界", "沙田區",
                new LatLng(22.3753, 114.1917), false, true, false));
        parks.add(new PetPark("梅子林路花園", "新界", "沙田區",
                new LatLng(22.3763, 114.1947), false, true, false));
        parks.add(new PetPark("白石角海濱長廊-沙田段", "新界", "沙田區",
                new LatLng(22.4203, 114.2097), false, true, true));
        parks.add(new PetPark("沙田公園", "新界", "沙田區",
                new LatLng(22.3788, 114.1869), false, true, false));
        parks.add(new PetPark("石門遊樂場", "新界", "沙田區",
                new LatLng(22.3883, 114.2093), false, true, true));
        parks.add(new PetPark("城門河第一海濱花園", "新界", "沙田區",
                new LatLng(22.3813, 114.1893), false, true, false));
        parks.add(new PetPark("城門河第二海濱花園", "新界", "沙田區",
                new LatLng(22.3823, 114.1897), false, true, false));
        parks.add(new PetPark("城門河第三海濱花園", "新界", "沙田區",
                new LatLng(22.3833, 114.1903), false, true, true));
        parks.add(new PetPark("大水坑第一、二、三及四避雨亭", "新界", "沙田區",
                new LatLng(22.4083, 114.2177), false, true, false));
        parks.add(new PetPark("黃泥頭花園", "新界", "沙田區",
                new LatLng(22.3943, 114.2053), false, true, false));

        // New Territories - Tai Po District
        parks.add(new PetPark("下坑村休憩處", "新界", "大埔區",
                new LatLng(22.4423, 114.1677), false, true, false));
        parks.add(new PetPark("梅樹坑遊樂場", "新界", "大埔區",
                new LatLng(22.4483, 114.1767), false, true, true));
        parks.add(new PetPark("白石角海濱長廊 - 大埔段", "新界", "大埔區",
                new LatLng(22.4583, 114.1893), false, true, false));
        parks.add(new PetPark("大埔滘花園", "新界", "大埔區",
                new LatLng(22.4223, 114.1643), false, true, false));
        parks.add(new PetPark("大埔滘公園", "新界", "大埔區",
                new LatLng(22.4213, 114.1647), false, true, true));
        parks.add(new PetPark("大埔海濱公園", "新界", "大埔區",
                new LatLng(22.4531, 114.1717), true, true, false));
        parks.add(new PetPark("達運道休憩處", "新界", "大埔區",
                new LatLng(22.4513, 114.1687), false, true, false));
        parks.add(new PetPark("汀太路兒童遊樂場", "新界", "大埔區",
                new LatLng(22.4493, 114.1603), false, true, false));
        parks.add(new PetPark("吐露港花園", "新界", "大埔區",
                new LatLng(22.4553, 114.1747), false, true, false));
        parks.add(new PetPark("洞梓山路花園", "新界", "大埔區",
                new LatLng(22.4473, 114.1823), false, true, false));
        parks.add(new PetPark("元洲仔公園", "新界", "大埔區",
                new LatLng(22.4465, 114.1703), false, true, true));

        // New Territories - Tsuen Wan District
        parks.add(new PetPark("荃灣海濱單車匯合中心", "新界", "荃灣區",
                new LatLng(22.3673, 114.1167), true, true, true));
        parks.add(new PetPark("圓環公園", "新界", "荃灣區",
                new LatLng(22.3733, 114.1133), false, false, true));
        parks.add(new PetPark("海安路遊樂場", "新界", "荃灣區",
                new LatLng(22.3683, 114.1147), true, false, false));
        parks.add(new PetPark("國瑞路公園", "新界", "荃灣區",
                new LatLng(22.3703, 114.1197), true, true, true));
        parks.add(new PetPark("深慈街遊樂場", "新界", "荃灣區",
                new LatLng(22.3713, 114.1183), true, true, true));
        parks.add(new PetPark("荃景圍花園", "新界", "荃灣區",
                new LatLng(22.3753, 114.1153), true, false, false));
        parks.add(new PetPark("荃灣公園", "新界", "荃灣區",
                new LatLng(22.3661, 114.1174), false, true, true));
        parks.add(new PetPark("荃灣海濱公園", "新界", "荃灣區",
                new LatLng(22.3673, 114.1152), false, true, true));
        parks.add(new PetPark("蕙荃路休憩花園", "新界", "荃灣區",
                new LatLng(22.3683, 114.1193), false, false, false));
        parks.add(new PetPark("和宜合道花園", "新界", "荃灣區",
                new LatLng(22.3787, 114.1083), false, true, false));

        // New Territories - Tuen Mun District
        parks.add(new PetPark("舊咖啡灣休憩處", "新界", "屯門區",
                new LatLng(22.3583, 113.9643), false, true, false));
        parks.add(new PetPark("麒麟崗公眾公園", "新界", "屯門區",
                new LatLng(22.3813, 113.9747), false, true, false));
        parks.add(new PetPark("新安休憩處", "新界", "屯門區",
                new LatLng(22.3873, 113.9683), false, true, false));
        parks.add(new PetPark("掃管笏休憩處", "新界", "屯門區",
                new LatLng(22.3743, 113.9937), false, true, false));
        parks.add(new PetPark("井頭上村休憩處", "新界", "屯門區",
                new LatLng(22.3893, 113.9663), false, true, false));
        parks.add(new PetPark("青碧休憩處", "新界", "屯門區",
                new LatLng(22.3763, 113.9713), false, true, false));
        parks.add(new PetPark("華發遊樂場", "新界", "屯門區",
                new LatLng(22.3855, 113.9725), true, true, true));
        parks.add(new PetPark("湖山花園", "新界", "屯門區",
                new LatLng(22.3853, 113.9777), false, true, false));

        // New Territories - Yuen Long District
        parks.add(new PetPark("屏柏里公園", "新界", "元朗區",
                new LatLng(22.4433, 114.0237), false, true, false));
        parks.add(new PetPark("上村公園", "新界", "元朗區",
                new LatLng(22.4383, 114.0763), false, true, false));
        parks.add(new PetPark("天河路遊樂場", "新界", "元朗區",
                new LatLng(22.4493, 113.9973), false, false, true));
        parks.add(new PetPark("天慈花園", "新界", "元朗區",
                new LatLng(22.4683, 114.0037), false, true, false));
        parks.add(new PetPark("唐人新村遊樂場", "新界", "元朗區",
                new LatLng(22.4453, 114.0283), false, true, false));
        parks.add(new PetPark("橫台山遊樂場", "新界", "元朗區",
                new LatLng(22.4423, 114.0347), false, true, false));
        parks.add(new PetPark("宏業南街休憩花園", "新界", "元朗區",
                new LatLng(22.4473, 114.0333), false, false, false));

        return parks;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}