package com.stuffaboutcode.bluedot;

import java.util.LinkedHashMap;
import java.util.Map;


public class Data {
    public static final Map<String, Integer> ConnProfiles = createMap();
    private static Map<String, Integer> createMap()
    {
        Map<String,Integer> myMap = new LinkedHashMap();
        myMap.put("Profile One",1);
        myMap.put("Profile Two",2);
        myMap.put("Profile Three",3);
        myMap.put("Profile Four",4);
        myMap.put("Profile Five",5);
        myMap.put("Profile Six",6);
        return myMap;
    }
}
