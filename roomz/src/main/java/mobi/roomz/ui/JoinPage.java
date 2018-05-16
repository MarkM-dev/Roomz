package mobi.roomz.ui;

import mobi.roomz.R;
import mobi.roomz.db.DBHandler;
import mobi.roomz.db.consts_interface;

import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources.NotFoundException;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.TextView.OnEditorActionListener;

import com.parse.GetCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParsePush;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;

@SuppressLint("NewApi") 
public class JoinPage extends Activity implements consts_interface {

	private TextView id_error_msg_textView, nickname_error_msg_textView;
	private EditText channel_id_editText, nick_in_channel_editText;
	private DBHandler database = null;
	private SharedPreferences pref;
	private String room_name;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.join_page);
		
		Button back_button = (Button) findViewById(R.id.join_page_back_button);
		final Button join_button = (Button) findViewById(R.id.join_page_join_button);
		
		pref =  getSharedPreferences("settings", MODE_PRIVATE);
		database = new DBHandler(this);
		
		channel_id_editText = (EditText) findViewById(R.id.join_page_room_id_editText);
		final Button id_clear_button = (Button)findViewById(R.id.join_page_room_id_clear_button);
		id_error_msg_textView = (TextView)findViewById(R.id.join_page_room_id_error_textView);
		nickname_error_msg_textView = (TextView)findViewById(R.id.join_page_nickname_error_textView);
		nick_in_channel_editText = (EditText) findViewById(R.id.join_page_room_nickname_editText);
		final Button nick_in_channel_clear_button = (Button)findViewById(R.id.join_page_room_nickname_clear_button);
		
		// user entered via deep-link.
		Intent intent = getIntent();
		try {
			String channel_id = intent.getStringExtra("channel_id");
			if (channel_id != null) {
				String _channel_id = channel_id.substring(8);
				if (_channel_id.contains("/")) {
					_channel_id = _channel_id.replace("/", "");
				}
				channel_id_editText.setText(_channel_id);
				join_button.setTextColor(getResources().getColor(R.color.White));
				id_clear_button.setBackgroundResource(R.drawable.abc_ic_clear_search_api_holo_light);
				nick_in_channel_editText.requestFocus();
				nick_in_channel_editText.postDelayed(new Runnable() {
		            @Override
		            public void run() {
		            	nick_in_channel_editText.setSelection(nick_in_channel_editText.getText().length());
		                InputMethodManager keyboard = (InputMethodManager)
		                getSystemService(Context.INPUT_METHOD_SERVICE);
		                keyboard.showSoftInput(nick_in_channel_editText, 0);
		            }
		        },200);
				
			}
		} catch (NotFoundException e) {
			e.printStackTrace();
		}
		
		channel_id_editText.addTextChangedListener(new TextWatcher() {
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				id_error_msg_textView.setText("");
				if (s.length() == 0) {
					join_button.setTextColor(getResources().getColor(R.color.LightSlateGray));
					id_clear_button.setBackgroundResource(R.drawable.abc_ic_clear_search_api_disabled_holo_light);
				} else {
					id_clear_button.setBackgroundResource(R.drawable.abc_ic_clear_search_api_holo_light);
					if (nick_in_channel_editText.getText().toString().length() != 0) {
						join_button.setTextColor(getResources().getColor(R.color.White));
						
					}
				}
			}
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
			@Override
			public void afterTextChanged(Editable s) {}
		});
		nick_in_channel_editText.setImeActionLabel("Join", EditorInfo.IME_ACTION_GO);
			nick_in_channel_editText.addTextChangedListener(new TextWatcher() {
				@Override
				public void onTextChanged(CharSequence s, int start, int before, int count) {
					nickname_error_msg_textView.setText("");
					if (s.length() == 0) {
						join_button.setTextColor(getResources().getColor(R.color.LightSlateGray));
						nick_in_channel_clear_button.setBackgroundResource(R.drawable.abc_ic_clear_search_api_disabled_holo_light);
					} else {
						nick_in_channel_clear_button.setBackgroundResource(R.drawable.abc_ic_clear_search_api_holo_light);
						if (channel_id_editText.getText().toString().length() != 0) {
							join_button.setTextColor(getResources().getColor(R.color.White));
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
	                	join_room();
	                }
	                return false;
	            }
	        });
			
			// load previous nickname from shared preferences.
			nick_in_channel_editText.setText(pref.getString("nickname", ""));
			
			back_button.setOnClickListener(new OnClickListener() {@Override	public void onClick(View v) {finish();}});
			join_button.setOnClickListener(new OnClickListener() {@Override public void onClick(View v) {join_room();}});
			
			// clear editTexts on click.
			id_clear_button.setOnClickListener(new View.OnClickListener() {@Override public void onClick(View v) {
				channel_id_editText.setText("");
				id_error_msg_textView.setText("");
			}});
			nick_in_channel_clear_button.setOnClickListener(new View.OnClickListener() {@Override public void onClick(View v) {
				nick_in_channel_editText.setText("");
			}});
	}

	@Override
	protected void onResume() {
		super.onResume();
		// open keyboard.
		channel_id_editText.postDelayed(new Runnable() {
           @Override
           public void run() {
               InputMethodManager keyboard = (InputMethodManager)
               getSystemService(Context.INPUT_METHOD_SERVICE);
               keyboard.showSoftInput(channel_id_editText, 0);
           }
       },200);
	}
	
	@Override
	protected void onPause() {
		super.onPause();
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.join_page_optionsmenu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.joinPage_menu_join:
			id_error_msg_textView.setText("");
			join_room();
			break;
		default:
			break;
		}
		return super.onOptionsItemSelected(item);
	}
	
	private void join_room() {
		final String channel_id = channel_id_editText.getText().toString();
		final String nick_in_channel = nick_in_channel_editText.getText().toString().trim();

		// if all required fields are filled. == true.
		if (check_empty_fields(channel_id, nick_in_channel)) {
			
			if (isNetworkConnected()) {
				
				// if user has already joined the room.
				if (database.check_if_joined_room(channel_id)) {
					InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
					imm.hideSoftInputFromWindow(channel_id_editText.getWindowToken(), 0);
					Intent chatPage_intent = new Intent(JoinPage.this, ChatPage.class);
					chatPage_intent.putExtra("already_joined", true);
					chatPage_intent.putExtra("channel_id", channel_id);
					startActivity(chatPage_intent);
					finish();
				} else {
					// show loading pop-up.
					ContextThemeWrapper ctw = new ContextThemeWrapper(JoinPage.this, R.style.Theme_Base);
					final ProgressDialog progress = new ProgressDialog(ctw);
					progress.setMessage("Joining..");
					progress.setCancelable(false);
					progress.show();
					
					// get the room name from the cloud.
					ParseQuery<ParseObject> query = ParseQuery.getQuery("Room");
					query.whereEqualTo("objectId", channel_id);
					query.getFirstInBackground(new GetCallback<ParseObject>() {
							@Override
							public void done(ParseObject object, ParseException e) {
								if (e == null) {
									room_name = object.getString("name");
									final int secured = object.getInt("secured");
									final ParseObject roomPointer = ParseObject.createWithoutData("Room", channel_id);
									ParseQuery<ParseObject> guest_query = ParseQuery.getQuery("Guest");
									guest_query.whereEqualTo("room", roomPointer);
									guest_query.whereEqualTo("nickname", nick_in_channel);
									guest_query.getFirstInBackground(new GetCallback<ParseObject>() {
										@Override
										public void done(ParseObject object, ParseException e) {
											if (e == null) {
												progress.dismiss();
												nickname_error_msg_textView.setText("* '" + nick_in_channel + "' already taken in the room.");
											} else {
												
												final ParseObject guestJoinObject = new ParseObject("Guest");
												guestJoinObject.put("user", ParseUser.getCurrentUser());
												guestJoinObject.put("nickname", nick_in_channel);
												guestJoinObject.put("room", roomPointer);
												guestJoinObject.put("admin", 0);
												guestJoinObject.saveInBackground(new SaveCallback() {
													@Override
													public void done(ParseException e) {
														if (e != null) {
															Log.e("PROBLEM JOINING", e.toString());
														} else {
															String guest_id = guestJoinObject.getObjectId();
															
															// subscribe to get push notifications from the channel.
															ParsePush.subscribeInBackground(channel_id);
															database.joinChannel(channel_id, room_name, nick_in_channel, guest_id, "wallpaper.jpg", secured);
															
															// disable FTE screen.
															SharedPreferences.Editor edit = pref.edit();
															edit.putBoolean("fte",false);
															edit.putString("nickname", nick_in_channel);
															edit.commit();
															
															// dismiss the loading pop-up.
															progress.dismiss();
															
															Intent chatPage_intent = new Intent(JoinPage.this, ChatPage.class);
															chatPage_intent.putExtra("channel_id", channel_id);
															startActivity(chatPage_intent);
															
															
															JSONObject data = new JSONObject();
															try {
																data.put("action", PUSH_ACTION_SYSTEM_MESSAGE);
																data.put("channel_name", database.get_channel_name(channel_id));
																data.put("nickname", nick_in_channel);
																data.put("guest_id", PUSH_ACTION_SYSTEM_MESSAGE);
																data.put("message", PUSH_KEYPHRASE_USER_JOINED);
																data.put("message_time", System.currentTimeMillis());
															} catch (JSONException e1) {
																e1.printStackTrace();
															}
															ParsePush push = new ParsePush();
															push.setChannel(channel_id);
															push.setData(data);
															push.sendInBackground();
														}
														finish();
													}
												});
												
											}
										}
										
									});
									
									// if room doesn't exist.
								} else {
									progress.dismiss();
									id_error_msg_textView.setText("* Room doesn't exist - check id.");
								}
							}
						});
				}
			}	
		}
	}
	
	private Boolean check_empty_fields(String room_idOrName, String room_nickname) {
		// if both fields empty.
		if (room_idOrName.equals("") && room_nickname.equals("")) {
			Toast toast = Toast.makeText(JoinPage.this, "Must enter room id and nickname", Toast.LENGTH_LONG);
			toast.setGravity(Gravity.CENTER, 0, 0);
			toast.show();
			channel_id_editText.requestFocus();
			return false;
		} else { // only one field is empty.
			
			// if room_id is empty.
			if (room_idOrName.equals("")) {
				Toast toast = Toast.makeText(JoinPage.this, "Must enter room id", Toast.LENGTH_LONG);
				toast.setGravity(Gravity.CENTER, 0, 0);
				toast.show();
				channel_id_editText.requestFocus();
				return false;
			} else {
				
				// if nickname is empty.
				if (room_nickname.equals("")) {
					Toast toast = Toast.makeText(JoinPage.this, "Must enter nickname", Toast.LENGTH_LONG);
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
			  ContextThemeWrapper ctw = new ContextThemeWrapper(JoinPage.this, R.style.Theme_Base);
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

