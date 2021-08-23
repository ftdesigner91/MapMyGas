package com.gmail.mapmygas;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class ProfileFragment extends Fragment {

    private static final int PERMISSION_TO_GET_LOCATION = 100;
    private static final int RC_SIGN_IN = 120;

    private View view;

    private ProgressBar nProgressBar;

    private LinearLayout nSigned_out_layout;
    private ImageView nLogin_logo_v;
    private SignInButton g_sign_in_button;

    private LinearLayout nSigned_in_layout;
    private ImageView nUser_iv;
    private TextView nCurrent_location;
    private LinearLayout nSign_out_btn;
    private TextView nUsername;

    private FirebaseAuth mAuth;
    private FirebaseUser mUser;

    private GoogleSignInClient mGoogleSignInClient;
    private GoogleSignInOptions gso;
    private GoogleSignInAccount account;

    private FirebaseFirestore firestore;
    private DocumentReference myDocument;


    private SimpleDateFormat simpleDateFormat;
    private Date date;

    // Date
    private String currentDateTime;

    // gps
    private FusedLocationProviderClient fusedLocationProviderClient;

    public ProfileFragment() {
        // Required empty public constructor
    }

    public static ProfileFragment newInstance(String param1, String param2) {
        ProfileFragment fragment = new ProfileFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_profile, container, false);
        init(view);
        setLayoutVisibility(nSigned_in_layout, nSigned_out_layout);

        signedInUserInfo();
        signedOutUserInfo();

        nLogin_logo_v.setImageResource(R.drawable.ic_logo_logo_large_place_holder);
        g_sign_in_button.setOnClickListener(view_in -> checkPermissions());
        nSign_out_btn.setOnClickListener(view1 -> signOut());
        return view;
    }

    private void signOut() {
        mAuth.signOut();
        mGoogleSignInClient.signOut();
        setLayoutVisibility(nSigned_in_layout, nSigned_out_layout);
    }

    private void progressbarVisibility(int visibility){
        nProgressBar.setVisibility(visibility);
    }

    private void init(View view) {
        nProgressBar = view.findViewById(R.id.progressBar);
        nSigned_out_layout = view.findViewById(R.id.signed_out_layout);
        g_sign_in_button = view.findViewById(R.id.g_sign_in);
        nLogin_logo_v = view.findViewById(R.id.login_logo_v);

        nSigned_in_layout = view.findViewById(R.id.signed_in_layout);
        nUser_iv = view.findViewById(R.id.user_iv);
        nCurrent_location = view.findViewById(R.id.current_location);
        nSign_out_btn = view.findViewById(R.id.sign_out_btn);
        nUsername = view.findViewById(R.id.username_tv);

        // Date
        simpleDateFormat = new SimpleDateFormat("hh:mm:ss dd/MM/yyyy", Locale.getDefault());
        date = Calendar.getInstance().getTime();
        currentDateTime = simpleDateFormat.format(date);
        /*SimpleDateFormat simpleDateFormat = new SimpleDateFormat("hh", Locale.getDefault());
        Date date = Calendar.getInstance().getTime();
        int currentTime = Integer.parseInt(simpleDateFormat.format(date));

        SimpleDateFormat currentDayFormat = new SimpleDateFormat("EEEE", Locale.getDefault());
        String today = currentDayFormat.format(date);*/
        mAuth = FirebaseAuth.getInstance();
        mUser = mAuth.getCurrentUser();

        firestore = FirebaseFirestore.getInstance();
        if (mUser != null){ myDocument = firestore.collection(MyConst.MY_COLLECTION).document(mUser.getUid()); }
        gso = new GoogleSignInOptions
                .Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken("381838828931-a5v84qhtpdhuuaeco6jg6jk1cj0q2n5p.apps.googleusercontent.com")
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(Objects.requireNonNull(getContext()), gso);

        //gps
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(getContext());
    }

    private void setLayoutVisibility(LinearLayout nSigned_in_layout, LinearLayout nSigned_out_layout){
        if (mUser != null){
            nSigned_out_layout.setVisibility(View.GONE);
            nSigned_in_layout.setVisibility(View.VISIBLE);
        }else {
            nSigned_in_layout.setVisibility(View.GONE);
            nSigned_out_layout.setVisibility(View.VISIBLE);
        }
    }

    private void signedInUserInfo() {
        if (mUser != null){
            Uri photoUrl = mUser.getPhotoUrl();
            nUsername.setText(mUser.getDisplayName());
            Picasso.get().load(photoUrl).into(nUser_iv);
            myDocument.addSnapshotListener((value, error) -> {
                if (Objects.requireNonNull(value).exists()){
                    if (value.get("user_address") != null){
                        String user_address = String.valueOf(value.get("user_address"));
                        nCurrent_location.setText(user_address);
                    }
                }
            });
        }
    }

    private void signedOutUserInfo() {

    }

    private void checkPermissions() {
        if (ActivityCompat.checkSelfPermission(Objects.requireNonNull(getContext()),
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        && ActivityCompat.checkSelfPermission(Objects.requireNonNull(getContext()),
                Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            signIn();
        } else {
            ActivityCompat.requestPermissions(Objects.requireNonNull(getActivity()),
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    PERMISSION_TO_GET_LOCATION);
        }
    }

    private void signIn() {
        progressbarVisibility(View.VISIBLE);
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener((Objects.requireNonNull(getActivity())), task -> {
                    if (task.isSuccessful()) {

                        Log.d("firebaseAuthWithGoogle", "signInWithCredential:success");
                        FirebaseUser user = mAuth.getCurrentUser();
                        addUserToFirestore(Objects.requireNonNull(user));
                        //updateUI(user);
                    } else {

                        Log.w("firebaseAuthWithGoogle", "signInWithCredential:failure", task.getException());
                        Toast.makeText(getContext(), "Failed to signed in", Toast.LENGTH_SHORT).show();
                        //updateUI(null);
                    }
                });
    }

    private void getUserLocationInfo() {
        if (ActivityCompat.checkSelfPermission(Objects.requireNonNull(getContext()),
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(Objects.requireNonNull(getContext()),
                Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            fusedLocationProviderClient.getLastLocation().addOnCompleteListener(task -> {

                Location location = task.getResult();
                if (location != null) {
                    try {
                        Geocoder geocoder = new Geocoder(getContext(), Locale.getDefault());
                        List<Address> addresses = geocoder.getFromLocation(
                                location.getLatitude(), location.getLongitude(), 1);
                        String addressLine = addresses.get(0).getAddressLine(0);

                        mAuth = FirebaseAuth.getInstance();
                        mUser = mAuth.getCurrentUser();
                        if (mUser != null){

                            myDocument = firestore.collection(MyConst.MY_COLLECTION).document(mUser.getUid());
                            HashMap<String, Double> usersMap = new HashMap<>();
                            usersMap.put("user_latitude", location.getLatitude());
                            usersMap.put("user_longitude", location.getLongitude());

                            myDocument.set(usersMap, SetOptions.merge()).addOnCompleteListener(task1 ->
                            {
                                if (task1.isComplete()) {
                                    if (task1.isSuccessful()) {
                                        HashMap<String, String> usersMap1 = new HashMap<>();
                                        usersMap1.put("user_address", addressLine);
                                        myDocument.set(usersMap1, SetOptions.merge()).addOnCompleteListener(task2 -> {
                                            if(task2.isComplete()){
                                                if (task2.isSuccessful()){
                                                    progressbarVisibility(View.GONE);
                                                    startActivity(new Intent(getContext(), MainActivity.class));
                                                    Toast.makeText(getContext(),
                                                            "Welcome "+mUser.getDisplayName(),
                                                            Toast.LENGTH_SHORT).show();
                                                }
                                            }
                                        });
                                    }else {
                                        Toast.makeText(getContext(),
                                                "" + Objects.requireNonNull(task1.getException()).getMessage(),
                                                Toast.LENGTH_LONG).show();
                                    }
                                }
                            });
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
        }else {
            ActivityCompat.requestPermissions(Objects.requireNonNull(getActivity()),
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    PERMISSION_TO_GET_LOCATION);
        }
    }

    private void addUserToFirestore(FirebaseUser user) {
        DocumentReference users_collection = firestore.collection("users")
                .document(user.getUid());
        HashMap<String, String> usersMap = new HashMap<>();
        usersMap.put("username", user.getDisplayName());
        usersMap.put("user_photo", String.valueOf(user.getPhotoUrl()));
        usersMap.put("user_email", user.getEmail());
        usersMap.put("date", currentDateTime);
        users_collection.set(usersMap, SetOptions.merge()).addOnCompleteListener(task1 ->
        {
            if (task1.isComplete()) {
                if (!task1.isSuccessful()) {
                    Toast.makeText(getContext(),
                            "" + Objects.requireNonNull(task1.getException()).getMessage(),
                            Toast.LENGTH_LONG).show();
                }
                else {
                    getUserLocationInfo();
                }
            }
        });
    }

    private void handleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            account = completedTask.getResult(ApiException.class);

            //updateUI(account);
        } catch (ApiException e) {

            Log.w("handleSignInResult", "signInResult:failed code = " + e.getStatusCode());
            //updateUI(null);
        }
    }

    private void revokeAccess() {
        mGoogleSignInClient.revokeAccess()
                .addOnCompleteListener((Objects.requireNonNull(getActivity())), task -> {
                    // ...
                });
    }

    private void updateUI(FirebaseUser currentUser) {
        if (currentUser != null) {
            try {
                Intent mainIntnet = new Intent(getContext(), MainActivity.class);
                startActivity(mainIntnet);
                Objects.requireNonNull(getActivity()).finish();

            } catch (Exception e) {
                Log.e("updateUI_if_not_null", e.getMessage());
            }
        }
    }
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {

            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                // Google Sign In was successful, authenticate with Firebase
                account = task.getResult(ApiException.class);
                //Log.d(TAG, "firebaseAuthWithGoogle:" + account.getId());
                firebaseAuthWithGoogle(account.getIdToken());
                handleSignInResult(task);
            } catch (ApiException e) {
                // Google Sign In failed, update UI appropriately
                Log.w("google sign in", "Google sign in failed", e);
                // ...
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_TO_GET_LOCATION
                && (grantResults.length > 0)
                && (grantResults[0] == PackageManager.PERMISSION_GRANTED)){
            signIn();
        }
        else {
            Toast.makeText(getContext(), "Failed to get permission\nPlease try again", Toast.LENGTH_LONG).show();
        }
    }
}