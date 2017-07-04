package com.example.android.inventory.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import com.example.android.inventory.data.InventoryContract.ProductEntry;

class InventoryDbHelper extends SQLiteOpenHelper {

    // Name of the database file
    private static final String DATABASE_NAME = "inventory.db";

    // Database version. If you change the database schema, you must increment the database version.
    private static final int DATABASE_VERSION = 1;

    // Inventory database helper constructor
    InventoryDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    /***
     * Called when the database is created for the first time.
     * @param db    The database
     */
    @Override
    public void onCreate(SQLiteDatabase db) {

        // Create a String that contains the SQL statement to create the products table
        String SQL_CREATE_PRODUCTS_TABLE  = "CREATE TABLE " + ProductEntry.TABLE_NAME + " (" +
                        ProductEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        ProductEntry.COLUMN_PRODUCT_NAME + " TEXT NOT NULL, " +
                        ProductEntry.COLUMN_PRODUCT_QUANTITY + " INTEGER NOT NULL DEFAULT 0, " +
                        ProductEntry.COLUMN_PRODUCT_PRICE + " FLOAT NOT NULL DEFAULT 0, " +
                        ProductEntry.COLUMN_PRODUCT_IMAGE + " TEXT);";

        // Execute the SQL statement
        db.execSQL(SQL_CREATE_PRODUCTS_TABLE);
    }

    /***
     * Called when the database needs to be upgraded.
     * @param db            The database
     * @param oldVersion    The old database version
     * @param newVersion    The new database version
     */
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }
}
