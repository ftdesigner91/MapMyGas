package com.gmail.mapmygas;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.gmail.mapmygas.contact.ContactActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.text.DecimalFormat;
import java.util.Objects;


public class SavesFragment extends Fragment {

    private RecyclerView nSaves_rv;

    private FirebaseFirestore firestore;
    private CollectionReference savesCollections;
    private Query query;
    private FirestoreRecyclerOptions<SavesModel> options;
    private FirestoreRecyclerAdapter<SavesModel, SavesViewHolder> adapter;
    private DocumentReference myDocument;

    private FirebaseAuth mAuth;
    private FirebaseUser mUser;

    private View view;

    public SavesFragment() {
        // Required empty public constructor
    }

    public static SavesFragment newInstance(String param1, String param2) {
        SavesFragment fragment = new SavesFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_saves, container, false);
        init(view);
        if (mUser != null){
            adapter = new FirestoreRecyclerAdapter<SavesModel, SavesViewHolder>(options) {
                @NonNull
                @Override
                public SavesViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                    view = LayoutInflater.from(parent.getContext()).inflate(R.layout.saves_item, parent, false);
                    return new SavesViewHolder(view);
                }

                @Override
                protected void onBindViewHolder(@NonNull SavesViewHolder holder, int position, @NonNull SavesModel model) {
                    ColorStateList openColor = ContextCompat.getColorStateList(Objects.requireNonNull(getContext()),
                            R.color.c_green);
                    ColorStateList closeColor = ContextCompat.getColorStateList(Objects.requireNonNull(getContext()),
                            R.color.design_default_color_error);
                    holder.nSaves_name_tv.setText(model.getStation_name());
                    holder.nSaves_address_tv.setText(model.getStation_address());
                    if (model.getStation_status().equals(MyConst.OPEN)){
                        holder.nSaves_status_tv.setBackgroundTintList(openColor);
                        holder.nSaves_status_tv.setText(model.getStation_status());
                    }else {
                        holder.nSaves_status_tv.setBackgroundTintList(closeColor);
                        holder.nSaves_status_tv.setText(model.getStation_status());
                    }
                    if (mUser != null){
                        myDocument.addSnapshotListener((value, error) -> {
                            try {
                                Long myLatitude = value.getLong("user_latitude");
                                Long myLongitude = value.getLong("user_longitude");
                                String stationDistance = "Distance: "+
                                        new DecimalFormat("#.#")
                                                .format(mi2K(getDistans(myLatitude, myLongitude,
                                                        model.getLatitude(), model.getLongitude())))+" K";
                                holder.nSaves_distance_tv.setText(stationDistance);
                            }catch (NullPointerException e){
                                e.printStackTrace();
                                Log.e("My longitude and latitude error:\n",
                                        "Localized Msg= "+e.getLocalizedMessage()+"\n"
                                                +"Msg= "+e.getMessage()+"\n"+
                                                "Cause= "+e.getCause());
                            }
                        });
                    }else { holder.nSaves_distance_tv.setText("Calculating Distance . . ."); }
                    holder.nSaves_dircetions_btn.setOnClickListener(view1 -> getDirections(model.getLatitude(), model.getLongitude()));
                    holder.nSaves_service_btn.setOnClickListener(view1 -> openServicesActivity(model.getStation_id()));
                    holder.nSaves_contact_btn.setOnClickListener(view1 -> toContactActivity(model.getStation_id()));
                }
            };
        }
        nSaves_rv.setHasFixedSize(true);
        nSaves_rv.setLayoutManager(new LinearLayoutManager(getContext()));
        nSaves_rv.setAdapter(adapter);
        return view;
    }

    private void openServicesActivity(String station_id) {
        Intent servicseActivity = new Intent(getContext(), ServicesActivity.class);
        servicseActivity.putExtra(MyConst.STATION_ID, station_id);
        startActivity(servicseActivity);
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

    private void init(View view) {
        mAuth = FirebaseAuth.getInstance();
        mUser = mAuth.getCurrentUser();
        nSaves_rv = view.findViewById(R.id.saves_rv);

        firestore = FirebaseFirestore.getInstance();
        if (mUser != null){
            myDocument = firestore.collection(MyConst.MY_COLLECTION).document(mUser.getUid());
            savesCollections = myDocument.collection(MyConst.FAV_LIST_COLLECTION);
            query = savesCollections;
            options = new FirestoreRecyclerOptions.Builder<SavesModel>()
                    .setQuery(query, SavesModel.class)
                    .build();
        }
    }

    private double getDistans(double lat1, double lon1, double lat2, double lon2) {
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

    private void getDirections(double staLatitude, double staLongitude) {
        Uri gmIntentUri = Uri.parse(MyConst.GOOGLE_NAVIGATION_URI + staLatitude +","+ staLongitude);
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmIntentUri);
        mapIntent.setPackage(MyConst.PACKAGE_NAME);
        if (staLatitude != 0.0 && staLongitude != 0.0){ startActivity(mapIntent); }
        else {
            Toast.makeText(getContext(),
                    "Location unavailable! please try again later",
                    Toast.LENGTH_LONG).show();
        }
    }

    private static class SavesViewHolder extends RecyclerView.ViewHolder {

        private final TextView nSaves_status_tv;
        private final TextView nSaves_address_tv;
        private final TextView nSaves_name_tv;
        private final TextView nSaves_distance_tv;
        private final Button nSaves_dircetions_btn;
        private final Button nSaves_service_btn;
        private final Button nSaves_contact_btn;

        public SavesViewHolder(@NonNull View itemView) {
            super(itemView);
            nSaves_status_tv = itemView.findViewById(R.id.saves_status_tv);
            nSaves_address_tv = itemView.findViewById(R.id.saves_address_tv);
            nSaves_name_tv = itemView.findViewById(R.id.saves_name_tv);
            nSaves_distance_tv = itemView.findViewById(R.id.saves_distance_tv);
            nSaves_dircetions_btn = itemView.findViewById(R.id.saves_directions_btn);
            nSaves_service_btn = itemView.findViewById(R.id.saves_services_btn);
            nSaves_contact_btn = itemView.findViewById(R.id.saves_contact_btn);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if (adapter != null){ adapter.startListening(); }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (adapter != null){ adapter.stopListening(); }
    }
}