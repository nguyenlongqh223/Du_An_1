package com.poly.ban_giay_app;

import android.content.Context;
import android.content.SharedPreferences;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.poly.ban_giay_app.models.TransactionHistory;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TransactionHistoryManager {
    private static final String PREFS_NAME = "transaction_history";
    private static final String KEY_TRANSACTIONS = "key_transactions";
    private static TransactionHistoryManager instance;
    private SharedPreferences sharedPreferences;
    private Gson gson;

    private TransactionHistoryManager(Context context) {
        this.sharedPreferences = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.gson = new Gson();
    }

    public static synchronized TransactionHistoryManager getInstance(Context context) {
        if (instance == null) {
            instance = new TransactionHistoryManager(context);
        }
        return instance;
    }

    public void addTransaction(TransactionHistory transaction) {
        List<TransactionHistory> transactions = getAllTransactions();
        transactions.add(0, transaction); // Add to beginning
        saveTransactions(transactions);
    }

    public List<TransactionHistory> getAllTransactions() {
        String json = sharedPreferences.getString(KEY_TRANSACTIONS, null);
        if (json == null) {
            return new ArrayList<>();
        }
        Type type = new TypeToken<List<TransactionHistory>>(){}.getType();
        List<TransactionHistory> transactions = gson.fromJson(json, type);
        if (transactions == null) {
            return new ArrayList<>();
        }
        // Sort by date descending (newest first)
        Collections.sort(transactions, (t1, t2) -> t2.paymentDate.compareTo(t1.paymentDate));
        return transactions;
    }

    public List<TransactionHistory> getTransactionsByUser(String userId) {
        List<TransactionHistory> allTransactions = getAllTransactions();
        List<TransactionHistory> userTransactions = new ArrayList<>();
        for (TransactionHistory transaction : allTransactions) {
            if (transaction.userName != null && transaction.userName.equals(userId)) {
                userTransactions.add(transaction);
            }
        }
        return userTransactions;
    }

    private void saveTransactions(List<TransactionHistory> transactions) {
        String json = gson.toJson(transactions);
        sharedPreferences.edit().putString(KEY_TRANSACTIONS, json).apply();
    }

    public void clearAll() {
        sharedPreferences.edit().remove(KEY_TRANSACTIONS).apply();
    }
}

