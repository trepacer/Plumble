package com.morlunk.mumbleclient.internet;

import org.json.JSONException;

public interface RequestListener {
	public void onSucess(Object info) throws JSONException ;
	public void onFailed(Exception e) ;
	public void onError(Exception e);
}
