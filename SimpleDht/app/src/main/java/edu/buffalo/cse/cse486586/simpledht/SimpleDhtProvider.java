package edu.buffalo.cse.cse486586.simpledht;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.AsyncTask;
import android.telephony.TelephonyManager;
import android.util.Log;

import java.io.IOException;
import java.net.ServerSocket;

import static edu.buffalo.cse.cse486586.simpledht.Helper.asyncSendMessage;
//import static edu.buffalo.cse.cse486586.simpledht.Helper.sendMessage;


public class SimpleDhtProvider extends ContentProvider {
    SQLiteDatabase myDatabase = null;

    static String successorPort;
    static String predecessorPort;
    static String myPort = null;

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {

        //todo check to see if the message is in this node or elsewhere, if it is this node continue to below line
        myDatabase.delete(Constants.GROUP_MESSENGER, Constants.KEY + "=?", new String[]{selection});
        return 0;
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        // todo this is only local insertion. Add stuff for global insertion.

        long returner = myDatabase.insertWithOnConflict(Constants.GROUP_MESSENGER, null, values, SQLiteDatabase.CONFLICT_REPLACE);
        Log.v("insert", values.toString() + "," + "return value->" + returner);
        return null;
    }
    static final String TAG = SimpleDhtProvider.class.getSimpleName();

    @Override
    public boolean onCreate() {
        Log.v(TAG, "Inside oncreate()");
        //todo dht creation stuff
        //todo create sockets

        //calculate my port
        TelephonyManager tel = (TelephonyManager) getContext().getSystemService(Context.TELEPHONY_SERVICE);
        String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        myPort = String.valueOf((Integer.parseInt(portStr) * 2));
        Log.i(TAG, "My port is " + myPort);

        //check if this is the god server
        if (!myPort.equalsIgnoreCase(Constants.god)) {
            try {
                Thread.sleep(7000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            Log.v(TAG, "Contacting the god server....");
            Message message = new Message(Message.MessageType.godJoin, myPort);

            asyncSendMessage(message, Constants.god);
            Log.i(TAG, "Sent a join request to God server " + message);
        } else {
            Log.i(TAG, "This is the God server. Bow down to me, mortal.");
        }
    //db creation stuff goes here
       DbHelper dbHelper = new DbHelper(getContext(), "group-messenger1.db", null, 1);
        myDatabase = dbHelper.getWritableDatabase();
        myDatabase.execSQL("DROP TABLE IF EXISTS " + Constants.GROUP_MESSENGER);
        myDatabase.execSQL("CREATE TABLE IF NOT EXISTS " + Constants.GROUP_MESSENGER + " (" + Constants.KEY + " VARCHAR PRIMARY KEY NOT NULL, " + Constants.VALUE + " VARCHAR);");

        //create a sequential thingy that keeps listening for connections in a while loop
        ServerSocket serverSocket = null;
        new ServerTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, serverSocket );
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
            String sortOrder) {

        //todo:  this is a local query. Check to see if the message should be inserted in this node or not before inserting
        Cursor cursor = myDatabase.query(true, Constants.GROUP_MESSENGER, new String[]{Constants.KEY, Constants.VALUE}, Constants.KEY + " = ?", new String[]{selection}, null, null, null, null);
        Log.i(TAG, selection);
        return cursor;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        //Sammok: Nothing to see here... move along
        return 0;
    }

}
