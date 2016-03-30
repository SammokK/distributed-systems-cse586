package edu.buffalo.cse.cse486586.groupmessenger2;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.util.Log;

/**
 * GroupMessengerProvider is a sequence-value table. Once again, please note that we do not implement
 * full support for SQL as a usual ContentProvider does. We re-purpose ContentProvider's interface
 * to use it as a sequence-value table.
 * <p/>
 * Please read:
 * <p/>
 * http://developer.android.com/guide/topics/providers/content-providers.html
 * http://developer.android.com/reference/android/content/ContentProvider.html
 * <p/>
 * before you start to get yourself familiarized with ContentProvider.
 * <p/>
 * There are two methods you need to implement---insert() and query(). Others are optional and
 * will not be tested.
 *
 * @author stevko
 */
public class GroupMessengerProvider extends ContentProvider {

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        // You do not need to implement this.
        return 0;
    }

    @Override
    public String getType(Uri uri) {
        // You do not need to implement this.
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        /*
         * TODO: You need to implement this method. Note that values will have two columns (a sequence
         * column and a value column) and one row that contains the actual (sequence, value) pair to be
         * inserted.
         *
         * For actual storage, you can use any option. If you know how to use SQL, then you can use
         * SQLite. But this is not a requirement. You can use other storage options, such as the
         * internal storage option that we used in PA1. If you want to use that option, please
         * take a look at the code for PA1.
         */
        long returner = myDatabase.insertWithOnConflict(Constants.GROUP_MESSENGER, null, values, SQLiteDatabase.CONFLICT_REPLACE);

        Log.v("insert", values.toString()+","+"return value->" + returner);
        return uri;
    }

    SQLiteDatabase myDatabase = null;

    @Override
    public boolean onCreate() {
        // If you need to perform any one-time initialization task, please do it here.
        DbHelper dbHelper = new DbHelper(getContext(), "group-messenger1.db", null, 1);
        myDatabase = dbHelper.getWritableDatabase();
        myDatabase.execSQL("DROP TABLE IF EXISTS " + Constants.GROUP_MESSENGER);
        myDatabase.execSQL("CREATE TABLE IF NOT EXISTS " + Constants.GROUP_MESSENGER + " (" + Constants.KEY + " VARCHAR PRIMARY KEY NOT NULL, " + Constants.VALUE + " VARCHAR);");
        return false;
    }


    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        // You do not need to implement this.

//        long Constants.GROUP_MESSENGER = myDatabase.insertWithOnConflict(Constants.GROUP_MESSENGER, null, values, SQLiteDatabase.CONFLICT_REPLACE);

        return 0;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {
        /*
         * TODO: You need to implement this method. Note that you need to return a Cursor object
         * with the right format. If the formatting is not correct, then it is not going to work.
         *
         * If you use SQLite, whatever is returned from SQLite is a Cursor object. However, you
         * still need to be careful because the formatting might still be incorrect.
         *
         * If you use a file storage option, then it is your job to build a Cursor * object. I
         * recommend building a MatrixCursor described at:
         * http://developer.android.com/reference/android/database/MatrixCursor.html
         */


        //SAMMOK adding code here

        Cursor cursor = myDatabase.query(true, Constants.GROUP_MESSENGER, new String[] {Constants.KEY, Constants.VALUE}, Constants.KEY + " = ?", new String[]{selection}, null, null, null, null);


//        if (cursor != null && !cursor.isClosed()) {
//            cursor.close();
//        }
        Log.v("query", selection);
        return cursor;
    }
}
