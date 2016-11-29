package com.morlunk.mumbleclient.internet;

import com.morlunk.mumbleclient.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

public class JsonUtil {
	public static String getString (JSONObject obj,String name) throws JSONException {
		Log.getClassInfo();
		if (obj.isNull(name)) {
			return "";
		}
		return obj.getString(name);
		
	}

}
