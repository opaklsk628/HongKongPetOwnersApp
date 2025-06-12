package com.example.hongkongpetowners;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;

import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.FirebaseFirestore;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.example.hongkongpetowners.databinding.ActivityMainBinding;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private AppBarConfiguration appBarConfiguration;
    private ActivityMainBinding binding;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);

        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        appBarConfiguration = new AppBarConfiguration.Builder(navController.getGraph()).build();
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);

        // start firestore
        db = FirebaseFirestore.getInstance();

        // test firebase connect
        testFirebaseConnection();

        binding.fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // write data to firebase
                writeTestData(view);
            }
        });
    }

    private void testFirebaseConnection() {
// Firestore test read data
        db.collection("test")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Log.d(TAG, "Firebase connect success,file number: " + queryDocumentSnapshots.size());
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Firebase connect fail " + e.getMessage());
                });
    }

    private void writeTestData(View view) {
// create test data
        Map<String, Object> testData = new HashMap<>();
        testData.put("message", "Hello world");
        testData.put("timestamp", new Date());
        testData.put("testUser", "Android Developer");

// write data to firestore
        db.collection("test")
                .add(testData)
                .addOnSuccessListener(documentReference -> {
                    Log.d(TAG, "test success, file ID: " + documentReference.getId());
                    Snackbar.make(view, "data write to firebase", Snackbar.LENGTH_LONG)
                            .setAnchorView(R.id.fab)
                            .setAction("Action", null).show();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "write fail: " + e.getMessage());
                    Snackbar.make(view, "write fail: " + e.getMessage(), Snackbar.LENGTH_LONG)
                            .setAnchorView(R.id.fab)
                            .show();
                });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, appBarConfiguration)
                || super.onSupportNavigateUp();
    }
}