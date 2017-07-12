package com.example.android.inventory;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.support.v4.content.FileProvider;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.android.inventory.data.InventoryContract.ProductEntry;
import com.example.android.inventory.util.BitmapUtility;

import java.util.Locale;

/**
 * {@link ProductCursorAdapter} is an adapter for a list or grid view
 * that uses a {@link Cursor} of product data as its data source. This adapter knows
 * how to create list items for each row of product data in the {@link Cursor}.
 */
class ProductCursorAdapter extends CursorAdapter {

    /**
     * Constructs a new {@link ProductCursorAdapter}.
     *
     * @param context The context
     * @param c       The cursor from which to get the data.
     */
    ProductCursorAdapter(Context context, Cursor c) {
        super(context, c, 0 /* flags */);
    }

    /**
     * Makes a new blank list item view. No data is set (or bound) to the views yet.
     *
     * @param context app context
     * @param cursor  The cursor from which to get the data. The cursor is already
     *                moved to the correct position.
     * @param parent  The parent to which the new view is attached to
     * @return the newly created list item view.
     */
    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return LayoutInflater.from(context).inflate(R.layout.list_item, parent, false);
    }

    /**
     * This method binds the product data (in the current row pointed to by cursor) to the given
     * list item layout. For example, the name for the current product can be set on the name
     * TextView in the list item layout.
     *
     * @param view    Existing view, returned earlier by newView() method
     * @param context app context
     * @param cursor  The cursor from which to get the data. The cursor is already moved to the
     *                correct row.
     */
    @Override
    public void bindView(View view, Context context, Cursor cursor) {

        // Find individual views that we want to modify in the list item layout
        TextView nameTextView = (TextView) view.findViewById(R.id.name);
        TextView priceTextView = (TextView) view.findViewById(R.id.price);
        final TextView quantityTextView = (TextView) view.findViewById(R.id.quantity);
        Button saleButton = (Button) view.findViewById(R.id.sale);
        ImageView imageView = (ImageView) view.findViewById(R.id.image);

        // Find the columns of product attributes that we're interested in
        int idColumnIndex = cursor.getColumnIndex(ProductEntry._ID);
        int nameColumnIndex = cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_NAME);
        int priceColumnIndex = cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_PRICE);
        int quantityColumnIndex = cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_QUANTITY);
        int imageColumnIndex = cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_IMAGE);

        // Read the product attributes from the Cursor for the current product
        int productId = cursor.getInt(idColumnIndex);
        String productName = cursor.getString(nameColumnIndex);
        final int itemQuantity = cursor.getInt(quantityColumnIndex);
        float itemPrice = cursor.getFloat(priceColumnIndex);
        String productImage = cursor.getString(imageColumnIndex);
        String productQuantity;
        if (itemQuantity > 0) {
            productQuantity = context.getString(R.string.in_stock, Integer.toString(itemQuantity));
        } else {
            productQuantity = context.getString(R.string.out_of_stock);
        }
        String productPrice = "$" + String.format(Locale.getDefault(), "%.2f", itemPrice) + " \u2014 ";

        // Update the ImageView with the product image attribute for the current product
        if (!TextUtils.isEmpty(productImage)) {

            Uri uri = Uri.parse(productImage);
            Bitmap bmp = BitmapUtility.getBitmapFromUri(context, uri, 40, 40);
            imageView.setImageBitmap(BitmapUtility.getCircleBitmap(bmp));
        }

        // Update the TextViews with the attributes for the current product
        nameTextView.setText(productName);
        priceTextView.setText(productPrice);
        quantityTextView.setText(productQuantity);

        final Context context1 = context;
        final Uri uri = ContentUris.withAppendedId(ProductEntry.CONTENT_URI, productId);

        // Register the callback for Sale button's click action
        saleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                int quantity = itemQuantity;
                if (quantity > 0) {
                    quantity --;
                    if (quantity > 0) {
                        quantityTextView.setText(context1.getString(R.string.in_stock,
                                Integer.toString(quantity)));
                    } else {
                        quantityTextView.setText(context1.getString(R.string.out_of_stock));
                    }

                    ContentValues values = new ContentValues();
                    values.put(ProductEntry.COLUMN_PRODUCT_QUANTITY, quantity);

                    context1.getContentResolver().update(uri, values, null, null);

                }
            }
        });
    }
}