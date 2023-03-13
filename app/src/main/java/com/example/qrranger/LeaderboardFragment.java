package com.example.qrranger;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.qrranger.R;

public class LeaderboardFragment extends Fragment {

    TextView rank1Username;
    TextView rank2Username;
    TextView rank3Username;
    TextView rank1Score;
    TextView rank2Score;
    TextView rank3Score;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_leaderboard, container, false);

        rank1Username = view.findViewById(R.id.Rank1Username);
        rank2Username = view.findViewById(R.id.Rank2Username);
        rank3Username = view.findViewById(R.id.Rank3Username);
        rank1Score = view.findViewById(R.id.Rank1Score);
        rank2Score = view.findViewById(R.id.Rank2Score);
        rank3Score = view.findViewById(R.id.Rank3Score);

        PlayerCollection pc = new PlayerCollection(null);
        pc.getTop3Players(
                top3Players -> {
                    // handle successful retrieval of top 3 players
                    // top3Players is a List<Map<String, Object>> containing the data of the top 3 players
                    System.out.println("Top 3 Players: " + top3Players);
                },
                error -> {
                    // handle error
                    // error is an Exception object containing details about the error
                    System.out.println("Error getting top 3 players.");
                }
        );


        // get rank 1 user
        // get rank 2 user
        // get rank 3 user


        // set views


        return view;
    }
}