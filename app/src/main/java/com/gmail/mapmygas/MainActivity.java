package com.gmail.mapmygas;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_TO_GET_LOCATION = 100;

    private FirebaseAuth mAuth;
    private FirebaseUser mUser;

    private Toolbar nToolbar;

    private BottomNavigationView nMain_bottom_nav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        showAppbar();

        nMain_bottom_nav = findViewById(R.id.main_bottom_nav);

        mAuth = FirebaseAuth.getInstance();
        mUser = mAuth.getCurrentUser();

        nMain_bottom_nav.setSelectedItemId(R.id.home_tab);
        loadFragment(new HomeFragment());

        navLintener();

        checkPermissions();
    }

    private void navLintener() {
        nMain_bottom_nav.setOnNavigationItemSelectedListener(item ->
        {
            try {
                // Get selected item id
                int id = item.getItemId();
                // Load fragment
                loadFragment(getFragment(item.getItemId()));
            }catch (Exception e)
            {
                Toast.makeText(getApplicationContext(), "item selected: "+e.getMessage(), Toast.LENGTH_SHORT).show();
            }
            return true;
        });
    }

    private Fragment getFragment(int itemId) {
        if (itemId == R.id.home_tab)
        {
            return new HomeFragment();
        }
        else if (itemId == R.id.saves_tab)
        {
            return new SavesFragment();
        }
        else if (itemId == R.id.profile_tab)
        {
            return new ProfileFragment();
        }
        return new HomeFragment();
    }
    private void loadFragment(Fragment fragment) {
        try {

            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.main_frame_layout, fragment, fragment.getClass().getSimpleName())
                    .commit();
        }catch (Exception e)
        {
            Toast.makeText(this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void userStatusChecker() {
        if (mUser == null)
        {
            /*Intent signInIntent = new Intent(MainActivity.this, SignInActivity.class);
            startActivity(signInIntent);
            finish();*/
        }
    }

    private void showAppbar() {

        nToolbar = findViewById(R.id.toolbar);
        setSupportActionBar(nToolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayShowTitleEnabled(false);
        getSupportActionBar().setDisplayHomeAsUpEnabled(false);
    }

    private void permissionDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View permiDialogV = LayoutInflater.from(this).inflate(R.layout.permi_dialog, null, false);
        builder.setView(permiDialogV);
        AlertDialog dialog = builder.create();

        Button accept = permiDialogV.findViewById(R.id.accept_btn);
        Button cancel = permiDialogV.findViewById(R.id.cancel_btn);

        accept.setOnClickListener(view1 -> {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    PERMISSION_TO_GET_LOCATION);
            new Handler().postDelayed(dialog::dismiss, 900);
        });
        cancel.setOnClickListener(view1 -> dialog.dismiss());

        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        dialog.show();
    }

    private void checkPermissions() {
        if
        (
                ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
        )
        {
            permissionDialog();
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if
        (
                requestCode != PERMISSION_TO_GET_LOCATION
                && (grantResults.length > 0)
                && (grantResults[0] != PackageManager.PERMISSION_GRANTED)
        )
        {
            Toast.makeText(this, "Failed to get permission\nPlease try again", Toast.LENGTH_LONG).show();
        }
    }
}