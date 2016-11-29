package com.morlunk.mumbleclient.internet;

import android.content.Context;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.morlunk.mumbleclient.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

public class SendRequest {
	public static void openRequest(Context context,String method,Map<String, String> param,final RequestListener callBack) {
		Log.getClassInfo();
		String url = getUrl(method,param);
		
		RequestQueue mQueue =null;
		mQueue = Volley.newRequestQueue(context/*getApplicationContext()*/);
		StringRequest stringRequest = new StringRequest(url,
                new Response.Listener<String>() {  
                    @Override  
                    public void onResponse(String response) {
						Log.getClassInfo("Volly :"+ response);

						try {
							JSONObject ds = new JSONObject(response);
							callBack.onSucess(ds);
						} catch (JSONException e) {
							e.printStackTrace();
							callBack.onFailed(e);
						}
                    }  
                }, new Response.ErrorListener() {
                    @Override  
                    public void onErrorResponse(VolleyError error) {
						Log.getClassInfo("Volley :"+ error.getMessage());
                        callBack.onError(error);
                    }  
                }); 
		stringRequest.setRetryPolicy(new DefaultRetryPolicy(20 * 1000, 2, 2.0f));
		mQueue.add(stringRequest); 
//		mQueue.start();
	}
	@SuppressWarnings("deprecation")
	private static String getUrl(String method,Map<String, String> param){
		String url = "http://10.2.4.88:8080/" + method + "?";
		if(param != null){
			Iterator<Entry<String,String>> iter = param.entrySet().iterator();
			Entry<String,String> rel;
			while(iter.hasNext())
			{
				rel = iter.next();
				String parm = rel.getValue();
				if (parm == null) {
					parm = "";
				}
				url += rel.getKey() + "=" + java.net.URLEncoder.encode(parm)+"&";
			}
		}
//		url += PacerGlobal.urlSuffix;
		Log.getClassInfo("Volly url :"+ url);
		return url;
	}
}


