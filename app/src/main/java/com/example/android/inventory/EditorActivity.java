package com.example.android.inventory;

import android.Manifest;
import android.app.Activity;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.NavUtils;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.FileProvider;
import android.support.v4.content.Loader;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.android.inventory.data.InventoryContract;
import com.example.android.inventory.data.InventoryContract.ProductEntry;
import com.example.android.inventory.util.BitmapUtility;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Allows user to create a new product or edit an existing one.
 */
public class EditorActivity extends AppCompatActivity
        implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final int PICK_IMAGE_REQUEST = 0;
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int MY_PERMISSIONS_REQUEST = 2;

    private static final String JPEG_FILE_PREFIX = "IMG_";
    private static final String JPEG_FILE_SUFFIX = ".jpg";

    private static final int EDIT_LOADER = 1;

    /** EditText field to enter the product's name */
    private EditText mNameEditText;

    /** EditText field to enter the product's quantity */
    private EditText mQuantityEditText;

    /** EditText field to enter the product's price */
    private EditText mPriceEditText;

    private ImageView mImageView;
    private Button mButtonTakePicture;

    private Uri mCurrentProductUri;

    private Uri mImageUri = null;
    private boolean mProductHasChanged = false;

    private View.OnTouchListener mTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            mProductHasChanged = true;
            return false;
        }
    };

    /***
     * Perform initialization of layout, variables and loader.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_editor);

        // Find all relevant views that we will need to read user input from
        mNameEditText = (EditText) findViewById(R.id.edit_product_name);
        mQuantityEditText = (EditText) findViewById(R.id.edit_product_quantity);
        mPriceEditText = (EditText) findViewById(R.id.edit_product_price);
        mImageView = (ImageView) findViewById(R.id.image);
        mButtonTakePicture = (Button) findViewById(R.id.take_picture);

        // Register a callback to be invoked when the global layout state or the visibility of
        // product image view changes
        ViewTreeObserver viewTreeObserver = mImageView.getViewTreeObserver();
        viewTreeObserver.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                mImageView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                mImageView.setImageBitmap(BitmapUtility.getBitmapFromUri(
                        getApplicationContext(), mImageUri,
                        mImageView.getWidth(), mImageView.getHeight()));
            }
        });

        mButtonTakePicture.setEnabled(false);

        requestPermissions();

        // Register a callback to be invoked when a touch event is dispatched to the input views
        mNameEditText.setOnTouchListener(mTouchListener);
        mQuantityEditText.setOnTouchListener(mTouchListener);
        mPriceEditText.setOnTouchListener(mTouchListener);

        // Get the URI from the intent that started this activity
        Intent intent = getIntent();
        Uri currentProductUri = intent.getData();

        // If URI is null so user has clicked on Add Product FAB
        // Else, user has clicked on specific product of the list
        if (currentProductUri == null) {
            // Set the activity title to add a product and invalidate the Delete option menu.
            setTitle(getString(R.string.editor_activity_title_new_product));
            invalidateOptionsMenu();
        } else {
            // Set the activity title to edit product and initialize the loader to get data for
            // the selected product in the Catalog activity
            setTitle(getString(R.string.editor_activity_title_edit_product));
            mCurrentProductUri = currentProductUri;
            getSupportLoaderManager().initLoader(EDIT_LOADER, null, this);
        }

        if (savedInstanceState != null) {
            mImageUri = savedInstanceState.getParcelable("ImageUri");
            mProductHasChanged = savedInstanceState.getBoolean("ProductHasChanged");
        }
    }

    /***
     * Request permissions to camera, read and write external storage
     */
    public void requestPermissions() {

        String[] permissions = new String[] {
                Manifest.permission.CAMERA,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE };

        if (ContextCompat.checkSelfPermission(this,
                    permissions[0]) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this,
                    permissions[1]) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this,
                    permissions[2]) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this, permissions, MY_PERMISSIONS_REQUEST);

        } else {

            mButtonTakePicture.setEnabled(true);
        }
    }

    /***
     * Callback for the result from requesting permissions.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[],
                                           @NonNull int[] grantResults) {

        if (requestCode == MY_PERMISSIONS_REQUEST) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED
                    && grantResults[1] == PackageManager.PERMISSION_GRANTED
                    && grantResults[2] == PackageManager.PERMISSION_GRANTED) {
                mButtonTakePicture.setEnabled(true);
            } else {
                Toast.makeText(getApplicationContext(), "Permission Denied",
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    /***
     * Called to retrieve state from an activity before being killed so that the state can be
     * restored
     */
    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);

        savedInstanceState.putParcelable("ImageUri", mImageUri);
        savedInstanceState.putBoolean("ProductHasChanged", mProductHasChanged);
    }

    /***
     * Instantiate and return a loader for the product editor loader ID.
     */
    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {

        String[] projection = {
                ProductEntry._ID,
                ProductEntry.COLUMN_PRODUCT_NAME,
                ProductEntry.COLUMN_PRODUCT_QUANTITY,
                ProductEntry.COLUMN_PRODUCT_PRICE,
                ProductEntry.COLUMN_PRODUCT_IMAGE};

        return new CursorLoader(this,
                mCurrentProductUri,
                projection,
                null,
                null,
                null);
    }

    /***
     * Called when the product loader has finished its load
     */
    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {

        if (!mProductHasChanged) {

            if (cursor.moveToFirst()) {

                // Get the indexes of product table columns
                int nameColumnIndex = cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_NAME);
                int quantityColumnIndex = cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_QUANTITY);
                int priceColumnIndex = cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_PRICE);
                int imageColumnIndex = cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_IMAGE);

                // Get the values of each field in database
                String name = cursor.getString(nameColumnIndex);
                int quantity = cursor.getInt(quantityColumnIndex);
                float price = cursor.getFloat(priceColumnIndex);
                String image = cursor.getString(imageColumnIndex);

                // Set the views with the given data
                mNameEditText.setText(name);
                mQuantityEditText.setText(String.format(Locale.getDefault(), "%d", quantity));
                mPriceEditText.setText(String.format(Locale.getDefault(), "%.2f", price));

                if (!TextUtils.isEmpty(image)) {
                    mImageUri = Uri.parse(image);
                    Bitmap bmp = BitmapUtility.getBitmapFromUri(
                            this, mImageUri, mImageView.getWidth(), mImageView.getHeight());
                    mImageView.setImageBitmap(bmp);
                }

            }
        }

        hideSoftKeyboard();

    }

    /***
     * Hide soft keyboard when activity starts
     */
    public void hideSoftKeyboard() {
        if (getCurrentFocus() != null) {
            InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            inputMethodManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
        }
    }

    /***
     * Called when the product loader is being reset, and thus making its data unavailable.
     */
    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mNameEditText.setText("");
        mQuantityEditText.setText("");
        mPriceEditText.setText("");
    }

    /***
     * Called when user click on Increase (+) or Decrease (-) buttons to modify the product
     * quantity
     */
    public void modifyQuantity(View view) {

        String quantityString = mQuantityEditText.getText().toString().trim();
        int quantity = 0;

        if (!quantityString.isEmpty()) {
            quantity = Integer.parseInt(quantityString);
        }

        if (view.getId() == R.id.increase_quantity) {
            quantity++;
            mProductHasChanged = true;
        } else {
            if (quantity > 0) {
                quantity--;
                mProductHasChanged = true;
            }
        }

        mQuantityEditText.setText(String.valueOf(quantity));
    }

    /***
     * Order more from the current product
     */
    public void orderProduct(View view) {

        String nameProduct = mNameEditText.getText().toString();

        // Call an intent to the Mail app
        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(Uri.parse("mailto:"));
        String[] address = new String[] {getString(R.string.supplier_email)};
        intent.putExtra(Intent.EXTRA_EMAIL, address);
        intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.subject_email) + " " + nameProduct);
        intent.putExtra(Intent.EXTRA_TEXT, getString(R.string.body_email, nameProduct));
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        }
    }

    /***
     * Delete the product
     */
    public void delete(View view) {
        showDeleteConfirmationDialog();
    }

    /***
     * Create the intent for selecting picture from gallery
     */
    public void selectPicture(View view) {
        Intent intent;

        if (Build.VERSION.SDK_INT < 19) {
            intent = new Intent(Intent.ACTION_GET_CONTENT);
        } else {
            intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
        }

        intent.setType("image/*");
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST);
    }

    /***
     * Create the intent for taking a new picture from camera
     */
    public void takePicture(View view) {

        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        if (intent.resolveActivity(getPackageManager()) != null) {

            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException e) {
                e.printStackTrace();
            }

            if (photoFile != null) {
                mImageUri = FileProvider.getUriForFile(
                        this,
                        getApplicationContext().getPackageName() + ".fileprovider",
                        photoFile);
                intent.putExtra(MediaStore.EXTRA_OUTPUT, mImageUri);
                intent.setFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                startActivityForResult(intent, REQUEST_IMAGE_CAPTURE);
            }
        }
    }

    /***
     * Get an output media file to take picture from camera.
     */
    private File createImageFile() throws IOException {

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmssSSS").format(new Date());
        String imageFileName = JPEG_FILE_PREFIX + timeStamp;
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return File.createTempFile(
                imageFileName,
                JPEG_FILE_SUFFIX,
                storageDir
        );
    }

    /***
     * Called when launched activity exits, giving the requestCode started it with, the resultCode
     * it returned, and additional data from it.
     *
     * If the result code were successful, then set the product image obtained from the gallery or
     * camera.
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {

        if (resultCode == Activity.RESULT_OK) {

            if (requestCode == PICK_IMAGE_REQUEST && resultData != null) {

                mImageUri = resultData.getData();
                mImageView.setImageBitmap(
                        BitmapUtility.getBitmapFromUri(
                                this, mImageUri, mImageView.getWidth(), mImageView.getHeight()));

            } else if (requestCode == REQUEST_IMAGE_CAPTURE) {

                mImageView.setImageBitmap(
                        BitmapUtility.getBitmapFromUri(
                                this, mImageUri, mImageView.getWidth(), mImageView.getHeight()));
            }

            mProductHasChanged = true;
        }
    }

    /***
     * Save the product in the database
     */
    private void saveProduct() {

        // If user is adding a new product or updating a existing one
        if (mProductHasChanged || mCurrentProductUri == null) {

            // Get the values from the input fields
            String name = mNameEditText.getText().toString().trim();
            String quantityString = mQuantityEditText.getText().toString().trim();
            String priceString = mPriceEditText.getText().toString().trim();
            String image = "";

            // Validate the name
            if (TextUtils.isEmpty(name)) {
                throw new IllegalArgumentException(getString(R.string.invalid_product_name_exception));
            }

            // Validate the quantity
            int quantity;
            if (TextUtils.isEmpty(quantityString)) {
                quantity = -1;
            } else {
                try {
                    quantity = Integer.parseInt(quantityString);
                } catch (Exception e) {
                    quantity = -1;
                }
            }

            if (quantity < 0) {
                throw new IllegalArgumentException(getString(R.string.invalid_product_quantity_exception));
            }

            // Validate the price
            float price;
            if (TextUtils.isEmpty(priceString)) {
                price = -1f;
            } else {
                try {
                    priceString = priceString.replace(",", ".");
                    price = Float.parseFloat(priceString);
                } catch (Exception e) {
                    price = -1f;
                }
            }

            if (price < 0) {
                throw new IllegalArgumentException(getString(R.string.invalid_product_price_exception));
            }

            if (mImageUri != null) {
                image = mImageUri.toString();
            }

            // Create a new map of values, where column names are the keys
            ContentValues values = new ContentValues();
            values.put(InventoryContract.ProductEntry.COLUMN_PRODUCT_NAME, name);
            values.put(ProductEntry.COLUMN_PRODUCT_QUANTITY, quantity);
            values.put(ProductEntry.COLUMN_PRODUCT_PRICE, price);
            values.put(ProductEntry.COLUMN_PRODUCT_IMAGE, image);

            if (mCurrentProductUri == null) {

                // Insert the new row, returning the primary key value of the new row
                Uri uri = getContentResolver().insert(ProductEntry.CONTENT_URI, values);

                long newRowId = ContentUris.parseId(uri);

                if (newRowId > 0) {
                    Toast.makeText(this, R.string.editor_insert_product_successful, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, R.string.editor_insert_product_failed, Toast.LENGTH_SHORT).show();
                }
            } else {

                // Update the existing row
                int rowsUpdated = getContentResolver().update(mCurrentProductUri, values, null, null);

                if (rowsUpdated == 0) {
                    Toast.makeText(this, R.string.editor_update_product_failed, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, R.string.editor_update_product_successful, Toast.LENGTH_SHORT).show();
                }

            }
        }

        finish();

    }

    /***
     * Initialize the contents of the Activity's standard options menu.
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu options from the res/menu/menu_editor.xml file.
        // This adds menu items to the app bar.
        getMenuInflater().inflate(R.menu.menu_editor, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        // User clicked on a menu option in the app bar overflow menu
        switch (item.getItemId()) {

            // Respond to a click on the "Save" menu option
            case R.id.action_save:
                try {
                    saveProduct();
                } catch (Exception e) {
                    Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
                }
                return true;

            // Respond to a click on the "Delete" menu option
            case R.id.action_delete:
                showDeleteConfirmationDialog();
                return true;

            // Respond to a click on the "Up" arrow button in the app bar
            case android.R.id.home:
                // If the product hasn't changed, continue with navigating up to parent activity
                if (!mProductHasChanged) {
                    NavUtils.navigateUpFromSameTask(EditorActivity.this);
                    return true;
                }

                // Otherwise if there are unsaved changes, setup a dialog to warn the user.
                // Create a click listener to handle the user confirming that
                // changes should be discarded.
                DialogInterface.OnClickListener discardButtonClickListener =
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                // User clicked "Discard" button, navigate to parent activity.
                                NavUtils.navigateUpFromSameTask(EditorActivity.this);
                            }
                        };

                // Show a dialog that notifies the user they have unsaved changes
                showUnsavedChangesDialog(discardButtonClickListener);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * This method is called when the back button is pressed.
     */
    @Override
    public void onBackPressed() {
        // If the product hasn't changed, continue with handling back button press
        if (!mProductHasChanged) {
            super.onBackPressed();
            return;
        }

        // Otherwise if there are unsaved changes, setup a dialog to warn the user.
        // Create a click listener to handle the user confirming that changes should be discarded.
        DialogInterface.OnClickListener discardButtonClickListener =
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // User clicked "Discard" button, close the current activity.
                        finish();
                    }
                };

        // Show dialog that there are unsaved changes
        showUnsavedChangesDialog(discardButtonClickListener);
    }

    /**
     * Show a dialog that warns the user there are unsaved changes that will be lost
     * if they continue leaving the editor.
     *
     * @param discardButtonClickListener is the click listener for what to do when
     *                                   the user confirms they want to discard their changes
     */
    private void showUnsavedChangesDialog(
            DialogInterface.OnClickListener discardButtonClickListener) {

        // Create an AlertDialog.Builder and set the message, and click listeners
        // for the postivie and negative buttons on the dialog.
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.unsaved_changes_dialog_msg);
        builder.setPositiveButton(R.string.discard, discardButtonClickListener);
        builder.setNegativeButton(R.string.keep_editing, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Keep editing" button, so dismiss the dialog
                // and continue editing the product.
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });

        // Create and show the AlertDialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    private void showDeleteConfirmationDialog() {

        // Create an AlertDialog.Builder and set the message, and click listeners
        // for the postivie and negative buttons on the dialog.
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.delete_dialog_msg);
        builder.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {

                // User clicked the "Delete" button, so delete the product.
                deleteProduct();
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {

                // User clicked the "Cancel" button, so dismiss the dialog
                // and continue editing the product.
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });

        // Create and show the AlertDialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    /**
     * Perform the deletion of the product in the database.
     */
    private void deleteProduct() {

        if (mCurrentProductUri != null) {

            // Delete the current product
            int rowsDeleted = getContentResolver().delete(mCurrentProductUri, null, null);

            if (rowsDeleted == 0){
                Toast.makeText(this, R.string.editor_delete_product_failed, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, R.string.editor_delete_product_successful, Toast.LENGTH_SHORT).show();
            }

            finish();
        }
    }
}