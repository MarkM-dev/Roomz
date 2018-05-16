package mobi.roomz.ui;

import mobi.roomz.R;
import mobi.roomz.db.DBHandler;
import mobi.roomz.db.consts_interface;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

import com.parse.ParseAnalytics;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParsePush;
import com.parse.ParseUser;
import com.parse.SaveCallback;

@SuppressLint("NewApi") 
public class HostPage extends Activity implements consts_interface {
	
	private EditText channel_name_editText, nick_in_channel_editText;
	private DBHandler database = null;
	private SharedPreferences pref;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.host_page);
		
		pref =  getSharedPreferences("settings", MODE_PRIVATE);
		database = new DBHandler(this);
		
		Button back_button = (Button) findViewById(R.id.host_page_back_button);
		final Button host_button = (Button) findViewById(R.id.host_page_host_button);
		
		channel_name_editText = (EditText) findViewById(R.id.host_page_room_name_editText);
		final Button channel_name_clear_button = (Button)findViewById(R.id.host_page_room_name_clear_button);
		nick_in_channel_editText = (EditText) findViewById(R.id.host_page_room_nickname_editText);
		final Button nick_in_channel_clear_button = (Button)findViewById(R.id.host_page_room_nickname_clear_button);
		
		nick_in_channel_editText.setImeActionLabel("Host", EditorInfo.IME_ACTION_GO);
		
		channel_name_editText.addTextChangedListener(new TextWatcher() {
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				if (s.length() == 0) {
					host_button.setTextColor(getResources().getColor(R.color.LightSlateGray));
					channel_name_clear_button.setBackgroundResource(R.drawable.abc_ic_clear_search_api_disabled_holo_light);
				} else {
					channel_name_clear_button.setBackgroundResource(R.drawable.abc_ic_clear_search_api_holo_light);
					if (nick_in_channel_editText.getText().toString().length() != 0) {
						host_button.setTextColor(getResources().getColor(R.color.White));
					}
				}
			}
			
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
			@Override
			public void afterTextChanged(Editable s) {}
		});
		
		nick_in_channel_editText.addTextChangedListener(new TextWatcher() {
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				if (s.length() == 0) {
					host_button.setTextColor(getResources().getColor(R.color.LightSlateGray));
					nick_in_channel_clear_button.setBackgroundResource(R.drawable.abc_ic_clear_search_api_disabled_holo_light);
				} else {
					nick_in_channel_clear_button.setBackgroundResource(R.drawable.abc_ic_clear_search_api_holo_light);
					if (channel_name_editText.getText().toString().length() != 0) {
						host_button.setTextColor(getResources().getColor(R.color.White));
					}
				}
			}
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
			@Override
			public void afterTextChanged(Editable s) {}
		});
		
		nick_in_channel_editText.setOnEditorActionListener(new OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if(actionId==EditorInfo.IME_ACTION_GO){
                	host_room();
                }
                return false;
            }
        });
		
		// load previous nickname from shared preferences.
		nick_in_channel_editText.setText(pref.getString("nickname", ""));
		
		back_button.setOnClickListener(new OnClickListener() {@Override	public void onClick(View v) {finish();}});
		host_button.setOnClickListener(new OnClickListener() {@Override public void onClick(View v) {host_room();}});
		
		// clear editTexts on click.
		channel_name_clear_button.setOnClickListener(new View.OnClickListener() {@Override public void onClick(View v) {channel_name_editText.setText("");}});
		nick_in_channel_clear_button.setOnClickListener(new View.OnClickListener() {@Override public void onClick(View v) {nick_in_channel_editText.setText("");}});
	
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		// open keyboard.
		channel_name_editText.postDelayed(new Runnable() {
           @Override
           public void run() {
               InputMethodManager keyboard = (InputMethodManager)
               getSystemService(Context.INPUT_METHOD_SERVICE);
               keyboard.showSoftInput(channel_name_editText, 0);
           }
       },200);
	}
	
	@Override
	protected void onPause() {
		super.onPause();
	}

	private void host_room() {
		final String channel_name = channel_name_editText.getText().toString();
		final String nick_in_channel = nick_in_channel_editText.getText().toString().trim();
		
		// if all required fields are filled. == true.
		if (check_empty_fields(channel_name, nick_in_channel)) {
			
			if (isNetworkConnected()) {
			
				ContextThemeWrapper ctw = new ContextThemeWrapper(HostPage.this, R.style.Theme_Base);
				final ProgressDialog progress = new ProgressDialog(ctw);
				progress.setMessage("Hosting..");
				progress.setCancelable(false);
				progress.show();
				
				
				final ParseObject channelCreateObject = new ParseObject("Room");
				channelCreateObject.put("name", channel_name);
				channelCreateObject.put("secured", 0);
				channelCreateObject.put("owner", ParseUser.getCurrentUser());
				channelCreateObject.saveInBackground(new SaveCallback() {	
					@Override
					public void done(ParseException e) {
						if (e == null) {
							final String channel_id = channelCreateObject.getObjectId();
							
							ParseObject	roomPointer = ParseObject.createWithoutData("Room", channel_id);
							final ParseObject guestJoinObject = new ParseObject("Guest");
							guestJoinObject.put("user", ParseUser.getCurrentUser());
							guestJoinObject.put("nickname", nick_in_channel);
							guestJoinObject.put("room", roomPointer);
							guestJoinObject.put("admin", 1);
							guestJoinObject.saveInBackground(new SaveCallback() {
								@Override
								public void done(ParseException e) {
									String guest_id = guestJoinObject.getObjectId();
									
									// subscribe to get push notifications from the channel.
									ParsePush.subscribeInBackground(channel_id);
									database.hostChannel(channel_id,channel_name, nick_in_channel, guest_id, "wallpaper.jpg", 0);
									// disable FTE screen.
									SharedPreferences.Editor edit = pref.edit();
									edit.putBoolean("fte",false);
									edit.putString("nickname", nick_in_channel);
									edit.commit();
									
									// dismiss the loading pop-up.
									progress.dismiss();
									
									// go to chat page.
									Intent chatPage_intent = new Intent(HostPage.this, ChatPage.class);
									chatPage_intent.putExtra("channel_id", channel_id);
									startActivity(chatPage_intent);
									finish();
								}
							});
						} else {
							Toast toast = Toast.makeText(HostPage.this, "Error hosting - check connection.", Toast.LENGTH_LONG);
							toast.setGravity(Gravity.CENTER, 0, 0);
							toast.show();
							}
					}
				});
			}
		}
	}

	private Boolean check_empty_fields(String room_idOrName, String room_nickname) {
		// if both fields empty.
		if (room_idOrName.equals("") && room_nickname.equals("")) {
			Toast toast = Toast.makeText(HostPage.this, "Must enter room name and nickname", Toast.LENGTH_LONG);
			toast.setGravity(Gravity.CENTER, 0, 0);
			toast.show();
			channel_name_editText.requestFocus();
			return false;
		} else { // only one field is empty.
			
			// if room_id is empty.
			if (room_idOrName.equals("")) {
				Toast toast = Toast.makeText(HostPage.this, "Must enter room name", Toast.LENGTH_LONG);
				toast.setGravity(Gravity.CENTER, 0, 0);
				toast.show();
				channel_name_editText.requestFocus();
				return false;
			} else {
				
				// if nickname is empty.
				if (room_nickname.equals("")) {
					Toast toast = Toast.makeText(HostPage.this, "Must enter nickname", Toast.LENGTH_LONG);
					toast.setGravity(Gravity.CENTER, 0, 0);
					toast.show();
					nick_in_channel_editText.requestFocus();
					return false;
				} else {
					
					// both field are filled.
					return true;
				}
			}
		}
	}
	
	private boolean isNetworkConnected() {
		  ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		  NetworkInfo ni = cm.getActiveNetworkInfo();
		  if (ni == null) {
		   // There are no active networks.
			  ContextThemeWrapper ctw = new ContextThemeWrapper(HostPage.this, R.style.Theme_Base);
				AlertDialog.Builder builder = new AlertDialog.Builder(ctw);
				builder.setIcon(android.R.drawable.ic_dialog_alert);
				builder.setTitle("No connection.");
				builder.setMessage("An internet connection is required to join a room.");
				builder.setNeutralButton("Okay", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
					}
				});
				builder.show();
		   return false;
		  } else {
			  return true;
		  }
	 }
}
