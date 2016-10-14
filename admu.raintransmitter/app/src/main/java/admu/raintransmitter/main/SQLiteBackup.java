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
    private static final String COLsnd2 = "snd2";
    private static final String COLsnd3 = "snd3";
    private static final String COLsnd4 = "snd4";
    private static final String COLsnd5 = "snd5";
    private static final String COLsnd6 = "snd6";
    private static final String COLsig1 = "sig1";
    private static final String COLsig2 = "sig2";
    private static final String COLsig3 = "sig3";
    private static final String COLsig4 = "sig4";
    private static final String COLsig5 = "sig5";
    private static final String COLsig6 = "sig6";
    private static final String COLdis1 = "dis1";
    private static final String COLdis2 = "dis2";
    private static final String COLdis3 = "dis3";
    private static final String COLdis4 = "dis4";
    private static final String COLdis5 = "dis5";
    private static final String COLdis6 = "dis6";
    private static final String COLdis7 = "dis7";
    private static final String COLdis8 = "dis8";
    private static final String COLdis9 = "dis9";
    private static final String COLdis10 = "dis10";

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
                + COLsnd1 + " TEXT,"
                + COLsnd2 + " TEXT,"
                + COLsnd3 + " TEXT,"
                + COLsnd4 + " TEXT,"
                + COLsnd5 + " TEXT,"
                + COLsnd6 + " TEXT,"
                + COLsig1 + " TEXT,"
                + COLsig2 + " TEXT,"
                + COLsig3 + " TEXT,"
                + COLsig4 + " TEXT,"
                + COLsig5 + " TEXT,"
                + COLsig6 + " TEXT,"
                + COLdis1 + " TEXT,"
                + COLdis2 + " TEXT,"
                + COLdis3 + " TEXT,"
                + COLdis4 + " TEXT,"
                + COLdis5 + " TEXT,"
                + COLdis6 + " TEXT,"
                + COLdis7 + " TEXT,"
                + COLdis8 + " TEXT,"
                + COLdis9 + " TEXT,"
                + COLdis10 + " TEXT)";
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

        String[] snd = text[2].split(",");
        for (int i = 0; i < 6; i++)  values.put("snd" + String.valueOf(i+1), snd[i]);

        String[] sig = text[3].split(",");
        for (int i = 0; i < 6; i++)  values.put("sig" + String.valueOf(i+1), sig[i]);

        String[] dis = text[4].split(",");
        for (int i = 0; i < 10; i++) values.put("dis" + String.valueOf(i+1), dis[i]);

        db.insert(TABLE_BACKUP, null, values);
    }

}