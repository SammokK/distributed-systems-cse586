package edu.buffalo.cse.cse486586.simpledynamo;

import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

/**
 * Created by SammokKabasi on 3/15/16.
 */
public class Constants {
    public static final String KEY = "key";
    public static final String VALUE = "value";
    public static final String SIMPLE_DHT = "SIMPLE_DHT";
    public static final String SERVER_PORT = "10000";
    public static final String god = "11108"; //hardcoded
    public static final String REMOTE_PORTS_IN_CONNECTED_ORDER[] = {"11112", "11108", "11116", "11120", "11124"};
    public static String hashedPortsInAscendingOrder[] = new String[5];
    public static String hash[];

    static {
        for(int i = 0; i<5; i++) {
            try {
                hashedPortsInAscendingOrder[i] = new String(Helper.genHash(REMOTE_PORTS_IN_CONNECTED_ORDER[i]));
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            }
        }

    }






}
