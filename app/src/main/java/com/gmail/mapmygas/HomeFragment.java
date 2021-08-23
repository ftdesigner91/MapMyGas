package com.gmail.mapmygas;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.text.TextUtils;
import android.transition.AutoTransition;
import android.transition.TransitionManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.gmail.mapmygas.contact.ContactActivity;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.SetOptions;

import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class HomeFragment extends Fragment {

    private RecyclerView nHome_rv;
    private SearchView nSearchView;
    private FloatingActionButton nRefresh_btn;

    private FirestoreRecyclerOptions<StationModel> options;
    private FirestoreRecyclerAdapter<StationModel, MainViewHolder> adapter;
    private FirebaseFirestore firestore;
    private Query query;
    private CollectionReference workingDayTimeColl;
    private DocumentReference myDocument;
    private CollectionReference favCollection;

    private FirebaseAuth mAuth;
    private FirebaseUser mUser;

    private FusedLocationProviderClient fusedLocationProviderClient;

    private View view;

    public HomeFragment() {
        // Required empty public constructor
    }


    public static HomeFragment newInstance(String param1, String param2) {
        HomeFragment fragment = new HomeFragment();
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
        view = inflater.inflate(R.layout.fragment_home, container, false);
        init(view);
        new MyTouchEvent(getContext()).collapseKeyboard(nHome_rv);

        adapter = new FirestoreRecyclerAdapter<StationModel, MainViewHolder>(options) {
            @NonNull
            @Override
            public MainViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.main_rv_item, parent, false);
                return new MainViewHolder(view);
            }

            @Override
            protected void onBindViewHolder(@NonNull MainViewHolder holder, int position, @NonNull StationModel model) {
                workingDayTimeColl = firestore.collection(MyConst.STATION_COLLECTION)
                        .document(model.getStation_id()).collection(MyConst.WORKING_DAYS_TIME);
                DocumentReference daysDocument = workingDayTimeColl.document(MyConst.DAYS);
                DocumentReference hoursDocument = workingDayTimeColl.document(MyConst.HOURS);
                double staLatitude = model.getLatitude();
                double staLongitude = model.getLongitude();
                holder.nStation_name.setText(model.getStation_name());
                holder.nStation_address.setText("Address: "+model.getstation_address());
                if (mUser != null){
                    activateFavBtn(model.getStation_id(), holder.nFav_btn);
                }
                holder.nFav_btn.setOnClickListener(view1 ->
                        addToFavList(model.getStation_name(), model.getstation_address(),
                                model.getStation_id(), holder.nHome_progress_bar,
                                model.getLatitude(), model.getLongitude()));
                holder.nDirections_btn.setOnClickListener(view1 -> getDirections(staLatitude, staLongitude));
                holder.nServices_btn.setOnClickListener(view1 -> toServicesActivity(model.getStation_id()));
                holder.nContact_btn.setOnClickListener(view1 -> {
                    if (model.getStation_id() != null && model.getStation_name() != null){
                        toContactActivity(model.getStation_id());
                    }else {
                        Toast.makeText(getContext(), "something went wrong!\nplease try again", Toast.LENGTH_LONG).show();
                    }
                });

                if (mUser != null){
                    myDocument.addSnapshotListener((value, error) -> {
                        try {
                            Long myLati = value.getLong(MyConst.MY_LAT_FIELD);
                            Long myLongi = value.getLong(MyConst.MY_LON_FIELD);
                            String stationDistance = "Distance: "+
                                    new DecimalFormat("#.#")
                                            .format(mi2K(distance(myLati, myLongi, staLatitude, staLongitude)))+" k";

                            holder.nDistance_tv.setText(stationDistance);
                        }catch (NullPointerException e){
                            e.printStackTrace();
                            Log.e("My longitude and latitude error:\n",
                                    "Localized Msg= "+e.getLocalizedMessage()+"\n"
                                            +"Msg= "+e.getMessage()+"\n"+
                                            "Cause= "+e.getCause());
                        }
                    });
                }
                else { holder.nDistance_tv.setText("Calculating Distance . . ."); }
                daysDocument.addSnapshotListener((value, error) -> {
                    if (Objects.requireNonNull(value).exists()){
                        String day = String.valueOf(value.get(getCurrentday()));
                        if (day.equals("ON")){
                            hoursDocument.addSnapshotListener((value1, error1) -> {
                                Long startHr = value1.getLong(MyConst.START_HOUR);
                                Long endHr = value1.getLong(MyConst.END_HOUR);
                                Long startMin = value1.getLong(MyConst.START_MIN);
                                Long endMin = value1.getLong(MyConst.END_MIN);

                                if (getHour() >= startHr && getMinute() >= startMin){
                                    if (getHour() <= endHr && getMinute() <= endMin){
                                        holder.nStation_status.setText(MyConst.STATION_OPEN);
                                        updateStationStatus(model, MyConst.OPEN);
                                    }else {
                                        updateStationStatus(model, MyConst.CLOSED);
                                        holder.nStation_status.setText(MyConst.STATION_CLOSED);
                                    }
                                }else {
                                    updateStationStatus(model, MyConst.CLOSED);
                                    holder.nStation_status.setText(MyConst.STATION_CLOSED);
                                }
                            });
                        }
                        else {
                            updateStationStatus(model, MyConst.CLOSED);
                            holder.nStation_status.setText(MyConst.STATION_CLOSED);
                        }
                    }
                });

            }
        };
        nHome_rv.setHasFixedSize(true);
        nHome_rv.setLayoutManager(new LinearLayoutManager(getContext()));
        nHome_rv.setAdapter(adapter);

        searchBar();

        nRefresh_btn.setOnClickListener(view1 -> refresh());

        return view;
    }

    private void init(View view) {
        nHome_rv = view.findViewById(R.id.home_rv);
        nSearchView = view.findViewById(R.id.search_et);
        nRefresh_btn = view.findViewById(R.id.refresh_btn);

        firestore = FirebaseFirestore.getInstance();
        query = firestore.collection(MyConst.STATION_COLLECTION);

        mAuth = FirebaseAuth.getInstance();
        mUser = mAuth.getCurrentUser();

        options = new FirestoreRecyclerOptions.Builder<StationModel>()
                .setQuery(query, StationModel.class)
                .build();
        if (mUser != null){
            myDocument = firestore.collection(MyConst.MY_COLLECTION).document(mUser.getUid());
            favCollection = firestore.collection(MyConst.MY_COLLECTION).document(mUser.getUid())
                    .collection(MyConst.FAV_LIST_COLLECTION);
        }

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(Objects.requireNonNull(getContext()));
    }

    private void toServicesActivity(String stationID) {
        if (stationID != null){
            Intent servicesIntent = new Intent(getContext(), ServicesActivity.class);
            servicesIntent.putExtra(MyConst.STATION_ID, stationID);
            startActivity(servicesIntent);
        }
    }

    private void updateStationStatus(@NonNull StationModel model, String status) {
        if (mUser != null){
            HashMap<String, String> stationStatusMap = new HashMap<>();
            stationStatusMap.put(MyConst.STATION_STATUS, status);
            DocumentReference document = firestore.collection(MyConst.MY_COLLECTION).document(mUser.getUid())
                    .collection(MyConst.FAV_LIST_COLLECTION).document(model.getStation_id());
            document.addSnapshotListener((value, error) -> {
                if (Objects.requireNonNull(value).get(MyConst.STATION_ID) != null){ document.set(stationStatusMap, SetOptions.merge()); }
            });
        }

    }

    private void activateFavBtn(String id, ImageButton imageButton) {
        if(mUser != null){
            favCollection.document(id).addSnapshotListener((value, error) -> {
                if(Objects.requireNonNull(value).get(MyConst.STATION_ID) != null){
                    imageButton.setImageTintList(ColorStateList.valueOf(ContextCompat
                            .getColor(Objects.requireNonNull(getContext()), R.color.c_yellow)));
                }
            });
        }
    }

    private void refresh() {
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

                        HashMap<String, Double> latiLongiMap = new HashMap<>();
                        latiLongiMap.put(MyConst.MY_LAT_FIELD, location.getLatitude());
                        latiLongiMap.put(MyConst.MY_LON_FIELD, location.getLongitude());

                        myDocument.set(latiLongiMap, SetOptions.merge()).addOnCompleteListener(task1 ->
                        {
                            if (task1.isComplete()) {
                                if (task1.isSuccessful()) {
                                    Toast.makeText(getContext(),
                                            "refreshed!",
                                            Toast.LENGTH_SHORT).show();
                                }else {
                                    Toast.makeText(getContext(),
                                            "" + Objects.requireNonNull(task1.getException()).getMessage(),
                                            Toast.LENGTH_LONG).show();
                                }
                            }
                        });
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
        }else {
            ActivityCompat.requestPermissions(Objects.requireNonNull(getActivity()),
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    MyConst.PERMISSION_TO_GET_LOCATION);
        }
    }

    private void searchBar() {
        nSearchView.setOnSearchClickListener(view1 -> TransitionManager
                .beginDelayedTransition(nSearchView, new AutoTransition()));

        nSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener()
        {
            @Override
            public boolean onQueryTextSubmit(String input) {
                // when submitting
                searchUserInput(firestore, input.trim().toLowerCase());
                return false;
            }
            @Override
            public boolean onQueryTextChange(String input)
            {
                // when typing
                searchUserInput(firestore, input.trim().toLowerCase());
                return false;
            }
        });
    }
    private void searchUserInput(FirebaseFirestore firestore, String input) {
        Query query;
        if (!TextUtils.isEmpty(input)) {
            query = firestore.collection(MyConst.STATION_COLLECTION)
                    .whereGreaterThanOrEqualTo(MyConst.STATION_NAME, input);
        }
        else
        {
            query = firestore.collection(MyConst.STATION_COLLECTION);
        }
        options = new FirestoreRecyclerOptions.Builder<StationModel>()
                .setQuery(query, StationModel.class)
                .build();

        adapter.updateOptions(options);
    }

    private String getCurrentday() {
        Date date = new Date();
        SimpleDateFormat outFormat = new SimpleDateFormat("EEEE", Locale.getDefault());
        return outFormat.format(date);
    }
    private int getHour(){
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("hh", Locale.getDefault());
        Date date = Calendar.getInstance().getTime();
        return convert24to12(Integer.parseInt(simpleDateFormat.format(date)));
    }
    private int convert24to12(int hr){
        if (hr > 12){
            return hr - 12;
        }else if (hr == 0){
            return hr+12;
        }
        else return hr;
    }
    private int getMinute(){
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("mm", Locale.getDefault());
        Date date = Calendar.getInstance().getTime();
        return Integer.parseInt(simpleDateFormat.format(date));
    }

    private static class MainViewHolder extends RecyclerView.ViewHolder {

        private final TextView nStation_name;
        private final TextView nStation_address;
        private final TextView nStation_status;
        private final TextView nDistance_tv;
        private final ImageButton nFav_btn;
        private final ProgressBar nHome_progress_bar;
        private final Button nDirections_btn;
        private final Button nServices_btn;
        private final Button nContact_btn;

        public MainViewHolder(@NonNull View itemView) {
            super(itemView);

            nStation_name = itemView.findViewById(R.id.station_name_tv);
            nStation_address = itemView.findViewById(R.id.station_address_tv);
            nStation_status = itemView.findViewById(R.id.station_status_tv);
            nDistance_tv = itemView.findViewById(R.id.distance_tv);
            nFav_btn = itemView.findViewById(R.id.fav_btn);
            nHome_progress_bar = itemView.findViewById(R.id.home_progress_bar);
            nDirections_btn = itemView.findViewById(R.id.directions_btn);
            nContact_btn = itemView.findViewById(R.id.contact_btn);
            nServices_btn = itemView.findViewById(R.id.services_btn);
        }
    }

    private void getDirections(double staLatitude, double staLongitude) {
        Uri gmIntentUri = Uri.parse(MyConst.GOOGLE_NAVIGATION_URI + staLatitude +","+ staLongitude);
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmIntentUri);
        mapIntent.setPackage(MyConst.PACKAGE_NAME);
        if (staLatitude != 0.0 && staLongitude != 0.0){ startActivity(mapIntent); }
        else {
            Toast.makeText(getContext(),
                    "Location unavailable! please save\nand try again later",
                    Toast.LENGTH_LONG).show();
        }
    }

    private double distance(double lat1, double lon1, double lat2, double lon2) {
        double theta = lon1 - lon2;
        double dist = Math.sin(deg2rad(lat1))
                * Math.sin(deg2rad(lat2))
                + Math.cos(deg2rad(lat1))
                * Math.cos(deg2rad(lat2))
                * Math.cos(deg2rad(theta));
        dist = Math.acos(dist);
        dist = rad2deg(dist);
        dist = dist * 60 * 1.1515;
        return (dist);
    }
    private double mi2K(double mi){return mi * 1.6;}
    private double deg2rad(double deg) {
        return (deg * Math.PI / 180.0);
    }
    private double rad2deg(double rad) {
        return (rad * 180.0 / Math.PI);
    }

    private void setProgressbarVisibilty(ProgressBar progressBar){
        if (progressBar.getVisibility() == View.INVISIBLE){
            progressBar.setVisibility(View.VISIBLE);
        }else {
            progressBar.setVisibility(View.INVISIBLE);
        }
    }
    private void addToFavList(String station_name, String station_address, String station_id,
                              ProgressBar progress_bar,
                              double latitude, double longitude){

        if (mUser != null){
            setProgressbarVisibilty(progress_bar);
            DocumentReference fav_list_document = firestore.collection(MyConst.MY_COLLECTION).document(mUser.getUid())
                    .collection(MyConst.FAV_LIST_COLLECTION)
                    .document(station_id);

            fav_list_document.get().addOnCompleteListener(task -> {
                if (task.isComplete()){
                    if (task.isSuccessful()){
                        if (!task.getResult().exists()){
                            HashMap<String, String> fav_list_map = new HashMap<>();
                            fav_list_map.put(MyConst.STATION_ID, station_id);
                            fav_list_map.put(MyConst.STATION_NAME, station_name);
                            fav_list_map.put(MyConst.STATION_ADDRESS, station_address);
                            fav_list_document.set(fav_list_map, SetOptions.merge()).addOnCompleteListener(task1 -> {
                                if (task1.isComplete()){
                                    if (!task1.isSuccessful()){
                                        setProgressbarVisibilty(progress_bar);
                                        Toast.makeText(getContext(), Objects.requireNonNull(task1.getException()).getMessage(),
                                                Toast.LENGTH_LONG).show();
                                    }else {
                                        HashMap<String, Double> fav_list_map1 = new HashMap<>();
                                        fav_list_map1.put(MyConst.STATION_LATITUDE, latitude);
                                        fav_list_map1.put(MyConst.STATION_LONGITUDE, longitude);
                                        fav_list_document.set(fav_list_map1, SetOptions.merge()).addOnCompleteListener(task2 -> {
                                            if (task2.isComplete()){
                                                if (!task2.isSuccessful()){
                                                    Toast.makeText(getContext(), Objects.requireNonNull(task2.getException()).getMessage(),
                                                            Toast.LENGTH_LONG).show();
                                                }else {
                                                    setProgressbarVisibilty(progress_bar);
                                                }
                                            }
                                        });
                                    }
                                }
                            });
                        }else {
                            setProgressbarVisibilty(progress_bar);
                            Toast.makeText(getContext(), station_name+" is already added to your list",
                                    Toast.LENGTH_LONG).show();
                        }
                    }
                }
            });
        }
    }

    private void toContactActivity(String station_id){
        Intent contactIntent = new Intent(getContext(), ContactActivity.class);
        firestore.collection(MyConst.STATION_COLLECTION).document(station_id).get()
                .addOnCompleteListener(task -> {
                    if (task.isComplete()){
                        if (task.isSuccessful()){
                            contactIntent.putExtra(MyConst.STATION_ID, station_id);
                            startActivity(contactIntent);
                        }
                    }
                });
    }
    @Override
    public void onStart() {
        super.onStart();
        adapter.startListening();
    }
    @Override
    public void onStop() {
        super.onStop();
        adapter.stopListening();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == MyConst.PERMISSION_TO_GET_LOCATION
                && (grantResults.length > 0)
                && (grantResults[0] == PackageManager.PERMISSION_GRANTED)){
            refresh();
        }
        else {
            Toast.makeText(getContext(), "Failed to get permission\nPlease try again", Toast.LENGTH_LONG).show();
        }
    }
}