package com.example.qrranger;

import static android.content.ContentValues.TAG;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.protobuf.FieldMask;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class PlayerCollection extends Database_Controls {

    CollectionReference collection;

    // Instantiating Database class with variable db
    public PlayerCollection(Database db) {
        if (db == null) {
            db = Database.getInstance();
        }
        collection = db.getCollection("players");
    }

    @Override
    void create(Map values) {
        // adds the given values into the database
        collection.add(values).addOnCompleteListener(new OnCompleteListener<DocumentReference>() {
            @Override
            public void onComplete(@NonNull Task<DocumentReference> task) {
                if (task.isSuccessful()) {
                    Log.d(TAG, "onComplete: Finished");
                } else {
                    Log.d(TAG, "UnSuccessful");
                }
            }
        });
        // can be run with:
        // Map<String, Object> values;
        // PlayerCollection pc = new PlayerCollection(null)
        // values = createValues(...) can put null values
        // pc.create(values)
    }

    @Override
    public void read(String userID, Consumer<Map<String, Object>> onSuccess, Consumer<Exception> onError) {
        // returns the data for a user with the given userID
        // not 100%
        Query query = collection.whereEqualTo("userID", userID);

        query.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                for (QueryDocumentSnapshot document : task.getResult()) {
                    onSuccess.accept(document.getData());
                }
            } else {
                onError.accept(task.getException());
            }
        });
        // can be run with:
        // PlayerCollection pc = new PlayerCollection(null);
        // pc.read(userID, data -> {
        //      // on success so code with data here
        //      System.out.println("Data for user1: " + data); },
        //      error -> {
        //      System.out.println("Error getting player data: " + error);});
        // can change system.out.println to assign to a variable
    }

    @Override
    public CompletableFuture<Void> update(String userID, Map<String, Object> newData) {
        // returns null on completion of update, exception otherwise
        CompletableFuture<Void> future = new CompletableFuture<>();
        collection.whereEqualTo("userID", userID)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        future.completeExceptionally(new Exception("No player found with username: " + userID));
                    } else {
                        DocumentReference documentReference = queryDocumentSnapshots.getDocuments().get(0).getReference();
                        newData.put("userID", userID); // Add the username to the HashMap
                        newData.remove("qr_code_ids");
                        documentReference.update(newData)
                                .addOnSuccessListener(aVoid -> future.complete(null))
                                .addOnFailureListener(e -> future.completeExceptionally(e));
                    }
                })
                .addOnFailureListener(e -> future.completeExceptionally(e));
        return future;
        // can be run with:
        // PlayerCollection pc = new PlayerCollection(null);
        // Map<String, Object> values;
        // values = createValues(...) // can be null
        // pc.update(userID, values, task -> {
        //    if (task.isSuccessful()) {
        //        System.out.println("Player data updated");
        //    } else {
        //        System.out.println("Error updating player data: " + task.getException());
        //    }
        //});
    }

    @Override
    public void delete(String userID) {
        // deletes the document in the player collection that has the given userID
        Query query = collection.whereEqualTo("userID", userID);

        query.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                for (QueryDocumentSnapshot document : task.getResult()) {
                    collection.document(document.getId()).delete();
                }
            } else {
                System.out.println("Error deleting player: " + task.getException());
            }
        });
        // can be run with:
        // PlayerCollection pc = new PlayerCollection();
        // pc.delete(userID);
    }

    // returns a map to be used for adding and updating
    public Map createValues(String userID, String username, String phoneNumber, String email, Boolean geolocation_setting, Integer totalScore, Integer totalQRCode) {
        // This represents the fields in the player collection
        // can add or remove fields here
        Map<String, Object> values = new HashMap<>();
        values.put("userID", userID);
        values.put("username", username);
        values.put("phoneNumber", phoneNumber);
        values.put("email", email);
        values.put("geolocation_setting", geolocation_setting);
        values.put("totalScore", totalScore);
        values.put("totalQRCode", totalQRCode);
        values.put("qr_code_ids", new ArrayList<String>());
        return values;
    }

    public CompletableFuture<Boolean> checkUserExists(String userID) {
        // returns true if a user exists, false otherwise
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        Query query = collection.whereEqualTo("userID", userID);
        query.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                QuerySnapshot querySnapshot = task.getResult();
                if (querySnapshot != null && !querySnapshot.isEmpty()) {
                    future.complete(true);
                } else {
                    future.complete(false);
                }
            } else {
                future.completeExceptionally(task.getException());
            }
        });
        return future;
    }

    public CompletableFuture<Boolean> checkUsernameUnique(String username) {
        // returns true if a user exists, false otherwise
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        Query query = collection.whereEqualTo("username", username);
        query.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                QuerySnapshot querySnapshot = task.getResult();
                if (querySnapshot != null && !querySnapshot.isEmpty()) {
                    future.complete(false);
                } else {
                    future.complete(true);
                }
            } else {
                future.completeExceptionally(task.getException());
            }
        });
        return future;
    }

    public CompletableFuture<String> generateUniqueUsername() {
        // generates a unique username to use a default for a new user
        // asynchronous call as it counts the number of players in the database
        CompletableFuture<String> future = new CompletableFuture<>();
        collection.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                QuerySnapshot snapshot = task.getResult();
                int numPlayers = snapshot.size();
                String uniqueUsername = "User" + (numPlayers + 1);
                future.complete(uniqueUsername);
            } else {
                future.completeExceptionally(task.getException());
            }
        });
        return future;
    }


    // Function to add a QR code ID to a player's document in the player collection
    public void add_QR_to_players(String userID, String QR_ID) {
        // Use a query to find the player document with the matching userID field
        Query query = collection.whereEqualTo("userID", userID);
        query.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                // There should be only one document with the given userID field
                for (DocumentSnapshot document : task.getResult()) {
                    // Update the qr_code_ids field of the player document
                    ArrayList<String> qrCodeIds = (ArrayList<String>) document.get("qr_code_ids");
                    qrCodeIds.add(QR_ID);
                    document.getReference().update("qr_code_ids", qrCodeIds);
                }
            } else {
                // Handle errors here
            }
        });
    }
    // run with:
