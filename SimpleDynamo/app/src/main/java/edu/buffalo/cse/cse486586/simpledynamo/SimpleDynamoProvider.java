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
import static edu.buffalo.cse.cse486586.simpledynamo.Helper.lookupPredecessor;
import static edu.buffalo.cse.cse486586.simpledynamo.Helper.lookupSuccessor;

//import static edu.buffalo.cse.cse486586.simpledht.Helper.sendMessage;


public class SimpleDynamoProvider extends ContentProvider {
	static  SQLiteDatabase myDatabase = null;

	static String successorPort;
	static String predecessorPort;
	static String myPort= null;
	static  int recoveryCounter = 4;

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		while(recoveryMode) {
			try {
				Log.i("DELETE_PROVIDER", "Sleeping on recovery mode");
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		new Helper().recalculateHashValues();
		//todo check to see if the message is in this node or elsewhere, if it is this node continue to below line


		//find which three nodes must insert
		Message message = new Message(Message.MessageType.delete, myPort);
		HashMap<String, String> map = new HashMap<String, String>();
		message.setMessageMap(map);
		String primaryPort = findNode(selection);
		//send the messages
		deleteFromNode(selection, primaryPort);
		deleteFromNode(selection, lookupSuccessor(primaryPort));
		deleteFromNode(selection, lookupSuccessor(lookupSuccessor(primaryPort)));
		return 0;
	}

	private void deleteFromNode(String key, String port) {
		Log.i("DELETE_FROM_NODE", "Delete " + key + " to node " + port + " . My port is " + myPort);
		if (!port.equalsIgnoreCase(myPort)) {
			Message message = new Message(Message.MessageType.delete, myPort);
			message.setData(key);
			try {
				asyncSendMessage(message, port);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			Log.i("DELETE_FROM_NODE_LOCAL", "Deleting " + key + " from local db");
			myDatabase.delete(Constants.SIMPLE_DYNAMO, Constants.KEY + "=?", new String[]{key});
		}
	}
	@Override
	public String getType(Uri uri) {
		return null;
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		while(recoveryMode) {
			try {
				Log.i("INSERT_PROVIDER", "Sleeping on recovery mode");
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		// todo this is only local insertion. Add stuff for global insertion.
		Log.i(TAG, "start of INSERT " + values.toString());

		//find which three nodes must insert
		String key = (String) values.get(Constants.KEY);
		String value = (String) values.get(Constants.VALUE);
		Log.i("INSERT_PROVIDER", "key=" +key + " value=" + value);
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

	public  static boolean recoveryMode = false;
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
		ServerSocket serverSocket = null;
		new ServerTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, serverSocket );
		queryStarMessage = new Message(Message.MessageType.queryStarResult, "0");
		if(recoveryMode) {
			Log.i("RECOVERY_MODE", "Entering recovery mode");
			//do a query star, and insert it locally.

			Log.i("RECOVERY_MODE", "In recovery mode, querying everything in Dynamo");
			//select key, value from table
			Message message = new Message(Message.MessageType.recoveryQueryStar, myPort);
			for (int i = 0; i < 5; i++) {
				if (!Constants.REMOTE_PORTS_IN_CONNECTED_ORDER[i].equalsIgnoreCase(myPort)) {
					asyncSendMessage(message, Constants.REMOTE_PORTS_IN_CONNECTED_ORDER[i]);
				}
			}
			try {
				Log.d("RECOVERY_MODE", "Sleeping for 5000ms waiting for query result of query * ");
				Thread.sleep(5000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			recoveryCounter = 4;
			//construct a cursor object from the starMessageMap and return it
			HashMap<String, String> queryStarReturnMap = queryStarMessage.getMessageMap();
			MatrixCursor returnCursor = new MatrixCursor(new String[]{Constants.KEY, Constants.VALUE});
			for (String key : queryStarReturnMap.keySet()) {
				returnCursor.addRow(new String[]{key, queryStarReturnMap.get(key)});
			}
			Log.i("RECOVERY_MODE", "Query result for * in recovery mode" + queryStarReturnMap);
			//inset into the database
			for (String key : queryStarReturnMap.keySet()) {
//				if (isItThisNode(lookupPredecessor(myPort), key) || isItThisNode(lookupPredecessor(lookupPredecessor(myPort)), key) || isItThisNode(myPort, key)) {
				String portString = findNode(key);

				if (portString.equalsIgnoreCase(myPort) || portString.equalsIgnoreCase(lookupPredecessor(myPort))
						|| portString.equalsIgnoreCase(lookupPredecessor(lookupPredecessor(myPort)))) {
//					HashMap<String, String> insertMap = message.getMessageMap();
					//directly insert into this node.
					Log.i("RECOVERY_MODE", "Inserting " + key + "=" + queryStarReturnMap.get(key)  + " because it belongs to " + portString);
					ContentValues values = new ContentValues();
					values.put(Constants.KEY, key);
					values.put(Constants.VALUE, queryStarReturnMap.get(key));
					new Helper().insert(values, myDatabase);
				} else {
					Log.i("RECOVERY_MODE", "Discarding " + key + "=" + queryStarReturnMap.get(key) + " because it belongs to " + portString);
				}
			}

			recoveryMode = false;
			Log.i("RECOVERY_MODE", "Exiting recovery mode");
		}


		return true;
	}

	public static Message queryStarMessage = new Message(Message.MessageType.queryStarResult, "0");
	public static ConcurrentHashMap<String, String> queryMap = new ConcurrentHashMap<String, String>(25);
	Object lock = new Object();
	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
						String sortOrder) {
		while(recoveryMode) {
			try {
				Log.i("QUERY_PROVIDER", "Sleeping on recovery mode");
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		new Helper().recalculateHashValues();

		Log.i("QUERY_PROVIDER", "Querying " + selection);

		if (selection.equalsIgnoreCase("@")) {
			Log.i("QUERY_PROVIDER", "Querying everything in this node");
			//select key, value from table
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			Cursor cursor = new Helper().query(myDatabase);
			HashMap<String, String> map = new HashMap<String, String>();
			while (cursor.moveToNext()) {
				map.put(cursor.getString(cursor.getColumnIndex(Constants.KEY)), cursor.getString(cursor.getColumnIndex(Constants.VALUE)));
			}
//			Log.i("QUERY_RESULT", "query result " +map);
			return cursor;
		}

		if (selection.equalsIgnoreCase("*")) {
			synchronized (lock) {
				Log.i("QUERY_PROVIDER", "Querying everything in Dynamo");
				//select key, value from table
				Message message = new Message(Message.MessageType.queryStar, myPort);

				for(int i =0; i<5;i++) {
					asyncSendMessage(message, Constants.REMOTE_PORTS_IN_CONNECTED_ORDER[i]);
				}
				try {
					Log.d("QUERY_PROVIDER", "Sleeping for 2000ms waiting for query result of " + selection);
					Thread.sleep(2000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				//construct a cursor object from the starMessageMap and return it
				HashMap<String, String> queryStarReturnMap = queryStarMessage.getMessageMap();
				MatrixCursor returnCursor = new MatrixCursor(new String[]{Constants.KEY, Constants.VALUE});
				for (String key : queryStarReturnMap.keySet()) {
					returnCursor.addRow(new String[]{key, queryStarReturnMap.get(key)});
				}
				Log.i("QUERY_PROVIDER", "Query result for *" + returnCursor);
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
		Log.i("QUERY_PROVIDER", "Forwarding query " + selection);
		Message message = new Message(Message.MessageType.query, myPort);
		message.setData(selection);
		queryMap.put(selection, "null");
		asyncSendMessage(message, port);
		asyncSendMessage(message, lookupSuccessor(port));
		asyncSendMessage(message, lookupSuccessor(lookupSuccessor(port)));
		while (queryMap.get(selection).equalsIgnoreCase("null")) {
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




	@Override
	public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
		//Sammok: Nothing to see here... move along
		return 0;
	}

}
