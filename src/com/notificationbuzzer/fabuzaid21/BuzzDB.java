package com.notificationbuzzer.fabuzaid21;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;


public class BuzzDB {
	
	// a bunch of static strings for table names, column names, and column
	// indexes.
	public static final int DATABASE_VERSION = 1;
	public static final String DATABASE_NAME = "buzzdb";
	public static final String DATABASE_APP_TABLE = "apptable";
	
	public static final String APP_NAME = "APP";
	public static final String VIBRATION = "VIBRATION";
		
	public static final String KEY_ROW_ID = "_id";// Android requires exactly
	// this key name
	public static final int INDEX_ROW_ID = 0;
	
	// Field names -- use the KEY_XXX constants here and in
	// client code, so it's all consistent and checked at compile-time.

	public static final String APP_KEY_NAME = "name";
	public static final int APP_INDEX_NAME = 1;
	public static final String APP_KEY_VIBRATION = "active";
	public static final int APP_INDEX_VIBRATION = 2;
		
	public static final String[] APP_KEYS_ALL = {APP_KEY_NAME, APP_KEY_VIBRATION};
	
	private Context context;
	protected SQLiteDatabase database;// is this secure?
	private BuzzDBHelper helper;
	
	/** Construct DB for this activity context. */
	public BuzzDB(Context cont) {
		context = cont;
	}
	
	/** Opens up a connection to the database. Do this before any operations. */
	public void open() throws SQLException {
		helper = new BuzzDBHelper(context);
		database = helper.getWritableDatabase();

	}
	
	/** Closes the database connection. Operations are not valid after this. */
	public void close() {
		helper.close();
		helper = null;
		database = null;
	}
	
	/**
	 * Creates and inserts a new row using the given values. Returns the rowid
	 * of the new row, or -1 on error. todo: values should not include a rowid I
	 * assume.
	 */
	public long createRow(String tableName, ContentValues values) {
		return database.insert(tableName, null, values);

	}
	
	/**
	 * Updates the given rowid with the given values. Returns true if there was
	 * a change (i.e. the rowid was valid).
	 */
	public boolean updateRow(String tableName, long rowId, ContentValues values) {
		return database.update(tableName, values, KEY_ROW_ID + "="
				+ rowId, null) > 0;
	}
	
	public boolean updateRow(String tableName, String whereClause, ContentValues values)
	{
		return database.update(tableName, values, whereClause, null)>0;
	}
	
	/**
	 * Deletes the given rowid. Returns true if any rows were deleted (i.e. the
	 * id was valid). tableName is the complete name
	 */
	public boolean deleteRow(String tableName, long rowId) {
		return database.delete(tableName, KEY_ROW_ID + "=" + rowId,
				null) > 0;
	}
	
	public boolean deleteRow(String tableName, String whereClause)
	{
		return database.delete(tableName, whereClause, null)>0;
	}
	
	/**
	 * Returns a cursor for all the rows. Caller should close or manage the
	 * cursor. tableName is the full proper name of the table. No modification
	 * necessary.
	 */
	public Cursor queryAll(String tableName) {

		String[] keysList;
		String orderMe;

		//To use if/when we add another table
		/*if (tableName == DATABASE_APP_TABLE) {
			keysList = APP_KEYS_ALL;
			orderMe = APP_KEY_NAME + " ASC";
		}*/
		
		keysList = APP_KEYS_ALL;
		orderMe = APP_KEY_NAME + " ASC";
		
		return database.query(tableName, keysList, // i.e. return all 4 columns
				null, null, null, null, orderMe // order-by, "DESC" for
				// descending
		);
	
	}
	
	/**
	 * Returns a cursor for the given row id. Caller should close or manage the
	 * cursor.
	 */
	public Cursor query(String tableName, long rowId) throws SQLException {

		String[] keysList;
		
		keysList=APP_KEYS_ALL;
		
		//to be used if/when we add a new table
		/*if (tableName == DATABASE_APP_TABLE) 
		keysList = APP_KEYS_ALL;*/
		
		Cursor cursor = database.query(true, tableName, keysList, KEY_ROW_ID
				+ "=" + rowId, // select the one row we care about
				null, null, null, null, null);

		// cursor starts before first -- move it to the row itself.
		cursor.moveToFirst();
		return cursor;
	}
	
	// tableName is the proper table name. No manipulation necessary.
	public Cursor query(String tableName, String[] columns, String whereClause,
			String keyToOrder, boolean orderAscending) {
		String orderBy;
		if (orderAscending)
			orderBy = keyToOrder + " ASC";
		else
			orderBy = keyToOrder + " DESC";

		return database.query(tableName, columns, whereClause, null, null,
				null, orderBy);

	}
	
	public Cursor query(String tableName, String[] columns, String whereClause,
			String keyToOrder, boolean orderAscending, String groupBy) {
		String orderBy;
		if (orderAscending)
			orderBy = keyToOrder + " ASC";
		else
			orderBy = keyToOrder + " DESC";

		return database.query(tableName, columns, whereClause, null, groupBy,
				null, orderBy);

	}
	
	// tableName is the proper table name. No manipulation necessary. Double
	// using this method name
	public Cursor query(String tableName, String[] columns, String whereClause) {
		return database.query(tableName, columns, whereClause, null, null,
				null, null);
	}
	
	private static class BuzzDBHelper extends SQLiteOpenHelper {
		// SQL text to create table (basically just string or integer)
		private static final String DATABASE_CREATE_APP = "create table "
			+ DATABASE_APP_TABLE + " (" + KEY_ROW_ID
			+ " integer primary key autoincrement, "
			+ APP_KEY_NAME + " text not null unique, "
			+ APP_KEY_VIBRATION+ " text not null unique "
			+");";
		
		public BuzzDBHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);

		}
		
		/** Creates the initial (empty) database. */
		@Override
		public void onCreate(SQLiteDatabase database) {
			database.execSQL(DATABASE_CREATE_APP);
		}
		/**
		 * Called at version upgrade time, in case we want to change/migrate the
		 * database structure. Here we just do nothing.
		 */
		
		@Override
		public void onUpgrade(SQLiteDatabase database, int oldVersion,
				int newVersion) {
			// we do nothing for this case
		}
	}


}
