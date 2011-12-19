/*******************************************************************************
 * Copyright (c) 2011 ETH Zurich.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     Paolo Carta - Implementation
 *     Theus Hossmann - Implementation
 *     Dominik Schatzmann - Message specification
 ******************************************************************************/

package ch.ethz.twimight.activities;

import ch.ethz.twimight.R;
import ch.ethz.twimight.net.twitter.DirectMessages;
import ch.ethz.twimight.util.Constants;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

/**
 * The activity to write a new tweet.
 * @author thossmann
 *
 */
public class NewDMActivity extends Activity{

	private static final String TAG = "NewDMActivity";
	
	private EditText text;
	private EditText recepient;
	private TextView characters;
	private Button cancelButton;
	private Button sendButton;
		
	private TextWatcher textWatcher;
	
	/** 
	 * Called when the activity is first created. 
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.new_dm);

		cancelButton = (Button) findViewById(R.id.dm_cancel);
		cancelButton.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View v) {
				finish();
			}
			
		});
		
		sendButton = (Button) findViewById(R.id.dm_send);
		sendButton.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View v) {
				sendDM();
				finish();
			}
			
		});
		
		characters = (TextView) findViewById(R.id.dm_characters);
		characters.setText(Integer.toString(Constants.TWEET_LENGTH));
		
		Intent i = getIntent();
		
		text = (EditText) findViewById(R.id.dmText);
		if(i.hasExtra("text")){
			text.setText(i.getStringExtra("text"));
			text.setSelection(text.getText().length());
		}
		
		if(text.getText().length()==0){
			sendButton.setEnabled(false);
		}
		
		if(text.getText().length()>Constants.TWEET_LENGTH){
			text.setText(text.getText().subSequence(0, Constants.TWEET_LENGTH));
			text.setSelection(text.getText().length());
    		characters.setTextColor(Color.RED);
		}
		
		characters.setText(Integer.toString(Constants.TWEET_LENGTH-text.getText().length()));
		
		// This makes sure we do not enter more than 140 characters	
		textWatcher = new TextWatcher(){
		    public void afterTextChanged(Editable s){
		    	int nrCharacters = Constants.TWEET_LENGTH-text.getText().length();
		    	
		    	if(nrCharacters < 0){
		    		text.setText(text.getText().subSequence(0, Constants.TWEET_LENGTH));
		    		text.setSelection(text.getText().length());
		    		nrCharacters = Constants.TWEET_LENGTH-text.getText().length();
		    	}
		    	
		    	if(nrCharacters <= 0){
		    		characters.setTextColor(Color.RED);
		    	} else {
		    		characters.setTextColor(Color.BLACK);
		    	}
		    	
		    	if(nrCharacters == Constants.TWEET_LENGTH){
		    		sendButton.setEnabled(false);
		    	} else {
		    		sendButton.setEnabled(true);
		    	}
		    	
		    	characters.setText(Integer.toString(nrCharacters));
		    	
		    }
		    public void  beforeTextChanged(CharSequence s, int start, int count, int after){}
		    public void  onTextChanged (CharSequence s, int start, int before,int count) {} 
		};
		text.addTextChangedListener(textWatcher);
		text.setSelection(text.getText().length());
		
		recepient = (EditText) findViewById(R.id.dmRecepient);
		// Did we get some extras in the intent?
		if(i.hasExtra("recipient")){
			recepient.setText(Html.fromHtml("<i>"+i.getStringExtra("recipient")+"</i>"));
			recepient.setSelection(recepient.getText().length());
			text.requestFocus();
		} else {
			recepient.requestFocus();
		}
		
		Log.i(TAG, "onCreated");
	}
	
	/**
	 * onResume
	 */
	@Override
	public void onResume(){
		super.onResume();
	}
	
	/**
	 * onPause
	 */
	@Override
	public void onPause(){
		super.onPause();
	}
	
	/**
	 * On Destroy
	 */
	@Override
	public void onDestroy(){
		super.onDestroy();
		
		cancelButton.setOnClickListener(null);
		cancelButton = null;
		
		sendButton.setOnClickListener(null);
		sendButton = null;
		
		text.removeTextChangedListener(textWatcher);
		text = null;
		textWatcher = null;
		
		unbindDrawables(findViewById(R.id.showNewDMRoot));
	}
	
	/**
	 * Checks whether we are in disaster mode and inserts the content values into the content provider.
	 */
	private void sendDM(){
		Log.i(TAG, "send DM!");
		// if no connectivity, notify user that the tweet will be send later
		try{
			ContentValues cv = createContentValues();
			if(PreferenceManager.getDefaultSharedPreferences(this).getBoolean("prefDisasterMode", false) == true){				
				// our own DMs go into the my disaster dm buffer
				cv.put(DirectMessages.COL_BUFFER, DirectMessages.BUFFER_MYDISASTER|DirectMessages.BUFFER_MESSAGES);
				getContentResolver().insert(Uri.parse("content://" + DirectMessages.DM_AUTHORITY + "/" + DirectMessages.DMS + "/" + DirectMessages.DMS_LIST + "/" + DirectMessages.DMS_SOURCE_DISASTER), cv);
			} else {
				
				// our own DMs go into the messages buffer
				cv.put(DirectMessages.COL_BUFFER, DirectMessages.BUFFER_MESSAGES);
				getContentResolver().insert(Uri.parse("content://" + DirectMessages.DM_AUTHORITY + "/" + DirectMessages.DMS + "/" + DirectMessages.DMS_LIST + "/" + DirectMessages.DMS_SOURCE_NORMAL), cv);
				
				ConnectivityManager cm = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
				if(cm.getActiveNetworkInfo()==null || !cm.getActiveNetworkInfo().isConnected()){
					Toast.makeText(this, "No connectivity, your Message will be sent once we have a connection!", Toast.LENGTH_SHORT).show();
				}				
			}
			
		} catch (Exception e) {
			Log.e(TAG, "Exception while inserting DM into DB: " + e.toString());
			Toast.makeText(this, "There was an error inserting your DM into the local database! Please try again.", Toast.LENGTH_SHORT).show();
			return;
		}
	}

	/**
	 * Prepares the content values of the DM for insertion into the DB.
	 * @return
	 */
	private ContentValues createContentValues() {
		
		ContentValues dmContentValues = new ContentValues();
		
		dmContentValues.put(DirectMessages.COL_TEXT, text.getText().toString());
		dmContentValues.put(DirectMessages.COL_SENDER, LoginActivity.getTwitterId(this));
		dmContentValues.put(DirectMessages.COL_RECEIVER_SCREENNAME, recepient.getText().toString());
		
		// we mark the tweet for sending to twitter
		dmContentValues.put(DirectMessages.COL_FLAGS, DirectMessages.FLAG_TO_INSERT);
		
		return dmContentValues;
	}
	
	/**
	 * Clean up the views
	 * @param view
	 */
	private void unbindDrawables(View view) {
	    if (view.getBackground() != null) {
	        view.getBackground().setCallback(null);
	    }
	    if (view instanceof ViewGroup) {
	        for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
	            unbindDrawables(((ViewGroup) view).getChildAt(i));
	        }
	        try{
	        	((ViewGroup) view).removeAllViews();
	        } catch(UnsupportedOperationException e){
	        	// No problem, nothing to do here
	        }
	    }
	}
}