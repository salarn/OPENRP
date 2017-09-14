package com.example.salar.openrp;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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

    // Own experience data table name
    private static final String TABLE_OWN_DATA_NAME = "ownExperienceData";

    // Other experience data table name
    private static final String TABLE_OTHER_DATA_NAME = "otherExperienceData";

    // Last Recommendation time table name
    private static final String TABLE_LAST_RECOM_NAME = "lastRecommendationTime";

    // Contacts Table Columns names
    public static final String KEY_COUNTER = "counter";
    public static final String KEY_PEER_ID = "peer_id";
    public static final String KEY_START_TIME = "start_time";
    public static final String KEY_FINISH_TIME = "finish_time";
    public static final String KEY_VALUE = "value";
    public static final String KEY_FROM_PEER_ID = "from_peer_id";
    public static final String KEY_LAST_RECOM_TIME = "last_recom_time";

    public DatabaseHandler(Context context){
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_OWN_DATA_TABLE = "CREATE TABLE " + TABLE_OWN_DATA_NAME + "("
                + KEY_COUNTER + " INTEGER PRIMARY KEY," + KEY_PEER_ID + " TEXT,"
                + KEY_START_TIME + " BIGINT,"+ KEY_FINISH_TIME + " BIGINT," + KEY_VALUE + " FLOAT"+ ")";

        String CREATE_OTHER_DATA_TABLE = "CREATE TABLE " + TABLE_OTHER_DATA_NAME + "("
                + KEY_COUNTER + " INTEGER PRIMARY KEY," + KEY_FROM_PEER_ID + " TEXT," +KEY_PEER_ID + " TEXT,"
                + KEY_START_TIME + " BIGINT,"+ KEY_FINISH_TIME + " BIGINT," + KEY_VALUE + " FLOAT"+ ")";

        String CREATE_LAST_RECOM_TIME_TABLE = "CREATE TABLE " + TABLE_LAST_RECOM_NAME + "("
                + KEY_COUNTER + " INTEGER PRIMARY KEY," + KEY_PEER_ID + " TEXT,"
                + KEY_LAST_RECOM_TIME + " BIGINT" + ")";

        db.execSQL(CREATE_OWN_DATA_TABLE);
        db.execSQL(CREATE_OTHER_DATA_TABLE);
        db.execSQL(CREATE_LAST_RECOM_TIME_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Drop older table if existed
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_OWN_DATA_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_OTHER_DATA_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_LAST_RECOM_NAME);

        // Create tables again
        onCreate(db);
    }

    // Adding new CacheRequests
    public void addCacheRequest(CacheRequest rq) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();

        values.put(KEY_PEER_ID, rq.getPeer_id()); // CacheRequest Peer ID
        values.put(KEY_START_TIME, rq.getStartTime()); // CacheRequest Start Time
        values.put(KEY_FINISH_TIME, rq.getFinishTime()); // CacheRequest Finish Time
        values.put(KEY_VALUE, rq.getValue()); // CacheRequest Value

        // Inserting Row
        db.insert(TABLE_OWN_DATA_NAME, null, values);
        db.close(); // Closing database connection
    }

    // Getting single CacheRequest
    public CacheRequest getCacheRequestWithCounter(int counter) {
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(TABLE_OWN_DATA_NAME, new String[] { KEY_COUNTER,
                        KEY_PEER_ID, KEY_START_TIME, KEY_FINISH_TIME, KEY_VALUE }, KEY_COUNTER + "=?",
                new String[] { String.valueOf(counter) }, null, null, null, null);
        if (cursor != null)
            cursor.moveToFirst();

        CacheRequest cacheRequest = new CacheRequest(cursor.getString(1),
                Long.valueOf(cursor.getString(2)), Long.valueOf(cursor.getString(3)), Float.parseFloat(cursor.getString(4)));
        cursor.close();
        // return cacheRequest
        return cacheRequest;
    }

    public CacheRequest getCacheRequestWithStartTime(long startTime){
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(TABLE_OWN_DATA_NAME, new String[] { KEY_COUNTER,
                        KEY_PEER_ID, KEY_START_TIME, KEY_FINISH_TIME, KEY_VALUE }, KEY_START_TIME + "=?",
                new String[] { String.valueOf(startTime) }, null, null, null, null);
        if (cursor.moveToFirst()) {
            CacheRequest cacheRequest = new CacheRequest(cursor.getString(1),
                    Long.valueOf(cursor.getString(2)), Long.valueOf(cursor.getString(3)), Float.parseFloat(cursor.getString(4)));
            cursor.close();
            return cacheRequest;
        }
        else {
            cursor.close();
            return null;
        }
    }

    // Getting All CacheRequests
    public List<CacheRequest> getAllCacheRequests() {
        List<CacheRequest> cacheRequestList = new ArrayList<CacheRequest>();
        // Select All Query
        String selectQuery = "SELECT  * FROM " + TABLE_OWN_DATA_NAME;

        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        // looping through all rows and adding to list
        if (cursor.moveToFirst()) {
            do {
                CacheRequest cacheRequest = new CacheRequest();
                cacheRequest.setPeer_id(cursor.getString(1));
                cacheRequest.setStartTime(Long.valueOf(cursor.getString(2)));
                cacheRequest.setFinishTime(Long.valueOf(cursor.getString(3)));
                cacheRequest.setValue(Float.parseFloat(cursor.getString(4)));
                // Adding cacheRequest to list
                cacheRequestList.add(cacheRequest);
            } while (cursor.moveToNext());
        }
        cursor.close();
        // return contact list
        return cacheRequestList;
    }

    // Getting Reputation of device with own CacheRequests
    // With formula 1000*Zigma(1/(timeNow-timeEntry)*value)
    public float getOwnReputation(String peerId, long timeNow) {

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_OWN_DATA_NAME, new String[] { KEY_COUNTER,
                        KEY_PEER_ID, KEY_START_TIME, KEY_FINISH_TIME, KEY_VALUE }, KEY_PEER_ID + "=?",
                new String[] { String.valueOf(peerId) }, null, null, null, null);
        float zigma = (float) 0.0;
        // looping through all founded rows
        if (cursor.moveToFirst()) {
            do {
                long timeEntry = Long.valueOf(cursor.getString(3));
                float value = Float.parseFloat(cursor.getString(4));
                zigma += value/((float)(timeNow-timeEntry));
            } while (cursor.moveToNext());
        }
        cursor.close();
        // return zigma*1000
        zigma *= (float)1000.0;
        return zigma;
    }


    // Getting Reputation of device with recommendation CacheRequests
    // With formula 1000*Zigma(1/(timeNow-timeEntry)*value)
    public float getDeviceRecommendReputation(String peerId,long timeNow){
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_OTHER_DATA_NAME, new String[] { KEY_COUNTER,
                        KEY_FROM_PEER_ID, KEY_PEER_ID, KEY_START_TIME, KEY_FINISH_TIME, KEY_VALUE }, KEY_PEER_ID + "=?",
                new String[] { String.valueOf(peerId) }, null, null, null, null);
        float zigma = (float) 0.0;
        // looping through all founded rows
        if (cursor.moveToFirst()) {
            do {
                long timeEntry = Long.valueOf(cursor.getString(4));
                float value = Float.parseFloat(cursor.getString(5));
                zigma += value/((float)(timeNow-timeEntry));
            } while (cursor.moveToNext());
        }
        cursor.close();
        // return zigma*1000
        zigma *= (float)1000.0;
        return zigma;
    }

    // Getting average time interaction of specific device
    public float getAverageTime(String peerId) {

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_OWN_DATA_NAME, new String[] { KEY_COUNTER,
                        KEY_PEER_ID, KEY_START_TIME, KEY_FINISH_TIME, KEY_VALUE }, KEY_PEER_ID + "=?",
                new String[] { String.valueOf(peerId) }, null, null, null, null);
        long zigma = (long) 0;
        // looping through all founded rows
        if (cursor.moveToFirst()) {
            do {
                long startTime = Long.valueOf(cursor.getString(2));
                long finishTime = Long.valueOf(cursor.getString(3));
                zigma += finishTime-startTime;
            } while (cursor.moveToNext());
        }
        // return zigma/cursorSize
        float ans = (float)0.0;
        if(cursor.getCount() != 0)
            ans = (float)(zigma)/(float)(cursor.getCount());
        cursor.close();
        return ans;
    }

    // Getting number of cacheRequests of specific device
    public int getNumberOfInteractions(String peerId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_OWN_DATA_NAME, new String[] { KEY_COUNTER,
                        KEY_PEER_ID, KEY_START_TIME, KEY_FINISH_TIME, KEY_VALUE }, KEY_PEER_ID + "=?",
                new String[] { String.valueOf(peerId) }, null, null, null, null);
        int answer = cursor.getCount();
        cursor.close();

        // return count
        return answer;
    }

    // Getting last time interaction with specific device
    public long getLastTimeInteraction(String peerId){
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_OWN_DATA_NAME, new String[] { KEY_COUNTER,
                        KEY_PEER_ID, KEY_START_TIME, KEY_FINISH_TIME, KEY_VALUE }, KEY_PEER_ID + "=?",
                new String[] { String.valueOf(peerId) }, null, null, null, null);
        long lastTime = (long) 0;
        // looping through all founded rows
        if (cursor.moveToFirst()) {
            do {
                long finishTime = Long.valueOf(cursor.getString(3));
                if(lastTime < finishTime)
                    lastTime = finishTime;
            } while (cursor.moveToNext());
        }
        cursor.close();
        return lastTime;
    }

    // Getting CacheRequests Count
    public int getCacheRequestsCount() {
        String countQuery = "SELECT  * FROM " + TABLE_OWN_DATA_NAME;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(countQuery, null);
        int answer = cursor.getCount();
        cursor.close();

        // return count
        return answer;
    }

    public String convertDataTableToJson(long fromTime){
        JSONArray jsonArray  = new JSONArray();

        SQLiteDatabase db = this.getWritableDatabase();

        Cursor cursor = db.query(TABLE_OWN_DATA_NAME, new String[] { KEY_COUNTER,
                        KEY_PEER_ID, KEY_START_TIME, KEY_FINISH_TIME, KEY_VALUE }, KEY_FINISH_TIME + ">=?",
                new String[] { String.valueOf(fromTime) }, null, null, null, null);

        // looping through all rows and adding to list
        if (cursor.moveToFirst()) {
            do {
                JSONObject jsonObject = new JSONObject();
                try {
                    jsonObject.put(KEY_PEER_ID,cursor.getString(1));
                    jsonObject.put(KEY_START_TIME,cursor.getString(2));
                    jsonObject.put(KEY_FINISH_TIME,cursor.getString(3));
                    jsonObject.put(KEY_VALUE,cursor.getString(4));

                } catch (JSONException e) {
                    e.printStackTrace();
                }
                // Adding jsonObject to JsonArray
                jsonArray.put(jsonObject);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return jsonArray.toString();
    }

    /////////////

    // Adding new CacheRecommendation
    public void addCacheRecommendation(CacheRecommendation rq) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();

        values.put(KEY_FROM_PEER_ID, rq.getFrom_peer_id()); //CacheRecommendation From peer id
        values.put(KEY_PEER_ID, rq.getPeer_id()); // CacheRecommendation Peer ID
        values.put(KEY_START_TIME, rq.getStartTime()); // CacheRecommendation Start Time
        values.put(KEY_FINISH_TIME, rq.getFinishTime()); // CacheRecommendation Finish Time
        values.put(KEY_VALUE, rq.getValue()); // CacheRecommendation Value

        // Inserting Row
        db.insert(TABLE_OTHER_DATA_NAME, null, values);
        db.close(); // Closing database connection
    }

    public void deleteCacheRecommendation(String id){
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_OTHER_DATA_NAME, KEY_FROM_PEER_ID + " = ?",
                new String[] { id });
        db.close();
    }

    // Getting All CacheRecommendation
    public List<CacheRecommendation> getAllCacheRecommendation() {
        List<CacheRecommendation> cacheRecommendationList = new ArrayList<CacheRecommendation>();
        // Select All Query
        String selectQuery = "SELECT  * FROM " + TABLE_OTHER_DATA_NAME;

        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        // looping through all rows and adding to list
        if (cursor.moveToFirst()) {
            do {
                CacheRecommendation cacheRecommendation = new CacheRecommendation();
                cacheRecommendation.setFrom_peer_id(cursor.getString(1));
                cacheRecommendation.setPeer_id(cursor.getString(2));
                cacheRecommendation.setStartTime(Long.valueOf(cursor.getString(3)));
                cacheRecommendation.setFinishTime(Long.valueOf(cursor.getString(4)));
                cacheRecommendation.setValue(Float.parseFloat(cursor.getString(5)));
                // Adding cacheRecommendation to list
                cacheRecommendationList.add(cacheRecommendation);
            } while (cursor.moveToNext());
        }
        cursor.close();
        // return CacheRecommendation list
        return cacheRecommendationList;
    }

    //////////////

    // Adding new RecommendationTime
    public void addRecommendationTime(RecommendationTime rt){
        SQLiteDatabase db = this.getWritableDatabase();

        db.delete(TABLE_LAST_RECOM_NAME, KEY_PEER_ID + " = ?",
                new String[] { rt.getPeer_id() });

        ContentValues values = new ContentValues();

        values.put(KEY_PEER_ID, rt.getPeer_id()); // RecommendationTime Peer ID
        values.put(KEY_LAST_RECOM_TIME, rt.getLastRecomTime()); // RecommendationTime Last Recommendation Time

        // Inserting Row
        db.insert(TABLE_LAST_RECOM_NAME, null, values);
        db.close(); // Closing database connection
    }

    // Getting Last Recommendation time

    public RecommendationTime getLastRecommendationTime(String id){
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(TABLE_LAST_RECOM_NAME, new String[] { KEY_COUNTER,
                        KEY_PEER_ID, KEY_LAST_RECOM_TIME }, KEY_PEER_ID + "=?",
                new String[] { String.valueOf(id) }, null, null, null, null);
        if (cursor.moveToFirst()) {
            RecommendationTime recommendationTime = new RecommendationTime(cursor.getString(1),
                    Long.valueOf(cursor.getString(2)) );
            cursor.close();
            return recommendationTime;
        }
        else {
            RecommendationTime recommendationTime = new RecommendationTime(id , 0);
            cursor.close();
            return recommendationTime;
        }
    }

    // Getting All RecommendationTimes
    public List<RecommendationTime> getAllRecommendationTimes() {
        List<RecommendationTime> recommendationTimeList = new ArrayList<RecommendationTime>();
        // Select All Query
        String selectQuery = "SELECT  * FROM " + TABLE_LAST_RECOM_NAME;

        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        // looping through all rows and adding to list
        if (cursor.moveToFirst()) {
            do {
                RecommendationTime recommendationTime = new RecommendationTime(cursor.getString(1),
                        Long.valueOf(cursor.getString(2)));
                // Adding RecommendationTime to list
                recommendationTimeList.add(recommendationTime);
            } while (cursor.moveToNext());
        }
        cursor.close();
        // return RecommendationTime list
        return recommendationTimeList;
    }

}
