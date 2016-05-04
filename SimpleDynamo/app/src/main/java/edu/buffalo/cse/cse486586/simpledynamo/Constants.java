package edu.buffalo.cse.cse486586.simpledynamo;

import java.util.HashMap;

/**
 * Created by SammokKabasi on 3/15/16.
 */
public class Constants {
    public static final String KEY = "key";
    public static final String VALUE = "value";
    public static final String SIMPLE_DYNAMO = "SIMPLE_DYNAMO";
    public static final String SERVER_PORT = "10000";
    public static final String god = "11108"; //hardcoded
    public static final String REMOTE_PORTS_IN_CONNECTED_ORDER[] = { "11112", "11108", "11116","11120", "11124"};

    public static HashMap<String,String> hashMap = new HashMap<String, String>();

    static {
        for (int i = 0; i < 5; i++) {
            hashMap.put(REMOTE_PORTS_IN_CONNECTED_ORDER[i], String.valueOf(Integer.parseInt(REMOTE_PORTS_IN_CONNECTED_ORDER[i]) / 2));
        }
    }
}