//    PlayerCollection pc = new PlayerCollection(null);
//    UserState us = UserState.getInstance();
//    String ID = us.getUserID();
//    pc.add_QR_from_players(ID, "test");


    // Function to delete a QR code ID from a player's document in the player collection
    public void delete_QR_from_players(String userID, String QR_ID) {
        // Use a query to find the player document with the matching userID field
        Query query = collection.whereEqualTo("userID", userID);
        query.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                // There should be only one document with the given userID field
                for (DocumentSnapshot document : task.getResult()) {
                    // Update the qr_code_ids field of the player document
                    ArrayList<String> qrCodeIds = (ArrayList<String>) document.get("qr_code_ids");
                    qrCodeIds.remove(QR_ID);
                    document.getReference().update("qr_code_ids", qrCodeIds);
                }
            } else {
                // Handle errors here
            }
        });
    }

    public CompletableFuture<Integer> getPlayerRank(String userID) {
        CompletableFuture<Integer> futureRank = new CompletableFuture<>();

        // Get the player document with the given playerId
        collection.document(userID).get().addOnSuccessListener(documentSnapshot -> {
            // Get the player's totalScore
            Long playerScore = documentSnapshot.getLong("totalScore");
            if (playerScore == null) {
                playerScore = (long) 0;
            }
            System.out.println("totalScore" + playerScore);

            // Query for all players with a higher totalScore
            Query query = collection.whereGreaterThan("totalScore", playerScore);

            // Count the number of players with a higher totalScore to determine the rank
            query.get().addOnSuccessListener(queryDocumentSnapshots -> {
                int rank = queryDocumentSnapshots.size() + 1;
                futureRank.complete(rank);
            }).addOnFailureListener(e -> {
                futureRank.completeExceptionally(e);
            });
        }).addOnFailureListener(e -> {
            futureRank.completeExceptionally(e);
        });

        return futureRank;
    }

    public void searchUser(String username, Consumer<Map<String, Object>> onSuccess, Consumer<Exception> onError) {
        // returns the data for a user with the given userID
        Query query = collection.whereEqualTo("username", username);

        query.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                for (QueryDocumentSnapshot document : task.getResult()) {
                    onSuccess.accept(document.getData());
                }
            } else {
                onError.accept(task.getException());
            }
        });
    }


}