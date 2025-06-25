package com.example.hongkongpetownersapp;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.example.hongkongpetownersapp.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private AppBarConfiguration appBarConfiguration;
    private ActivityMainBinding binding;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private NavController navController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);

        navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        appBarConfiguration = new AppBarConfiguration.Builder(navController.getGraph()).build();
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // Listen to navigation changes
        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            // Update menu visibility based on current destination
            invalidateOptionsMenu();
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Only show menu if user is logged in and not on login screen
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null && navController.getCurrentDestination() != null
                && navController.getCurrentDestination().getId() != R.id.FirstFragment) {
            getMenuInflater().inflate(R.menu.menu_main, menu);
            return true;
        }
        return false;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_account_settings) {
            showAccountSettings();
            return true;
        } else if (id == R.id.action_sign_out) {
            showSignOutConfirmation();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void showAccountSettings() {
        String[] options = {
                getString(R.string.change_password),
                getString(R.string.change_email),
                getString(R.string.delete_account)
        };

        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.account_settings))
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            changePassword();
                            break;
                        case 1:
                            changeEmail();
                            break;
                        case 2:
                            deleteAccount();
                            break;
                    }
                })
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }

    private void changePassword() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null && currentUser.getEmail() != null) {
            mAuth.sendPasswordResetEmail(currentUser.getEmail())
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(this,
                                    getString(R.string.password_reset_email_sent),
                                    Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(this,
                                    getString(R.string.password_reset_failed),
                                    Toast.LENGTH_LONG).show();
                        }
                    });
        }
    }

    private void changeEmail() {
        Toast.makeText(this, getString(R.string.feature_coming_soon), Toast.LENGTH_SHORT).show();
        // Implementation for email change would require re-authentication
    }

    private void deleteAccount() {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.delete_account))
                .setMessage(getString(R.string.delete_account_confirmation))
                .setPositiveButton(getString(R.string.delete), (dialog, which) -> {
                    FirebaseUser currentUser = mAuth.getCurrentUser();
                    if (currentUser != null) {
                        currentUser.delete()
                                .addOnCompleteListener(task -> {
                                    if (task.isSuccessful()) {
                                        Toast.makeText(this,
                                                getString(R.string.account_deleted),
                                                Toast.LENGTH_SHORT).show();
                                        // Navigate to login screen
                                        navController.navigate(R.id.FirstFragment);
                                    } else {
                                        Toast.makeText(this,
                                                getString(R.string.delete_account_failed),
                                                Toast.LENGTH_LONG).show();
                                    }
                                });
                    }
                })
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }

    private void showSignOutConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.logout_button))
                .setMessage(getString(R.string.logout_confirmation))
                .setPositiveButton(getString(R.string.logout_button), (dialog, which) -> {
                    mAuth.signOut();
                    Toast.makeText(this, getString(R.string.logout_success), Toast.LENGTH_SHORT).show();
                    // Navigate to login screen
                    navController.navigate(R.id.FirstFragment);
                })
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, appBarConfiguration)
                || super.onSupportNavigateUp();
    }
}