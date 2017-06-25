package admu.raintransmitter.main;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;

import java.util.Locale;

/**
 * Backs up the received data on device
 */
public class SQLiteBackup {

    // DATABASE ELEMENTS //
    private SQLiteDatabase db;
    private static final String TABLE_BACKUP = "backup";
    private static final String COLid_data = "id";
    private static final String COLdate_time = "date_time";
    private static final String COLsnd1 = "snd1";

    public SQLiteBackup(String sdLink){
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
                "CREATE TABLE IF NOT EXISTS " + TABLE_BACKUP + " ("
                + COLid_data + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLdate_time + " TEXT,"
                + COLsnd1 + " TEXT)";
        db.execSQL(CREATE_TABLE);
    }

    public void truncateTable() {
        final String DELETE_TABLE = "DELETE FROM " + TABLE_BACKUP;
        db.execSQL(DELETE_TABLE);
        createTable();
    }

    public void insertRow(String row){
        String[] text = row.split(";");
        ContentValues values = new ContentValues();
        values.put(COLdate_time, text[1]);
        values.put(COLsnd1, text[2]);
        db.insert(TABLE_BACKUP, null, values);
    }

}