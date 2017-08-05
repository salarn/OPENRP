package com.example.salar.openrp;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by salar on 8/4/17.
 */

public class DatabaseHandler extends SQLiteOpenHelper {

    // Database Version
    private static final int DATABASE_VERSION = 1;

    // Database Name
    private static final String DATABASE_NAME = "OPENRPDatabase";

    // Contacts table name
    private static final String TABLE_CONTACTS = "communicationData";

    // Contacts Table Columns names
    private static final String KEY_COUNTER = "counter";
    private static final String KEY_PEER_ID = "peer_id";
    private static final String KEY_TIME = "time";
    private static final String KEY_VALUE = "value";

    public DatabaseHandler(Context context){
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_CONTACTS_TABLE = "CREATE TABLE " + TABLE_CONTACTS + "("
                + KEY_COUNTER + " INTEGER PRIMARY KEY," + KEY_PEER_ID + " INTEGER,"
                + KEY_TIME + " TIMESTAMP," + KEY_VALUE + " FLOAT"+ ")";
        db.execSQL(CREATE_CONTACTS_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Drop older table if existed
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_CONTACTS);

        // Create tables again
        onCreate(db);
    }
    // Adding new CacheRequests
    public void addCacheRequest(CacheRequest rq) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        //values.put(KEY_COUNTER, 1);
        values.put(KEY_PEER_ID, rq.getPeer_id()); // CacheRequst Peer ID
        values.put(KEY_TIME, rq.getTime().toString()); // CacheRequst Time
        values.put(KEY_VALUE, rq.getValue()); // CacheRequst Value

        // Inserting Row
        db.insert(TABLE_CONTACTS, null, values);
        db.close(); // Closing database connection
    }

    // Getting single CacheRequest
    public CacheRequest getCacheRequest(int id) {
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(TABLE_CONTACTS, new String[] { KEY_COUNTER,
                        KEY_PEER_ID, KEY_TIME, KEY_VALUE }, KEY_COUNTER + "=?",
                new String[] { String.valueOf(id) }, null, null, null, null);
        if (cursor != null)
            cursor.moveToFirst();

        CacheRequest cacheRequest = new CacheRequest(Integer.parseInt(cursor.getString(1)),
                Timestamp.valueOf(cursor.getString(2)), Float.parseFloat(cursor.getString(3)));
        // return cacheRequest
        return cacheRequest;
    }

    // Getting All CacheRequests
    public List<CacheRequest> getAllCacheRequests() {
        List<CacheRequest> cacheRequestList = new ArrayList<CacheRequest>();
        // Select All Query
        String selectQuery = "SELECT  * FROM " + TABLE_CONTACTS;

        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        // looping through all rows and adding to list
        if (cursor.moveToFirst()) {
            do {
                CacheRequest cacheRequest = new CacheRequest();
                cacheRequest.setPeer_id(Integer.parseInt(cursor.getString(1)));
                cacheRequest.setTime(Timestamp.valueOf(cursor.getString(2)));
                cacheRequest.setValue(Float.parseFloat(cursor.getString(3)));
                // Adding cacheRequest to list
                cacheRequestList.add(cacheRequest);
            } while (cursor.moveToNext());
        }

        // return contact list
        return cacheRequestList;
    }

    // Getting CacheRequests Count
    public int getCacheRequestsCount() {
        String countQuery = "SELECT  * FROM " + TABLE_CONTACTS;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(countQuery, null);
        cursor.close();

        // return count
        return cursor.getCount();
    }

}
