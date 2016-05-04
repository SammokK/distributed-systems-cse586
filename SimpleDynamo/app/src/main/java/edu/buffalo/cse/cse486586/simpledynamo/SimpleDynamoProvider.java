package edu.buffalo.cse.cse486586.simpledynamo;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
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
import static edu.buffalo.cse.cse486586.simpledynamo.Helper.findNode;
import static edu.buffalo.cse.cse486586.simpledynamo.Helper.isItMyNode;
import static edu.buffalo.cse.cse486586.simpledynamo.Helper.lookupSuccessor;

//import static edu.buffalo.cse.cse486586.simpledht.Helper.sendMessage;


public class SimpleDynamoProvider extends ContentProvider {
	static  SQLiteDatabase myDatabase = null;

	static String successorPort;
	static String predecessorPort;
	static String myPort = null;

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		new Helper().recalculateHashValues();
		//todo check to see if the message is in this node or elsewhere, if it is this node continue to below line
		myDatabase.delete(Constants.SIMPLE_DYNAMO, Constants.KEY + "=?", new String[]{selection});
		return 0;
	}

	@Override
	public String getType(Uri uri) {
		return null;
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		// todo this is only local insertion. Add stuff for global insertion.
		Log.i(TAG, "start of INSERT " + values.toString());

		//find which three nodes must insert
		String key = (String) values.get(Constants.KEY);
		String value = (String) values.get(Constants.VALUE);
		Message message = new Message(Message.MessageType.insert, myPort);
		HashMap<String, String> map = new HashMap<String, String>();
		map.put(Constants.KEY, key);
		map.put(Constants.VALUE, value);
		message.setMessageMap(map);
		String primaryPort = findNode(key);
		//send the messages
		insertToNode(message, primaryPort);
		insertToNode(message, lookupSuccessor(primaryPort));
		insertToNode(message, lookupSuccessor(lookupSuccessor(primaryPort)));
		return null;
	}

	private void insertToNode(Message message, String port) {
		Log.i("INSERT_TO_NODE", "Inserting " + message + " to node " + port + " . My port is " + myPort);
		if (!port.equalsIgnoreCase(myPort)) {
			try {
				asyncSendMessage(message, port);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			Log.i("INSERT_TO_NODE_LOCAL", "Inserting " + message + " to local db");
			HashMap<String, String> insertMap = message.getMessageMap();
			ContentValues values = new ContentValues();
			values.put(Constants.KEY, insertMap.get(Constants.KEY));
			values.put(Constants.VALUE, insertMap.get(Constants.VALUE));
			new Helper().insert(values, myDatabase);
		}
	}


	static final String TAG = SimpleDynamoProvider.class.getSimpleName();

	void initializePorts() {
		//calculate my port
		TelephonyManager tel = (TelephonyManager) getContext().getSystemService(Context.TELEPHONY_SERVICE);
		String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
		myPort = String.valueOf((Integer.parseInt(portStr) * 2));
	}

	boolean recoveryMode = false;
	SharedPreferences preferences = null;
	@Override
	public boolean onCreate() {
		Log.v(TAG, "Inside oncreate()");
		//todo dht creation stuff
		//todo create sockets

		//Find out what my port number is
		initializePorts();

		Log.i(TAG, "My port is " + myPort);
		//db creation stuff goes here
		DbHelper dbHelper = new DbHelper(getContext(), "SimpleDht.db", null, 1);

		SharedPreferences isRecovery = this.getContext().getSharedPreferences("isRecovery", 0);
		if (isRecovery.getBoolean("isRecovery", true)) {
			isRecovery.edit().putBoolean("isRecovery", false).commit();
		} else {

			recoveryMode = true;
		}


		myDatabase = dbHelper.getWritableDatabase();
		myDatabase.execSQL("DROP TABLE IF EXISTS " + Constants.SIMPLE_DYNAMO);
		myDatabase.execSQL("CREATE TABLE IF NOT EXISTS " + Constants.SIMPLE_DYNAMO + " (" + Constants.KEY + " VARCHAR PRIMARY KEY NOT NULL, " + Constants.VALUE + " VARCHAR);");
		//create a sequential thingy that keeps listening for connections in a while loop


		if(recoveryMode) {




		}
		ServerSocket serverSocket = null;
		new ServerTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, serverSocket );
		return true;
	}

	public static Message queryStarMessage = new Message(Message.MessageType.queryStarResult, "0");
	public static ConcurrentHashMap<String, String> queryMap = new ConcurrentHashMap<String, String>(25);
	Object lock = new Object();
	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
						String sortOrder) {
		preferences.getString("isFirstTime", "0");
		new Helper().recalculateHashValues();

		Log.i("QUERY", "Querying " + selection);

		if (selection.equalsIgnoreCase("@")) {
			Log.i("QUERY", "Querying everything in this node");
			//select key, value from table
			Cursor cursor = new Helper().query(myDatabase);
			HashMap<String, String> map = new HashMap<String, String>();
			while (cursor.moveToNext()) {
				map.put(cursor.getString(cursor.getColumnIndex(Constants.KEY)), cursor.getString(cursor.getColumnIndex(Constants.VALUE)));
			}
			Log.i("QUERY_RESULT", "query result " +map);
			return cursor;
		}

		if (selection.equalsIgnoreCase("*")) {
			synchronized (lock) {
				Log.i("QUERY", "Querying everything in Dynamo");
				//select key, value from table
				Message message = new Message(Message.MessageType.queryStar, myPort);

				for(int i =0; i<5;i++) {
					asyncSendMessage(message, Constants.REMOTE_PORTS_IN_CONNECTED_ORDER[i]);
				}


//				HashMap<String, String> map = new HashMap<String, String>();
//				Cursor cursor = new Helper().query(myDatabase);
//				cursor.moveToPosition(-1);
//				while (cursor.moveToNext()) {
//					map.put(cursor.getString(cursor.getColumnIndex(Constants.KEY)), cursor.getString(cursor.getColumnIndex(Constants.VALUE)));
//				}
//				Log.i("QUERY", "map object at this point " + map);
//				message.setMessageMap(map);
//				asyncSendMessage(message, successorPort);
				while (queryStarMessage.getQueryStarCount() != 5) {
					try {
						Log.d("QUERY", "Sleeping for 100ms waiting for query result of " + selection);
						Thread.sleep(100);
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
				queryStarMessage = new Message(Message.MessageType.queryStarResult, "0");
				return returnCursor;
			}
		}
		//todo:  this is a local query. Check to see if the message should be inserted in this node or not before inserting
		//select key, value from table where key = selection

		Log.i("QUERY", "This is a regular query with selection " + selection);
		//this is a regular query

		//find which node
		String port = findNode(selection);
		if(port.equalsIgnoreCase(myPort)) {
			return new Helper().query(selection, myDatabase);
		} else {
			Log.i("QUERY", "Forwarding query " + selection );
			Message message = new Message(Message.MessageType.query,  myPort);
			message.setData(selection);
			queryMap.put(selection, "null");
			asyncSendMessage(message, port);
			asyncSendMessage(message, lookupSuccessor(port));
			asyncSendMessage(message, lookupSuccessor(lookupSuccessor(port)));
			while (queryMap.get(selection) .equalsIgnoreCase("null")) {
				try {
					Log.d("QUERY", "Sleeping for 100ms waiting for query result of " + selection);
					Thread.sleep(100);
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
