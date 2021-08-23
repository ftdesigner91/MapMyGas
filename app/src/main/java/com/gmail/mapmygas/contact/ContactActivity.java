package com.gmail.mapmygas.contact;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.gmail.mapmygas.MyConst;
import com.gmail.mapmygas.MyTouchEvent;
import com.gmail.mapmygas.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Objects;

public class ContactActivity extends AppCompatActivity {

    private static final String CONTACT_REF_PATH = "contact_station";

    private String station_id;
    private String station_name;

    private DatabaseReference contactRef;

    private FirebaseUser mUser;
    private String my_id;

    private FirebaseFirestore firestore;
    private DocumentReference notifCollection;

    private MessageAdapter adapter;

    private Toolbar nMsg_toolbar;
    private TextView nStation_name;
    private RecyclerView nMsg_rv;
    private EditText nMsg_et;
    private TextView nSend_msg_btn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contact);
        init();
        showToolbar();
        new MyTouchEvent(this).collapseKeyboard(nMsg_rv);

        nSend_msg_btn.setOnClickListener(view -> sendMsg());

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        nMsg_rv.setHasFixedSize(true);
        nMsg_rv.setLayoutManager(layoutManager);
        nMsg_rv.setAdapter(adapter);
    }

    private void init() {
        Bundle bundle = getIntent().getExtras();
        station_id = bundle.getString(MyConst.STATION_ID);
        station_name = bundle.getString(MyConst.STATION_NAME);

        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        mUser = mAuth.getCurrentUser();
        my_id = Objects.requireNonNull(mUser).getUid();

        FirebaseDatabase contactDB = FirebaseDatabase.getInstance();
        contactRef = contactDB.getReference(CONTACT_REF_PATH);

        Query query = contactRef.child(station_id).child(my_id);
        FirebaseRecyclerOptions<MessagesModel> options = new FirebaseRecyclerOptions.Builder<MessagesModel>()
                .setQuery(query, MessagesModel.class)
                .build();
        adapter = new MessageAdapter(options);

        firestore = FirebaseFirestore.getInstance();
        notifCollection = firestore.collection("stations").document(station_id)
                .collection("my_notifications").document(my_id);

        nMsg_toolbar = findViewById(R.id.msg_toolbar);
        nStation_name = findViewById(R.id.station_name);
        nMsg_rv = findViewById(R.id.msg_rv);
        nMsg_et = findViewById(R.id.msg_et);
        nSend_msg_btn = findViewById(R.id.send_msg_btn);
    }

    private void showToolbar(){
        setSupportActionBar(nMsg_toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayShowTitleEnabled(false);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        nStation_name.setText(station_name);
    }

    private void sendMsg(){
        String message = String.valueOf(nMsg_et.getText()).trim();
        DatabaseReference myMsgRef = contactRef.child(station_id).child(my_id);
        HashMap<String, Object> myMsgMap = new HashMap<>();
        myMsgMap.put("customer_name", mUser.getDisplayName());
        myMsgMap.put("customer_id", my_id);
        myMsgMap.put("message", message);
        if (!TextUtils.isEmpty(message)){
            myMsgRef.push().setValue(myMsgMap).addOnCompleteListener(task -> {
                if (task.isComplete()){
                    if (task.isSuccessful()){
                        nMsg_et.getText().clear();
                        nMsg_rv.smoothScrollToPosition(Objects.requireNonNull(nMsg_rv.getAdapter()).getItemCount());
                        sendNotification(message);
                    }
                    else {
                        nMsg_et.getText().clear();
                        Toast.makeText(this, Objects.requireNonNull(task.getException()).getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                }
            });
        }else {nMsg_et.getText().clear();}
    }
    private void sendNotification(String message) {
        notifCollection.get().addOnCompleteListener(task1 -> {
            if (task1.isComplete()){
                    if (!task1.getResult().exists()){
                        HashMap<String,String> notifMap = new HashMap<>();
                        notifMap.put("customer_name", mUser.getDisplayName());
                        notifMap.put("customer_id", my_id);
                        notifMap.put("message", message);
                        notifCollection.set(notifMap);
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
}