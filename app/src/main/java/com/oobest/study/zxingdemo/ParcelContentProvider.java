package com.oobest.study.zxingdemo;

import android.content.ContentProvider;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.provider.BaseColumns;
import android.support.annotation.NonNull;

import java.util.ArrayList;

public class ParcelContentProvider extends ContentProvider {
    public static final String AUTHORITY = "com.oobest.study.zxingdemo";

    public static final int ALL_PARCELS = 10;

    public static final int SINGLE_PARCEL = 20;

    public static final String PARCEL_TABLE = "parcel";

    public static final String[] ALL_COLUMNS = new String[]{
            ParcelColumns._ID,
            ParcelColumns.OBJECT_ID,
            ParcelColumns.STATUS,
            ParcelColumns.ORDER_ID,
            ParcelColumns.COM_CODE,
            ParcelColumns.CREATED_AT,
            ParcelColumns.UPDATED_AT
    };

    public static final String DATABASE_NAME = "ParcelProvider";

    public static final int DATABASE_VERSION = 1;

    public static final String TAG = "ParcelContentProvider";

    public static final String CREATE_SQL = "CREATE TABLE "
            + PARCEL_TABLE + " ("
            + ParcelColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
            + ParcelColumns.OBJECT_ID + " TEXT NOT NULL, "
            + ParcelColumns.STATUS + " TEXT NOT NULL, "
            + ParcelColumns.ORDER_ID + " TEXT NOT NULL, "
            + ParcelColumns.COM_CODE + " TEXT NOT NULL, "
            + ParcelColumns.CREATED_AT + " INTEGER DEFAULT 0, "
            + ParcelColumns.CREATED_AT + " INTEGER DEFAULT 0);";

    public static final String CREATED_INDEX_SQL = "CREATE INDEX "
            + ParcelColumns.CREATED_AT + "_idx ON " + PARCEL_TABLE + " (" + ParcelColumns.CREATED_AT + " ASC);";

    public static final String ORDER_INDEX_SQL = "CREATE INDEX "
            + ParcelColumns.ORDER_ID + "_idx ON " + PARCEL_TABLE + " (" + ParcelColumns.CREATED_AT + " ASC);";

    public static UriMatcher mUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    public MyDatabaseHelper mOpenHelper;

    static {
        mUriMatcher.addURI(AUTHORITY, "parcel", ALL_PARCELS);
        mUriMatcher.addURI(AUTHORITY, "parcel/#", SINGLE_PARCEL);
    }


    public interface ParcelColumns extends BaseColumns {
        String OBJECT_ID = "objectId";
        String STATUS = "status";
        String ORDER_ID = "orderId";
        String COM_CODE = "comCode";
        String CREATED_AT = "createAt";
        String UPDATED_AT = "updatedAt";
    }

    public static String[] fixSelectionArgs(String[] selectionArgs, String taskId) {
        if (selectionArgs == null) {
            selectionArgs = new String[]{taskId};
        } else {
            String[] newSelectionArg = new String[selectionArgs.length + 1];
            newSelectionArg[0] = taskId;
            System.arraycopy(selectionArgs, 0, newSelectionArg, 1, selectionArgs.length);
        }
        return selectionArgs;
    }

    public static String fixSelectionString(String selection) {
        selection = selection == null ? ParcelColumns._ID + " = ?" : ParcelColumns._ID + " = ? AND (" + selection + ")";
        return selection;
    }

    public ParcelContentProvider() {
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        SQLiteDatabase database = mOpenHelper.getReadableDatabase();

        switch (mUriMatcher.match(uri)) {
            case ALL_PARCELS:
                return database.delete(PARCEL_TABLE, selection, selectionArgs);
            case SINGLE_PARCEL:
                String taskId = uri.getLastPathSegment();
                selection = fixSelectionString(selection);
                selectionArgs = fixSelectionArgs(selectionArgs, taskId);
                return database.delete(PARCEL_TABLE, selection, selectionArgs);
            default:
                throw new IllegalArgumentException("Invalid Uri: " + uri);
        }
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    private Uri doInsert(Uri uri, ContentValues values, SQLiteDatabase database) {
        Uri result = null;
        switch (mUriMatcher.match(uri)) {
            case ALL_PARCELS:
                long id = database.insert(PARCEL_TABLE, "", values);
                if (id == -1) throw new SQLException("Error inserting data!");
                result = Uri.withAppendedPath(uri, String.valueOf(id));
        }
        return result;
    }


    @Override
    public Uri insert(Uri uri, ContentValues values) {
        SQLiteDatabase database = mOpenHelper.getWritableDatabase();
        Uri result = doInsert(uri, values, database);
        return result;
    }

    @Override
    public int bulkInsert(@NonNull Uri uri, @NonNull ContentValues[] contentValueses) {
        SQLiteDatabase database = mOpenHelper.getWritableDatabase();
        int count = 0;
        try {
            database.beginTransaction();
            for (ContentValues values : contentValueses) {
                Uri resultUri = doInsert(uri, values, database);
                if (resultUri != null) {
                    count++;
                } else {
                    count = 0;
                    throw new SQLException("Error in bulk insert");
                }
            }
            database.setTransactionSuccessful();
        } finally {
            database.endTransaction();
        }
        return count;
    }

    @NonNull
    @Override
    public ContentProviderResult[] applyBatch(@NonNull ArrayList<ContentProviderOperation> operations) throws OperationApplicationException {
        SQLiteDatabase database = mOpenHelper.getWritableDatabase();
        ContentProviderResult[] result = new ContentProviderResult[operations.size()];
        try {
            database.beginTransaction();
            for (int i = 0; i < operations.size(); i++) {
                ContentProviderOperation operation = operations.get(i);
                result[i] = operation.apply(this, result, i);
            }
            database.setTransactionSuccessful();
        } finally {
            database.endTransaction();
        }
        return result;
    }

    @Override
    public boolean onCreate() {
        mOpenHelper = new MyDatabaseHelper(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {
        projection = projection == null ? ALL_COLUMNS : projection;
        sortOrder = sortOrder == null ? ParcelColumns.CREATED_AT : sortOrder;
        SQLiteDatabase database = mOpenHelper.getReadableDatabase();

        switch (mUriMatcher.match(uri)) {
            case ALL_PARCELS:
                return database.query(PARCEL_TABLE, projection, selection, selectionArgs, null, null, sortOrder);
            case SINGLE_PARCEL:
                String taskId = uri.getLastPathSegment();
                selection = fixSelectionString(selection);
                selectionArgs = fixSelectionArgs(selectionArgs, taskId);
                return database.query(PARCEL_TABLE, projection, selection, selectionArgs, null, null, sortOrder);
            default:
                throw new IllegalArgumentException("Invalid Uri: " + uri);
        }
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection,
                      String[] selectionArgs) {
        SQLiteDatabase database = mOpenHelper.getReadableDatabase();

        switch (mUriMatcher.match(uri)) {
            case ALL_PARCELS:
                return database.update(PARCEL_TABLE, values, selection, selectionArgs);
            case SINGLE_PARCEL:
                String taskId = uri.getLastPathSegment();
                selection = fixSelectionString(selection);
                selectionArgs = fixSelectionArgs(selectionArgs, taskId);
                return database.update(PARCEL_TABLE, values, selection, selectionArgs);
            default:
                throw new IllegalArgumentException("Invalid Uri: " + uri);
        }
    }


    private class MyDatabaseHelper extends SQLiteOpenHelper {


        public MyDatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase database) {
            database.execSQL(CREATE_SQL);
            database.execSQL(CREATED_INDEX_SQL);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        }
    }
}
