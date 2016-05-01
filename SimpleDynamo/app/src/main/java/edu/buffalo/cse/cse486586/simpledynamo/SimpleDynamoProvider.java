package edu.buffalo.cse.cse486586.simpledynamo;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.AsyncTask;
import android.telephony.TelephonyManager;
import android.util.Log;

import java.net.ServerSocket;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

import static edu.buffalo.cse.cse486586.simpledynamo.Helper.asyncSendMessage;
import static edu.buffalo.cse.cse486586.simpledynamo.Helper.isItMyNode;

//import static edu.buffalo.cse.cse486586.simpledht.Helper.sendMessage;


public class SimpleDynamoProvider extends ContentProvider {
	static  SQLiteDatabase myDatabase = null;

	static String successorPort;
	static String secondSuccessorPort;
	static String predecessorPort;
	static String myPort = null;

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		new Helper().recalculateHashValues();
		//todo check to see if the message is in this node or elsewhere, if it is this node continue to below line
		myDatabase.delete(Constants.SIMPLE_DHT, Constants.KEY + "=?", new String[]{selection});
		return 0;
	}

	@Override
	public String getType(Uri uri) {
		return null;
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		new Helper().recalculateHashValues();
		// todo this is only local insertion. Add stuff for global insertion.
		Log.i(TAG, "start of INSERT " + values.toString());

		if (predecessorPort == null || successorPort == null) {
			Log.i("INSERT", "There is no successor or predecessor");
			long returner = new Helper().insert(values, myDatabase);
			Log.i("TAG", "inserted " + values.toString() + "," + "return value->" + returner);
			return null;
		} else {
			String key = (String)values.get(Constants.KEY);
			String value = (String)values.get(Constants.VALUE);

			Log.i("INSERT", "Inserting " + values);
			//check where the thing should be inserted
			if(isItMyNode(key)) {
				Log.i("INSERT", "Insert into my node");
				long returner = new Helper().insert(values, myDatabase);
				Log.i("INSERT", "inserted " + values.toString() + "," + "return value->" + returner);
				return null;
			} else {
				//forward the message to successor
				Log.i("INSERT", "Send to successor");
				Message message = new Message(Message.MessageType.insert, myPort);
				HashMap<String, String> map = new HashMap<String, String>();
				map.put(Constants.KEY, key);
				map.put(Constants.VALUE, value);
				message.setMessageMap(map);
				asyncSendMessage(message, successorPort);
				Log.i("INSERT", "Send to successor " + message);
			}
		}
		return null;
	}



	static final String TAG = SimpleDynamoProvider.class.getSimpleName();

	void initializePorts() {
		//calculate my port
		TelephonyManager tel = (TelephonyManager) getContext().getSystemService(Context.TELEPHONY_SERVICE);
		String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
		myPort = String.valueOf((Integer.parseInt(portStr) * 2));

		successorPort = lookupSuccessor(myPort);
		secondSuccessorPort = lookupSuccessor(successorPort);
		if (successorPort == null) {
			throw new RuntimeException("successor port is null... exiting");
		}
	}



	@Override
	public boolean onCreate() {
		Log.v(TAG, "Inside oncreate()");
		//todo dht creation stuff
		//todo create sockets

		initializePorts();

		Log.i(TAG, "My port is " + myPort);

		//check if this is the god server
		if (!myPort.equalsIgnoreCase(Constants.god)) {
//            try {
//                Thread.sleep(10000);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
			Log.v(TAG, "Contacting the god server....");
			Message message = new Message(Message.MessageType.godJoin, myPort);

			asyncSendMessage(message, Constants.god);
			Log.i(TAG, "Sent a join request to God server " + message);
		} else {
			Log.i(TAG, "This is the God server. Bow down to me, mortal.");
		}
		//db creation stuff goes here
		DbHelper dbHelper = new DbHelper(getContext(), "SimpleDht.db", null, 1);
		myDatabase = dbHelper.getWritableDatabase();
		myDatabase.execSQL("DROP TABLE IF EXISTS " + Constants.SIMPLE_DHT);
		myDatabase.execSQL("CREATE TABLE IF NOT EXISTS " + Constants.SIMPLE_DHT + " (" + Constants.KEY + " VARCHAR PRIMARY KEY NOT NULL, " + Constants.VALUE + " VARCHAR);");
		//create a sequential thingy that keeps listening for connections in a while loop
		ServerSocket serverSocket = null;
		new ServerTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, serverSocket );
		return true;
	}
	public static Message queryStarMessage = null;
	public static ConcurrentHashMap<String, String> queryMap = new ConcurrentHashMap<String, String>(25);
	Object lock = new Object();
	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
						String sortOrder) {
		new Helper().recalculateHashValues();

		Log.i("QUERY", "Querying " + selection);

		if(selection.equalsIgnoreCase("@")) {
			Log.i("QUERY", "Querying everything in this node");
			//select key, value from table
			Cursor cursor = new Helper().query(myDatabase);
			Log.i(TAG, "Query result for @");
			return cursor;
		}
		if(successorPort==null && !selection.equalsIgnoreCase("*")) {
			Log.i("QUERY", "Successor is null, so querying only this node for selection " + selection) ;
			Cursor cursor =  new Helper().query(selection, myDatabase);
			Log.i("QUERY", "query result " + cursor.toString());

			return cursor;
		}
		if(successorPort==null && selection.equalsIgnoreCase("*")) {
			Log.i("QUERY", "Successor is null, so querying only this node for * ") ;
			Cursor cursor =  new Helper().query(myDatabase);
			Log.i("QUERY", "query result " + cursor.toString());

			return cursor;
		}
		if (selection.equalsIgnoreCase("*")) {
			synchronized (lock) {
				Log.i("QUERY", "Querying everything in DHT");
				//select key, value from table
				Message message = new Message(Message.MessageType.queryStar, myPort);
				HashMap<String, String> map = new HashMap<String, String>();
				Cursor cursor = new Helper().query(myDatabase);
				cursor.moveToPosition(-1);
				while (cursor.moveToNext()) {
					map.put(cursor.getString(cursor.getColumnIndex(Constants.KEY)), cursor.getString(cursor.getColumnIndex(Constants.VALUE)));
				}
				Log.i("QUERY", "map object at this point " + map);
				message.setMessageMap(map);
				asyncSendMessage(message, successorPort);
				while (queryStarMessage == null) {
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				//construct a cursor object from the starMessageMap and return it

				HashMap<String, String> queryStarReturnMap = queryStarMessage.getMessageMap();
				MatrixCursor returnCursor = new MatrixCursor(new String[]{Constants.KEY, Constants.VALUE});
				for (String key : queryStarReturnMap.keySet()) {
					returnCursor.addRow(new String[]{key, queryStarReturnMap.get(key)});
				}
				Log.i(TAG, "Query result for *" + returnCursor);
				return returnCursor;
			}
		}
		//todo:  this is a local query. Check to see if the message should be inserted in this node or not before inserting
		//select key, value from table where key = selection

		Log.i("QUERY", "This is a regular query with selection " + selection);
		//this is a regular query

		if(isItMyNode(selection)) {
			Log.i("QUERY", "Querying local node");
			return new Helper().query(selection, myDatabase);
		} else {
			//forward
			Log.i("QUERY", "Forwarding query " + selection );
			Message message = new Message(Message.MessageType.query, myPort);
			message.setData(selection);
			queryMap.put(selection, "null");
			Log.i("QUERY", "successor port at this point is " + successorPort );
			asyncSendMessage(message, successorPort);
			while (queryMap.get(selection) .equalsIgnoreCase("null")) {
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			String result = queryMap.get(selection);
			MatrixCursor returnCursor = new MatrixCursor(new String[]{Constants.KEY, Constants.VALUE});
			returnCursor.addRow(new String[]{selection, result});
			Log.i("QUERY", "Query results count " + returnCursor);
			return returnCursor;
		}
	}




	@Override
	public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
		//Sammok: Nothing to see here... move along
		return 0;
	}

}
