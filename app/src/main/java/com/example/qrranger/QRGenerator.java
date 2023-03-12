package com.example.qrranger;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class QRGenerator {
    private QRCode qr;
    private QRCollection qrCollection;
    private PlayerCollection playerCollection;
    private String hash;
    private gemID gemRepresentation;

    /**
     * Checks if the QR exists in the database:
     * - If it does, pull it and populate the QR.
     * - If it does not, create a new QR and add it to the database.
     *
     * @param qrData
     *
     */
    public void generateQR(String qrData) {
        qrCollection = new QRCollection(null);

        // Generate a new QR if it doesn't already exist or pull the existing one from the db.
        CompletableFuture<Boolean> future = qrCollection.checkQRExists(qrData);
        future.thenAccept(qrExists -> {
            if (qrExists) {
                // Pull existing QR from the DB.
                qrCollection.read(qrData, data -> {
                    final Map values = data;
                }, error -> {

                });
            }
            else {
                // QR doesn't exist, so generate a new one!
                gemRepresentation = new gemID();

                qr = new QRCode(qrData, qrData);

                hash = SHA256Hash(qrData);
                Integer score = calculateScore(hash);
                qr.setPoints(score);
            }
        });


    }

    public void addQRToAccount(String qrData) {


        playerCollection = new PlayerCollection(null);
        UserState state = UserState.getInstance();
        String userID = state.getUserID();
        playerCollection.add_QR_to_players(userID, qrData);
    }

    private static String SHA256Hash(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hashInBytes = md.digest(input.getBytes());

            StringBuilder sb = new StringBuilder();
            for (byte b : hashInBytes) {
                sb.append(String.format("%02x", b));
            }
            //Returns the String representation of the hash
            //** Make sure to add "\n" to the end of the returned string to make give the desired hash**
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    private static Integer calculateScore(String hash) {
        // Calculate score
        /* From: geeksforgeeks.org
         * At: https://www.geeksforgeeks.org/java-program-for-hexadecimal-to-decimal-conversion/
         * Author: mayur_patil https://auth.geeksforgeeks.org/user/mayur_patil/articles
         */
        char[] hashChars = hash.toCharArray();
        char prevChar = hashChars[0];
        int duplicates = 0, totalScore = 0, base = 0;
        for (int i = 1; i < hash.length(); i++) {
            if (hashChars[i] == prevChar) {
                duplicates += 1;
            }
            else if (duplicates != 0) {
                if (prevChar == '0') {
                    base = 20;
                }
                else if (prevChar >= '1' && prevChar <= '9') { // 1-9
                    base = prevChar - 48; // 2-9
                }
                else if (prevChar >= 'a' && prevChar <= 'g') { // a-g
                    base = prevChar - 87; // 10-16
                }
                totalScore += Math.pow(base, duplicates); // base^duplicates
                duplicates = 0;
            }
            prevChar = hashChars[i];
        }

        return totalScore;
    }
}
