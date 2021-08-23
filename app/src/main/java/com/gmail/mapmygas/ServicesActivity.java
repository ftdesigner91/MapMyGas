package com.gmail.mapmygas;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.squareup.picasso.Picasso;

import java.util.Objects;

public class ServicesActivity extends AppCompatActivity {

    private static final String STATION_SERVICE_COLLECTION = "my_services";
    private Bundle bundle;
    private String stationID;
    private String stationName;

    private Toolbar nServices_toolbar;
    private RecyclerView nServices_rv;

    private FirebaseFirestore firestore;
    private CollectionReference serviceCollection;
    private DocumentReference stationDoc;
    private Query query;
    private FirestoreRecyclerOptions<ServicesModel> options;
    private FirestoreRecyclerAdapter<ServicesModel, ServiceViewHolder> adapter;
    private GridLayoutManager layoutManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_services);
        init();
        showToolbar();

        adapter = new FirestoreRecyclerAdapter<ServicesModel, ServiceViewHolder>(options) {
            @Override
            protected void onBindViewHolder(@NonNull ServiceViewHolder holder, int position, @NonNull ServicesModel model) {
                Picasso.get().load(model.getService_image())
                        .placeholder(R.drawable.ic_logo_logo_large2_place_holder_copy)
                        .into(holder.nService_iv);
                holder.nService_title_tv.setText(model.getService_title());
                holder.nService_description_tv.setText(model.getService_description());
                holder.nService_btn.setOnClickListener(view -> openServiceActivity(model.getStation_id(), model.getMillis()));
            }

            @NonNull
            @Override
            public ServiceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.service_item, parent, false);
                return new ServiceViewHolder(view);
            }
        };
        if (layoutManager != null) {
            nServices_rv.setHasFixedSize(true);
            nServices_rv.setLayoutManager(layoutManager);
            nServices_rv.setAdapter(adapter);
        }
    }

    private void init() {
        bundle = getIntent().getExtras();
        stationID = bundle.getString(MyConst.STATION_ID);
        //stationName = bundle.getString(MyConst.STATION_NAME);


        nServices_toolbar = findViewById(R.id.services_toolbar);
        nServices_rv = findViewById(R.id.services_rv);

        firestore = FirebaseFirestore.getInstance();
        serviceCollection = firestore.collection(MyConst.STATION_COLLECTION).document(stationID).collection(STATION_SERVICE_COLLECTION);
        stationDoc = firestore.collection(MyConst.STATION_COLLECTION).document(stationID);
        query = serviceCollection;
        options = new FirestoreRecyclerOptions.Builder<ServicesModel>()
                .setQuery(query, ServicesModel.class)
                .build();
        layoutManager = new GridLayoutManager(this, 2);
    }

    private void showToolbar() {
        setSupportActionBar(nServices_toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayShowTitleEnabled(true);
        stationDoc.addSnapshotListener((value, error) -> {
            if (value != null){
                if (value.exists()){
                    String station_name = String.valueOf(value.get("station_name"));
                    Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
                    getSupportActionBar().setTitle(station_name);
                }
            }
        });
    }

    private void openServiceActivity(String stationID, String millis) {
        Intent serviceIntent = new Intent(ServicesActivity.this, ServiceActivity.class);
        serviceIntent.putExtra(MyConst.STATION_ID, stationID);
        serviceIntent.putExtra("millis", millis);
        startActivity(serviceIntent);
    }

    private static class ServiceViewHolder extends RecyclerView.ViewHolder {

        private final ImageView nService_iv;
        private final TextView nService_title_tv;
        private final TextView nService_description_tv;
        private final CardView nService_btn;
        public ServiceViewHolder(@NonNull View itemView) {
            super(itemView);
            nService_iv = itemView.findViewById(R.id.service_iv);
            nService_title_tv = itemView.findViewById(R.id.service_title_tv);
            nService_description_tv = itemView.findViewById(R.id.service_description_tv);
            nService_btn = itemView.findViewById(R.id.view_service_btn);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        adapter.startListening();
    }

    @Override
    protected void onStop() {
        super.onStop();
        adapter.stopListening();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        startActivity(new Intent(ServicesActivity.this, MainActivity.class));
    }
}
