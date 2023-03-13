package com.example.qrranger;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import com.example.qrranger.R;
import com.google.firebase.firestore.DocumentSnapshot;

public class ProfileFragment extends Fragment {
    Player myUser = new Player();
    private TextView playerName;
    private TextView playerEmail;
    private Map<String, Object> value = new HashMap<>();
    private TextView playerPhoneNumb;
    private TextView playerTotalScore;
    private TextView playerTotalQRCodes;
    private TextView profileRank;
    private ImageView myAvatar;
    private ImageButton mySettButton;
    private Intent data;
    private ListView listView;

    private ActivityResultLauncher<Intent> startSettingsForResult =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == Activity.RESULT_OK) {
                            Intent data = result.getData();
                            if (data != null) {
                                boolean dataChanged = data.getBooleanExtra("dataChanged", false);
                                if (dataChanged) {
                                    System.out.println("Data changed");
                                    myUser = (Player) data.getSerializableExtra("myUser");
                                    setViews();
                                }
                            }
                        }
                    });

    PlayerCollection myPlayerCollection = new PlayerCollection(Database.getInstance());

    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        playerEmail = view.findViewById(R.id.ProfileUserEmail);
        playerName = view.findViewById(R.id.ProfileUserName);
        playerPhoneNumb = view.findViewById(R.id.ProfileUserPhoneNumber);
        playerTotalScore = view.findViewById(R.id.ProfileTS);
        playerTotalQRCodes = view.findViewById(R.id.ProfileQRNum);
        mySettButton = view.findViewById(R.id.ProfileSettingButton);
        profileRank = view.findViewById(R.id.ProfileRank);
        listView = view.findViewById(R.id.ProfileQR_list_view);

        UserState us = UserState.getInstance();
        String userID = us.getUserID();

        CompletableFuture<Boolean> future = myPlayerCollection.checkUserExists(userID);
        future.thenAccept(userExists -> {
                    if (userExists) {
                        // User exists
                        System.out.println("User exists");
                        setValues(userID);
                    } else {
                        // User does not exist, handle errors
                    }

                });

        mySettButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startSettingsActivity();
            }
        });

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                String name = adapterView.getItemAtPosition(i).toString();
            }
        });


        return view;
    }
    public void setValues(String userID){
        // get data from userID
        myPlayerCollection.read(userID, data -> {
            // player found so handle code using data here
            System.out.println("Data for user: " + data);
            myUser.setUserName(Objects.requireNonNull(data.get("username")).toString());
            myUser.setEmail(Objects.requireNonNull(data.get("email")).toString());
            myUser.setPhoneNumber(Objects.requireNonNull(data.get("phoneNumber")).toString());
            myUser.setTotalScore(((Long) data.get("totalScore")));
            myUser.setTotalQRCode(((Long) data.get("totalQRCode")));
            myUser.setGeoLocationSett((Boolean) data.get("geolocation_setting"));
            myUser.setPlayerId(userID);
            myUser.setQrCodeCollection((ArrayList<String>) data.get("qr_code_ids"));
            getAndSetRank(userID);
            System.out.println("Setting views");
            getAndSetList(userID);
            System.out.println("Setting List");
            setViews();
            }, error -> {
                // Player not found, cannot set values
                // perhaps change to default values for less chance of error during demo
                System.out.println("Error getting player data: " + error);
        });

    }

    public void setViews(){
        playerName.setText(myUser.getUserName());
        playerEmail.setText(myUser.getEmail());
        playerPhoneNumb.setText(myUser.getPhoneNumber());
        playerTotalScore.setText(myUser.getTotalScore().toString());
        playerTotalQRCodes.setText(myUser.getTotalQRCode().toString());
    }

    private void startSettingsActivity() {
        Intent intent = new Intent(getActivity(), SettingActivity.class);
        intent.putExtra("myUser", myUser); // pass the user data to the settings activity
        startSettingsForResult.launch(intent);
    }

    public void getAndSetRank(String userID){
        CompletableFuture<Integer> rankFuture = myPlayerCollection.getPlayerRank(userID);
        rankFuture.thenAccept(rank -> {
            System.out.println("Player rank: " + rank);
            profileRank.setText(rank.toString());
        }).exceptionally(e -> {
            System.err.println("Failed to get player rank: " + e.getMessage());
            return null;
        });
    }


    public void getAndSetList(String userID){
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // code that modifies the adapter
                ArrayList<String> qrCodeCollection = myUser.getQrCodeCollection();
                ArrayList<String> qrNames = new ArrayList<>();
                QRCollection qrc = new QRCollection(null);
                for (String qrCode : qrCodeCollection) {
                    qrc.read(qrCode, data -> {
                        // qr found
                        qrNames.add(data.get("name").toString());
                        if (qrNames.size() == qrCodeCollection.size()) {
                            // All QR names retrieved, update list view
                            ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(),
                                    android.R.layout.simple_list_item_1, qrNames);
                            listView.setAdapter(adapter);
                        }
                    }, error -> {
                        // qr not found, cannot set values
                        System.out.println("Error getting player data: " + error);
                    });

                }
            }
        });

    }
}