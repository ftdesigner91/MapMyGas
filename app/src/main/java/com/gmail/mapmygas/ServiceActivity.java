package com.gmail.mapmygas;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.gmail.mapmygas.contact.ContactActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.squareup.picasso.Picasso;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Objects;

public class ServiceActivity extends AppCompatActivity {

    private static final String MY_BOOKINGS_COLLECTION = "my_bookings";
    private Bundle bundle;
    private String stationID;
    private String millis;
    private String stationName;

    private Toolbar nV_service_toolbar;
    private ImageView nService1_iv;
    private TextView nService1_title_tv;
    private TextView nService1_description_tv;
    private Button nBook_btn;

    private FirebaseFirestore firestore;
    private DocumentReference serviceDoc;
    private CollectionReference booking_notif_collection;
    private DocumentReference myBookingsDocument;

    private FirebaseAuth mAuth;
    private FirebaseUser mUser;

    private SimpleDateFormat simpleDateFormat;
    private Date date;
    private String currentDate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_service);
        init();
        showToolbar();

        serviceDoc.addSnapshotListener((value, error) -> {
           if (value != null && value.exists()){
               String service_image = String.valueOf(value.get("service_image"));
               String service_tite = String.valueOf(value.get("service_title"));
               String service_description = String.valueOf(value.get("service_description"));

               Picasso.get().load(service_image)
                       .placeholder(R.drawable.ic_logo_logo_large3_place_holder)
                       .into(nService1_iv);
               nService1_title_tv.setText(service_tite);
               nService1_description_tv.setText(service_description);
           }
        });

        nBook_btn.setOnClickListener(view -> openBookingActivity());
    }

    private void init() {
        bundle = getIntent().getExtras();
        stationID = bundle.getString(MyConst.STATION_ID);
        millis = bundle.getString("millis");

        nV_service_toolbar = findViewById(R.id.v_service_toolbar);
        nService1_iv = findViewById(R.id.service1_iv);
        nService1_title_tv = findViewById(R.id.service1_title_tv);
        nService1_description_tv = findViewById(R.id.service1_description_tv);
        nBook_btn = findViewById(R.id.book_btn);

        simpleDateFormat = new SimpleDateFormat("MM/dd/yyyy", Locale.getDefault());
        date = new Date();
        currentDate = simpleDateFormat.format(date);

        mAuth = FirebaseAuth.getInstance();
        mUser = mAuth.getCurrentUser();

        firestore = FirebaseFirestore.getInstance();
        serviceDoc = firestore.collection(MyConst.STATION_COLLECTION).document(stationID)
                .collection(MyConst.SERVICES_COLLECTION).document(millis);
        booking_notif_collection = firestore.collection(MyConst.STATION_COLLECTION).document(stationID)
                .collection(MyConst.BOOKING_NOTIFICATIONS_COLLECTION);
        if (mUser != null){
            myBookingsDocument = firestore.collection(MyConst.MY_COLLECTION).document(mUser.getUid())
                    .collection(MY_BOOKINGS_COLLECTION).document(stationID);
        }
    }

    private void showToolbar() {
        setSupportActionBar(nV_service_toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        Objects.requireNonNull(getSupportActionBar()).setDisplayShowTitleEnabled(true);
        nV_service_toolbar.setNavigationOnClickListener(view -> {
            Intent backIntent = new Intent(ServiceActivity.this, ServicesActivity.class);
            backIntent.putExtra(MyConst.STATION_ID, stationID);
            startActivity(backIntent);
            finish();
        });
        serviceDoc.addSnapshotListener((value, error) -> {
            if (value != null){
                if (Objects.requireNonNull(value).exists()){
                    String service_title = String.valueOf(value.get("service_title"));
                    getSupportActionBar().setTitle(service_title);
                }
            }
        });
    }

    private void openBookingActivity() {
        //myBookingsDocument 2nd
        if (mUser != null){
            String customer_name = mUser.getDisplayName();
            String title = String.valueOf(nService1_title_tv.getText());


            HashMap<String, String> notifMap = new HashMap<>();
            notifMap.put("cutomer_name", customer_name);
            notifMap.put("service_title", title);
            notifMap.put("booked_at", currentDate);

            booking_notif_collection.document().set(notifMap, SetOptions.merge()).addOnCompleteListener(task -> {
                if (task.isComplete()){
                    if (task.isSuccessful()){
                        successDialog(title);
                    }
                }
            });
        }
    }

    private void successDialog(String title) {
        String msg = "You have booked "+title+" successfully\ndo you like to contact the station?";
        AlertDialog.Builder builder = new AlertDialog.Builder(ServiceActivity.this);
        View view = LayoutInflater.from(getBaseContext()).inflate(R.layout.successful_booking_dialog, null, false);
        builder.setView(view);

        TextView nDia_msg_tv = view.findViewById(R.id.dialog_msg_tv);
        Button nDia_contact_btn = view.findViewById(R.id.dia_contact_btn);
        Button nDia_later_btn = view.findViewById(R.id.dia_later_btn);

        AlertDialog dialog = builder.create();

        nDia_msg_tv.setText(msg);
        nDia_contact_btn.setOnClickListener(view1 -> toContactActivity(stationID));
        nDia_later_btn.setOnClickListener(view1 -> dialog.dismiss());

        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.show();
    }
    private void toContactActivity(String station_id){
        Intent contactIntent = new Intent(ServiceActivity.this, ContactActivity.class);
        firestore.collection(MyConst.STATION_COLLECTION).document(station_id).get()
                .addOnCompleteListener(task -> {
                    if (task.isComplete()){
                        if (task.isSuccessful()){
                            String station_name = String.valueOf(task.getResult().get("station_name"));
                            contactIntent.putExtra("name", station_name);
                            contactIntent.putExtra("id", station_id);
                            startActivity(contactIntent);
                        }
                    }
                });
    }
}