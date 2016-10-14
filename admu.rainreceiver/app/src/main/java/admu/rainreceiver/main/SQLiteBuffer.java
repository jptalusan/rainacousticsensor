package admu.rainreceiver.main;

import java.util.Locale;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public class SQLiteBuffer {
	
	// DATABASE ELEMENTS //
	private SQLiteDatabase db;
	private static final String TABLE_BUFFER = "buffer";
	private static final String COLid = "id";
	private static final String COLsensor = "sensor";
	private static final String COLserver = "server";
	private static final String COLmessage = "message";
		
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
	    	    + COLsensor + " INTEGER,"
	    	    + COLserver + " INTEGER,"
	        	+ COLmessage + " TEXT)";
		db.execSQL(CREATE_TABLE);
	}
	
	public void truncateTable() {
		final String DELETE_TABLE = "DELETE FROM " + TABLE_BUFFER;
		db.execSQL(DELETE_TABLE);
		createTable();
	}
	
	public int getNumberRows(){
		Cursor c = db.rawQuery("SELECT " + COLid + " FROM " + TABLE_BUFFER, null);
		return (c.getCount());
	}
	
	public String[] getFirstRow(){
		Cursor c = db.rawQuery("SELECT * FROM " + TABLE_BUFFER + " ORDER BY id ASC LIMIT 1", null);
		String[] result = new String[4];
		c.moveToFirst();
		result[0] = c.getString(0);
		result[1] = c.getString(1);
		result[2] = c.getString(2);
		result[3] = c.getString(3);
		c.close();
		return result;
	}
	
	public void insertRow(int sensor, int server, String message){
		ContentValues values = new ContentValues();
		values.put(COLsensor, sensor);
		values.put(COLserver, server);
		values.put(COLmessage, message);
		db.insert(TABLE_BUFFER, null, values);
	}
	
	public void deleteRow(int id) {
		final String DELETE_ROW = 
				"DELETE FROM " + TABLE_BUFFER + " WHERE " + COLid + " = " + id;
		db.execSQL(DELETE_ROW); 
	}

}