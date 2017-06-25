package admu.raintransmitter.main;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Prepares the date for sending to server
 */
//TODO: Fix buffers, i think they dont work well, right now, crashes on startup and then works
public class SQLiteBuffer {
    private static final String TAG = "SQLiteBuffer";
    // DATABASE ELEMENTS //
    public SQLiteDatabase db;
    private static final String TABLE_BUFFER = "buffer";
    private static final String COLid = "id";
    private static final String COLnumber = "number";
    private static final String COLmessage = "message";
    private static final String COLpriority = "priority";

    public SQLiteBuffer(String sdLink){
        // creating the database
        db = SQLiteDatabase.openOrCreateDatabase(sdLink, null);
        db.setVersion(1);
        db.setLocale(Locale.getDefault());
        createTable();
    }

    public void closeDatabase(){
        db.close();
    }

    public void createTable() {
        final String CREATE_TABLE =
                "CREATE TABLE IF NOT EXISTS " + TABLE_BUFFER + " ("
                + COLid + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLnumber + " TEXT,"
                + COLmessage + " TEXT,"
                + COLpriority + " TEXT)";
        db.execSQL(CREATE_TABLE);
    }

    public void truncateTable() {
        final String DELETE_TABLE = "DELETE FROM " + TABLE_BUFFER;
        db.execSQL(DELETE_TABLE);
        createTable();
    }

    public int getNumberRows(String priority){
        Cursor c = db.rawQuery("SELECT " + COLid + " FROM " + TABLE_BUFFER + " WHERE " + COLpriority + " = '" + priority + "'", null);
        return (c.getCount());
    }

    /**
     * Returns the first row of the rows of a certain priority. Used after getNumberRows()
     * @param priority
     * @return result[]: id, number, message, priority
     */
    public String[] getFirstRow(String priority){
        Cursor c = db.rawQuery("SELECT * FROM " + TABLE_BUFFER + " WHERE " + COLpriority + " = '" + priority + "' ORDER BY id ASC LIMIT 1", null);
        String[] result = new String[4];
        c.moveToFirst();
        result[0] = c.getString(0);
        result[1] = c.getString(1);
        result[2] = c.getString(2);
        result[3] = c.getString(3);
        c.close();
        return result;
    }

    //TODO: get list of 18 data points after checking if there are more than 18 of them
    public List<String[]> getXNumberOfDataPoints(int count){
        Cursor c = db.rawQuery("SELECT * FROM " + TABLE_BUFFER + " WHERE " + COLpriority + " = '" + 2 + "' ORDER BY id ASC LIMIT " + count, null);

        List<String[]> dataPoints = new ArrayList<>();
        while(c.moveToNext()) {
            String[] result = new String[4];
            result[0] = c.getString(0); //id
            result[1] = c.getString(1); //number
            result[2] = c.getString(2); //message
            result[3] = c.getString(3); //priority
            dataPoints.add(result);
        }
        c.close();

        return dataPoints;
    }

    public void insertRow(String number, String message, String priority){
        ContentValues values = new ContentValues();
        Log.d(TAG, "Insert: " + number + "," + message + "," + priority);
        values.put(COLnumber, number);
        values.put(COLmessage, message);
        values.put(COLpriority, priority);
        db.insert(TABLE_BUFFER, null, values);
    }

    public void deleteRow(int id) {
        final String DELETE_ROW =
                "DELETE FROM " + TABLE_BUFFER + " WHERE " + COLid + " = " + id;
        db.execSQL(DELETE_ROW);
    }

}