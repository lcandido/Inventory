package com.example.android.inventory.data;

import android.content.ContentResolver;
import android.net.Uri;
import android.provider.BaseColumns;

/***
 * The contract for Inventory app. Contains definition for the supported URIs and columns
 */
public final class InventoryContract {

    // The authority for the inventory provider
    public static final String AUTHORITY = "com.example.android.inventory";

    // A content:// style uri to the authority for the inventory provider
    public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + AUTHORITY);

    // The path which contains products data
    public static final String PATH_PRODUCTS = "products";

    public static final class ProductEntry implements BaseColumns {

        // A content:// style uri to the products for the inventory provider
        public static final Uri CONTENT_URI = Uri.withAppendedPath(BASE_CONTENT_URI, PATH_PRODUCTS);

        public static final String CONTENT_LIST_TYPE =
                ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + AUTHORITY + "/" + PATH_PRODUCTS;

        public static final String CONTENT_ITEM_TYPE =
                ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + AUTHORITY + "/" + PATH_PRODUCTS;

        public static final String TABLE_NAME = "products";

        public static final String _ID = BaseColumns._ID;
        public static final String COLUMN_PRODUCT_NAME = "name";
        public static final String COLUMN_PRODUCT_QUANTITY = "quantity";
        public static final String COLUMN_PRODUCT_PRICE = "price";
        public static final String COLUMN_PRODUCT_IMAGE = "image";

    }
}
