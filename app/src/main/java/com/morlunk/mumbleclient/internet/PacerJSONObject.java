package com.morlunk.mumbleclient.internet;


import com.morlunk.mumbleclient.util.Log;

import org.json.JSONObject;

public class PacerJSONObject extends JSONObject{
	
	JSONObject jsonObject;

	public PacerJSONObject(JSONObject jsonObject) {
		super();
		Log.getClassInfo();
		this.jsonObject = jsonObject;
	}
}
