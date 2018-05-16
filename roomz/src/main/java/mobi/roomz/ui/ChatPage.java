package mobi.roomz.ui;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import mobi.roomz.R;
import mobi.roomz.db.DBHandler;
import mobi.roomz.db.consts_interface;

import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources.NotFoundException;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.NavUtils;
import android.support.v4.widget.CursorAdapter;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ListView;
import android.widget.Toast;

import com.parse.FindCallback;
import com.parse.GetCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParsePush;
import com.parse.ParseQuery;
import com.parse.SaveCallback;
import com.parse.SendCallback;
import com.rockerhieu.emojicon.EmojiconEditText;
import com.rockerhieu.emojicon.EmojiconGridFragment;
import com.rockerhieu.emojicon.EmojiconsFragment;
import com.rockerhieu.emojicon.emoji.Emojicon;

@SuppressLint("NewApi")
public class ChatPage extends FragmentActivity implements consts_interface, EmojiconGridFragment.OnEmojiconClickedListener, EmojiconsFragment.OnEmojiconBackspaceClickedListener {
	EmojiconEditText mEditEmojicon;
 //   EmojiconTextView mTxtEmojicon;
	private AutoCompleteTextView user_input_actv;
	private DBHandler database = null;
	private ChatCursorAdapter adapter = null; 
	private Intent intent;
	private String channel_id, channel_name, nick_in_channel, guest_id, shared_text;
	private ActionBar actionBar;
	private ImageView wallpaper_imageView;
	private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,4}$", Pattern.CASE_INSENSITIVE);
	private final static DateFormat DATETIME_FORMATTER_DAY = new SimpleDateFormat("EEEE, MMM dd, yyyy");
	private int message_num_index = 1;
	private Set<String> channel_users_set;
	
	private BroadcastReceiver my_msg_receiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			Cursor cursor = database.get_channel_messages(channel_id, message_num_index);
			adapter.changeCursor(cursor);
			
			if (database.is_channel_secured(channel_id)) {
				getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
			}
		}
	};
	private BroadcastReceiver room_name_changed_receiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String new_channel_name = database.get_channel_name(channel_id);
			actionBar.setTitle(new_channel_name);
			
			Cursor cursor = database.get_channel_messages(channel_id, message_num_index);
			adapter.changeCursor(cursor);
		}
	};
	private BroadcastReceiver nickname_changed_receiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			refresh_channel_users_from_parse ();
		}
	};
	
	private BroadcastReceiver user_kicked_receiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String _channel_id = intent.getStringExtra("channel_id");
			String _guest_id = intent.getStringExtra("guest_id");
			String message = intent.getStringExtra("message");
			String my_guest_id = intent.getStringExtra("my_guest_id");
			
			// user (self) was kicked.
			if (_guest_id.equals(my_guest_id)) {
				Intent mainActivity_user_kicked_intent = new Intent(ChatPage.this, MainActivity.class);
				mainActivity_user_kicked_intent.putExtra("channel_id", _channel_id);
				mainActivity_user_kicked_intent.putExtra("guest_id", _guest_id);
				mainActivity_user_kicked_intent.putExtra("message", message);
				mainActivity_user_kicked_intent.putExtra("user_was_kicked", true);
				startActivity(mainActivity_user_kicked_intent);
				finish();
			} else {
				// room mate was kicked.
				Cursor cursor = database.get_channel_messages(channel_id, message_num_index);
    			adapter.changeCursor(cursor);
    			refresh_channel_users_from_parse();
			}
		}
	};
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// prevent screen shots.
//		getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
		setContentView(R.layout.chat_page);
	
		// prevent keyboard from popping up on page init.
		this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
		
		NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);  
		mNotificationManager.cancel(100);
		CONSTS_PUSH_COMPARISON_CHANNEL_ID_ARRAYLIST.clear();
		CONSTS_PUSH_EVENTS_ARRAYLIST.clear();
		
		intent = getIntent();
		channel_id = intent.getStringExtra("channel_id");
		shared_text = intent.getStringExtra("shared_text");
		database = new DBHandler(this);
		
		if (database.is_channel_secured(channel_id)) {
			getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
		}
		
		channel_name = database.get_channel_name(channel_id);
		nick_in_channel = database.get_nickname_in_channel(channel_id);
		guest_id = database.get_guestId_in_channel(channel_id);
		
		actionBar = getActionBar();
		actionBar.setDisplayHomeAsUpEnabled(true);
		actionBar.setTitle(channel_name);
		actionBar.setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.MyBackground_orange_red)));
		actionBar.setSubtitle("Nickname: " + database.get_nickname_in_channel(channel_id));
		actionBar.setIcon(R.drawable.icon);
		
		mEditEmojicon = (EmojiconEditText) findViewById(R.id.editEmojicon);
		wallpaper_imageView = (ImageView)findViewById(R.id.chatPage_wallpaper_imageView);
		
		try {
			wallpaper_imageView.post(new Runnable() {
				@Override
				public void run() {
					// set wallpaper.
					Bitmap bm = BitmapFactory.decodeFile(Environment.getExternalStorageDirectory().toString() + "/roomz_wallpapers/" + database.get_wallpaper_value(channel_id));
					wallpaper_imageView.setImageBitmap(bm);
				}
			});
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		ListView listView = (ListView) findViewById(R.id.chat_ListView);
		registerForContextMenu(listView);
		
		Cursor cursor = database.get_channel_messages(channel_id, message_num_index);
		adapter = new ChatCursorAdapter(this, cursor);
		
		Button show_earlier_messages_button = new Button(ChatPage.this);
		show_earlier_messages_button.setText("Show earlier messages");
		show_earlier_messages_button.setTextColor(getResources().getColor(R.color.White));
		show_earlier_messages_button.setBackgroundColor(getResources().getColor(R.color.Black));
		show_earlier_messages_button.setAlpha(0.45f);
		// show "show_earlier_messages_button" if the cursor is full (means there are probably more messages in the database).
		try {
			if (cursor.getCount() >= 100) {
				listView.addHeaderView(show_earlier_messages_button);
			}
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		listView.setAdapter(adapter);
		
		show_earlier_messages_button.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				message_num_index++;
				Cursor cursor = database.get_channel_messages(channel_id, message_num_index);
				adapter.swapCursor(cursor);
			}
		});
		
		listView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            	// welcome message.
                if (position == 0) {
                	AlertDialog.Builder builder = new AlertDialog.Builder(ChatPage.this);
        			builder.setIcon(android.R.drawable.ic_menu_info_details);
        			builder.setTitle(channel_id);
        			builder.setMessage("This room's ID is "+channel_id+".\n\nPeople can use this ID to join your room.\n");
        			builder.setPositiveButton("Invite", new DialogInterface.OnClickListener() {
        				@Override
        				public void onClick(DialogInterface dialog, int which) {
        					Intent share_intent = new Intent(Intent.ACTION_SEND);
        					share_intent.setType("text/plain");
        					share_intent.putExtra(Intent.EXTRA_TEXT, "Join my room! http://roomz.mobi/"+ channel_id);
        					String title = "Select an app to invite someone";
        					// Create intent to show chooser
        					Intent chooser = Intent.createChooser(share_intent, title);
        					// Verify the intent will resolve to at least one activity
        					if (chooser.resolveActivity(getPackageManager()) != null) {
        					    startActivity(chooser);
        					} else {
        						startActivity(share_intent);
        					}
        				}
        			});
        			builder.setNeutralButton("Copy", new DialogInterface.OnClickListener() {
        				@SuppressLint("NewApi")
        				@Override
        				public void onClick(DialogInterface dialog, int which) {
        					int sdk = android.os.Build.VERSION.SDK_INT;
        					if(sdk < android.os.Build.VERSION_CODES.HONEYCOMB) {
        					    android.text.ClipboardManager clipboard = (android.text.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        					    clipboard.setText(channel_id);
        					} else {
        					    android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE); 
        					    android.content.ClipData clip = android.content.ClipData.newPlainText("label",channel_id);
        					    clipboard.setPrimaryClip(clip);
        					}
        					Toast.makeText(getApplicationContext(), "copied to clipboard", Toast.LENGTH_SHORT).show();
        				}
        			});
        			builder.show();
				}
            }
        });
		
		try {
			int selection = adapter.getCount() - database.get_CHANNELS_TABLE_msg_unseen_counter_column(channel_id);
			listView.setSelection(selection);
			
		} catch (NotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		database.reset_CHANNELS_TABLE_msg_unseen_counter_column(channel_id);
		refresh_channel_users_from_parse();
		refresh_room_name_and_security_from_parse();
		user_input_actv = (AutoCompleteTextView) findViewById(R.id.chatPage_user_input_actv);
		addAutoCompleteUsers();
		user_input_actv.addTextChangedListener(new TextWatcher() {
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				if (s.toString().endsWith("@")) {
					user_input_actv.showDropDown();
				}
			}
			
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
			}
			
			@Override
			public void afterTextChanged(Editable s) {
			}
		});
		user_input_actv.setOnItemClickListener(new OnItemClickListener() {
		    @Override
		    public void onItemClick (AdapterView<?> parent, View view, int position, long id) {
		    	String channel_users_string = channel_users_set.toString();
		    	String[] separated = channel_users_string.split(",");
		    	
		    	String selected_user = separated[position].trim().substring(1);
		    	if (selected_user.startsWith("@@")) {
		    		selected_user = selected_user.substring(2);
				}
		    	if (selected_user.endsWith("]")) {
		    		selected_user = selected_user.substring(0, selected_user.length()-1);
				}
		    	if (selected_user.endsWith("[")) {
		    		selected_user = selected_user.substring(0, selected_user.length()-1);
				}
		    	if (selected_user.startsWith("[")) {
		    		selected_user = selected_user.substring(1);
				}
		    	if (selected_user.startsWith("@")) {
		    		selected_user = selected_user.substring(1);
				}
		    	mEditEmojicon.append(selected_user + " ");
		    }
		});
		
		
      //  mTxtEmojicon = (EmojiconTextView) findViewById(R.id.txtEmojicon);
        mEditEmojicon.addTextChangedListener(new TextWatcherAdapter() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
    //            mTxtEmojicon.setText(s);
            	String str = s.toString();
            	if (str.endsWith("@")) {
            		user_input_actv.showDropDown();
				} else {
					if (str.equals("")) {
	            		user_input_actv.dismissDropDown();
					} else {
						if (str.endsWith(" ")) {
		            		user_input_actv.dismissDropDown();
						}
					}
				}
            	
            	
            }
        });
    
        setEmojiconFragment(false);
        
        if (shared_text != null) {
        	if (!shared_text.equals("null")) {
        		mEditEmojicon.setText(shared_text);	
			}
        	
		}
        
        Button send_button = (Button) findViewById(R.id.chatPage_send_button);
		send_button.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				send_message();
			}
		});
		
        final FrameLayout emojicons = (FrameLayout)findViewById(R.id.emojicons);
		final ImageButton smily_imageButton = (ImageButton)findViewById(R.id.chatPage_smily_imageButton);
		smily_imageButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
				if (emojicons.getVisibility() == View.GONE) {
					smily_imageButton.setImageResource(R.drawable.chatpage_keyboard_smily);
					imm.hideSoftInputFromWindow(mEditEmojicon.getWindowToken(), 0);
					emojicons.setVisibility(1);
				} else {
					smily_imageButton.setImageResource(R.drawable.chatpage_smiley);
					emojicons.setVisibility(View.GONE);
					imm.showSoftInput(mEditEmojicon, 0);
				}
			}
		});
		
		mEditEmojicon.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				smily_imageButton.setImageResource(R.drawable.chatpage_smiley);
				emojicons.setVisibility(View.GONE);
			};
		});
		// if user tried to join a room he's already in.
		Boolean already_joined = intent.getBooleanExtra("already_joined", false);
		if (already_joined) {
			ContextThemeWrapper ctw = new ContextThemeWrapper(ChatPage.this, R.style.Theme_Base);
			final AlertDialog.Builder builder = new AlertDialog.Builder(ctw);
			builder.setTitle("Already joined");
			builder.setMessage("you've already joined this room.\n\nYour nickname is: " + nick_in_channel);
			builder.setNeutralButton("Okay", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
				}
			});
			builder.show();
		}
	}
	
	private void setEmojiconFragment(boolean useSystemDefault) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.emojicons, EmojiconsFragment.newInstance(useSystemDefault))
                .commit();
    }

    @Override
    public void onEmojiconClicked(Emojicon emojicon) {
        EmojiconsFragment.input(mEditEmojicon, emojicon);
    }

    @Override
    public void onEmojiconBackspaceClicked(View v) {
        EmojiconsFragment.backspace(mEditEmojicon);
    }
    
	@Override
	protected void onResume() {
		super.onResume();
		registerReceiver(my_msg_receiver, new IntentFilter(PUSH_INNERRECEIVER_INTENT_MSG_RECEIVED));
		registerReceiver(room_name_changed_receiver, new IntentFilter(PUSH_INNERRECEIVER_INTENT_ROOM_NAME_CHANGED));
		registerReceiver(nickname_changed_receiver, new IntentFilter(PUSH_INNERRECEIVER_INTENT_NICKNAME_CHANGED));
		registerReceiver(user_kicked_receiver, new IntentFilter(PUSH_INNERRECEIVER_INTENT_USER_KICKED));
		database.generate_notification_update(channel_id, 0);
		update_last_seen_in_parse_online();
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		unregisterReceiver(my_msg_receiver);
		unregisterReceiver(room_name_changed_receiver);
		unregisterReceiver(nickname_changed_receiver);
		unregisterReceiver(user_kicked_receiver);
		database.generate_notification_update(channel_id, 1);
		database.play_notification_sound_update(channel_id, 1);
		database.update_CHANNELS_TABLE_last_seen_column(channel_id);
		update_last_seen_in_parse();
	}
	
	@Override
	protected void onRestart() {
		super.onRestart();
		try {
			// set wallpaper.
			Bitmap bm = BitmapFactory.decodeFile(Environment.getExternalStorageDirectory().toString() + "/roomz_wallpapers/" + database.get_wallpaper_value(channel_id));
			wallpaper_imageView.setImageBitmap(bm);
		} catch (Exception e) {
			e.printStackTrace();
		}
		channel_name = database.get_channel_name(channel_id);
		actionBar.setTitle(channel_name);
		actionBar.setSubtitle("Nickname: " + database.get_nickname_in_channel(channel_id));
		Cursor cursor = database.get_channel_messages(channel_id, message_num_index);
		adapter.changeCursor(cursor);
		try {
			if (database.is_channel_secured(channel_id)) {
				getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);  
		mNotificationManager.cancel(100);
		CONSTS_PUSH_COMPARISON_CHANNEL_ID_ARRAYLIST.clear();
		CONSTS_PUSH_EVENTS_ARRAYLIST.clear();
		
	}
	
	@Override
	public void onBackPressed() {
		super.onBackPressed();
		NavUtils.navigateUpFromSameTask(this);
	//	finish();
	}
	
	public void addAutoCompleteUsers() {
        // Login AutoCompleteTextView
        String[] users_in_channels = database.get_nicknames_in_channel_ForActv(channel_id);
        channel_users_set = new HashSet<String>();
        for (int i = 0; i < users_in_channels.length; i++) {
        	if (!users_in_channels[i].equals("@" + nick_in_channel)) {
        		channel_users_set.add(users_in_channels[i]);
			}
		}
        user_input_actv.setAdapter(new ArrayAdapter<String>(ChatPage.this, android.R.layout.simple_dropdown_item_1line, new ArrayList<String>(channel_users_set)));
    }
	
	// channel_users_table (_id, channel_id, nickname, guest_id); for @ tagging.
	private void refresh_channel_users_from_parse () {
		if (isNetworkConnected()) {
			ParseObject	roomPointer = ParseObject.createWithoutData("Room", channel_id);
			ParseQuery<ParseObject> get_users_query = ParseQuery.getQuery("Guest");
			get_users_query.whereEqualTo("room", roomPointer);
			get_users_query.findInBackground(new FindCallback<ParseObject>() {
				@Override
				public void done(List<ParseObject> objects, ParseException e) {
					if (e == null) {
						database.delete_users_table(channel_id);
						for (int i = 0; i < objects.size(); i++) {
	                        Object object = objects.get(i);
	                        String nickname = ((ParseObject) object).getString("nickname");
	                        int admin = ((ParseObject) object).getInt("admin");
	                        long last_seen = ((ParseObject) object).getLong("last_seen");
	                        String guest_id = ((ParseObject) object).getObjectId();
	                        database.addChannelUser(channel_id, nickname, guest_id, admin, last_seen);
	                   }
						// refresh cursor.
                        Cursor cursor = database.get_channel_messages(channel_id, message_num_index);
            			adapter.changeCursor(cursor);
            			addAutoCompleteUsers();
            			check_if_user_exists();
					}
				}
			});
		}
	}
	
	private void check_if_user_exists() {
		// if user doesn't exist in the room - means user was -probably- kicked.
		// this method is a workaround to fixing a problem where a user doesn't receive the kick push message sometimes.
		if (!database.user_exists_in_channel(guest_id, channel_id)) {
			// kick user.
			Intent mainActivity_user_kicked_intent = new Intent(ChatPage.this, MainActivity.class);
			mainActivity_user_kicked_intent.putExtra("channel_id", channel_id);
			mainActivity_user_kicked_intent.putExtra("guest_id", guest_id);
			mainActivity_user_kicked_intent.putExtra("message", "You are no longer in this room.");
			mainActivity_user_kicked_intent.putExtra("user_was_kicked", true);
			startActivity(mainActivity_user_kicked_intent);
			finish();
		}
	}
	
	private void refresh_room_name_and_security_from_parse () {
		if (isNetworkConnected()) {
			ParseQuery<ParseObject> get_room_name_and_secured_query = ParseQuery.getQuery("Room");
			get_room_name_and_secured_query.whereEqualTo("objectId", channel_id);
			get_room_name_and_secured_query.getFirstInBackground(new GetCallback<ParseObject>() {
				@Override
				public void done(ParseObject object, ParseException e) {
					if (e == null) {
						String new_room_name = object.getString("name");
						database.change_room_name(channel_id, new_room_name);
						
						int secured = object.getInt("secured");
						if (secured == 1) {
							database.update_secured_column_SECURE(channel_id);
							// prevent screen shots.
							getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
						} else {
							database.update_secured_column_UNSECURE(channel_id);
						}
						
					}
				}
			});
		}
	}
	
	private void send_message() {
		final String nick_in_channel = database.get_nickname_in_channel(channel_id);
		final String message = mEditEmojicon.getText().toString().trim();
		final long time = System.currentTimeMillis();
		
		if (message.length() > 0) {
			// add message to local database.
			if (database.first_message_of_day(channel_id)) {
				database.addMessageToChannel(channel_id, PUSH_ACTION_SYSTEM_MESSAGE, DATETIME_FORMATTER_DAY.format(new Date(time)), PUSH_ACTION_SYSTEM_MESSAGE, 1, time-1);
			}
			final long message_id = database.addMessageToChannel(channel_id, nick_in_channel, message, guest_id, 0, time);
			// send push to channel.
			JSONObject data = new JSONObject();
			try {
				data.put("action", PUSH_ACTION_ROOMMATE_MESSAGE);
				data.put("channel_name", channel_name);
				data.put("guest_id", guest_id);
				data.put("nickname", nick_in_channel);
				data.put("message", message);
				data.put("message_time", time);
			} catch (JSONException e1) {
				e1.printStackTrace();
			}
			ParsePush push = new ParsePush();
			push.setChannel(channel_id);
			push.setData(data);
			push.sendInBackground(new SendCallback() {
				@Override
				public void done(ParseException e) {
					if (e == null) {
						database.message_sent_update(message_id, 1);
						Cursor cursor = database.get_channel_messages(channel_id, message_num_index);
						adapter.changeCursor(cursor);
					} else {
						database.message_sent_update(message_id, 2);
						Toast.makeText(getApplicationContext(), "Error sending message - check connection.", Toast.LENGTH_SHORT).show();
						Cursor cursor = database.get_channel_messages(channel_id, message_num_index);
						adapter.changeCursor(cursor);
						}
				}
			});
			
	/*		
			ParseObject	guestPointer = ParseObject.createWithoutData("Guest", database.get_guestId_in_channel(channel_id));
			ParseObject	roomPointer = ParseObject.createWithoutData("Room", channel_id);
			ParseObject sendMsgToParse = new ParseObject("Message");
			sendMsgToParse.put("text", message);
			sendMsgToParse.put("guest", guestPointer);
			sendMsgToParse.put("room", roomPointer);
			sendMsgToParse.saveInBackground(new SaveCallback() {
				@Override
				public void done(ParseException e) {
					if (e == null) {
						// send push to channel.
						JSONObject data = new JSONObject();
						try {
							data.put("action", PUSH_ACTION_ROOMMATE_MESSAGE);
							data.put("channel_name", channel_name);
							data.put("guest_id", guest_id);
							data.put("nickname", nick_in_channel);
							data.put("message", message);
							data.put("message_time", time);
						} catch (JSONException e1) {
							e1.printStackTrace();
						}
						ParsePush push = new ParsePush();
						push.setChannel(channel_id);
						push.setData(data);
						push.sendInBackground(new SendCallback() {
							@Override
							public void done(ParseException e) {
								if (e == null) {
									database.message_sent_update(message_id, 1);
		//							Toast.makeText(getApplicationContext(), "push sent", Toast.LENGTH_SHORT).show();
									Cursor cursor = database.get_channel_messages(channel_id, message_num_index);
									adapter.changeCursor(cursor);
								}
							}
						});
					} else {
						database.message_sent_update(message_id, 2);
						Toast.makeText(getApplicationContext(), "Error sending message - check connection.", Toast.LENGTH_SHORT).show();
						Cursor cursor = database.get_channel_messages(channel_id, message_num_index);
						adapter.changeCursor(cursor);
						}
				}
			});
*/
			mEditEmojicon.setText("");
			
			// refresh list.
			Cursor cursor = database.get_channel_messages(channel_id, message_num_index);
			adapter.changeCursor(cursor);
		}
	}
	
	private class ChatCursorAdapter extends CursorAdapter implements consts_interface {
		final LayoutInflater inflator;
		
		public ChatCursorAdapter(Context context, Cursor c) {
			super(context, c, true);
			inflator = LayoutInflater.from(context);
		}

		@Override
		public void bindView(View view, Context context, Cursor cursor) {
			Chat_item_viewHolder viewHolder = (Chat_item_viewHolder) view.getTag();
			
			viewHolder.setId(cursor.getLong(cursor.getColumnIndex(CHANNEL_ID)));
			viewHolder.setTimestamp(cursor.getLong(cursor.getColumnIndex(TIME)));
			String guestid = cursor.getString(cursor.getColumnIndex(GUEST_ID));
			String message = cursor.getString(cursor.getColumnIndex(MESSAGE));
			if (message.contains("@" + nick_in_channel + " ")) {
				SpannableStringBuilder sb_message1 = new SpannableStringBuilder(message);
				String user_mentioned_nickname = "@" + nick_in_channel;
				// make mention noticeable.
				StyleSpan iss = new StyleSpan(android.graphics.Typeface.BOLD);
				ForegroundColorSpan iss1 = new ForegroundColorSpan(Color.RED);
				sb_message1.setSpan(iss, message.indexOf(user_mentioned_nickname), message.indexOf(user_mentioned_nickname)+user_mentioned_nickname.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);
				sb_message1.setSpan(iss1, message.indexOf(user_mentioned_nickname), message.indexOf(user_mentioned_nickname)+user_mentioned_nickname.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE); // make mentioning red.
				
				viewHolder.setMessage_spannable_textView(sb_message1);
			} else {
				if (message.endsWith("@" + nick_in_channel)) {
					SpannableStringBuilder sb_message1 = new SpannableStringBuilder(message);
					String user_mentioned_nickname = "@" + nick_in_channel;
					// make mention noticeable.
					StyleSpan iss = new StyleSpan(android.graphics.Typeface.BOLD);
					ForegroundColorSpan iss1 = new ForegroundColorSpan(Color.RED);
					sb_message1.setSpan(iss, message.indexOf(user_mentioned_nickname), user_mentioned_nickname.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE); // make mentioning Bold.
					sb_message1.setSpan(iss1, message.indexOf(user_mentioned_nickname), user_mentioned_nickname.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE); // make mentioning red.
					
					viewHolder.setMessage_spannable_textView(sb_message1);
				} else {
					viewHolder.setMessage_textView(message);					
				}
			}
			
				// user message.
			if (guestid.equals(guest_id)) {
				viewHolder.getNickname_textView().setVisibility(View.GONE);
				viewHolder.getNickname_underline().setVisibility(View.GONE);
				viewHolder.getTime_textView().setVisibility(View.VISIBLE);
				LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		        params.gravity=Gravity.RIGHT;
				viewHolder.getContainer_layout().setLayoutParams(params);
				viewHolder.getContainer_layout().setBackgroundResource(R.drawable.usrbg);
				viewHolder.getMessage_textView().setTextColor(getResources().getColor(R.color.White));
				viewHolder.getMessage_textView().setPadding(0, 0, 0, 0);
				
				int is_message_sent = cursor.getInt(cursor.getColumnIndex(SENT));
				switch (is_message_sent) {
				case 0:
					viewHolder.setTimeStamp_sending();
					break;
				case 1:
					viewHolder.setTimestamp(cursor.getLong(cursor.getColumnIndex(TIME)));
					break;
				case 2:
					viewHolder.setTimeStamp_failed();
					break;
				default:
					break;
				}
				
			} else {
				
				// system message.
				if (guestid.equals(PUSH_ACTION_SYSTEM_MESSAGE)) {
					viewHolder.getNickname_textView().setVisibility(View.GONE);
					viewHolder.getNickname_underline().setVisibility(View.GONE);
					viewHolder.getTime_textView().setVisibility(View.GONE);
					LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
			        params.gravity=Gravity.CENTER_HORIZONTAL;
					viewHolder.getContainer_layout().setLayoutParams(params);
					viewHolder.getContainer_layout().setBackgroundResource(R.drawable.abc_menu_dropdown_panel_holo_dark);
					viewHolder.getMessage_textView().setTextColor(getResources().getColor(R.color.White));
					viewHolder.getMessage_textView().setPadding(25, 25, 25, 25);
					
					// room mate message.
				} else {
					viewHolder.getNickname_textView().setVisibility(View.VISIBLE);
					viewHolder.getNickname_underline().setVisibility(View.VISIBLE);
					viewHolder.getTime_textView().setVisibility(View.VISIBLE);
					LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
			        params.gravity=Gravity.LEFT;
					viewHolder.getContainer_layout().setLayoutParams(params);
					viewHolder.getContainer_layout().setBackgroundResource(R.drawable.roommatemsgbg);
					viewHolder.setNickname_textView(cursor.getString(cursor.getColumnIndex(NICK_NAME)));
					viewHolder.getNickname_textView().setTextColor(getResources().getColor(R.color.text_roomate_bg_color));
					viewHolder.getMessage_textView().setTextColor(getResources().getColor(R.color.Black));
					viewHolder.getMessage_textView().setPadding(0, 0, 0, 0);
				}
			}
		}

		@Override
		public View newView(Context context, Cursor cursor, ViewGroup viewGroup) {
			View view = inflator.inflate(R.layout.chat_item_layout, null);
			Chat_item_viewHolder viewHolder = new Chat_item_viewHolder(view);
			view.setTag(viewHolder);

			return view;
		}

	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.chatpage_options_menu, menu);
		return true;
	}
	
	//  This is called right before the menu is shown, every time it is shown.
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		MenuItem mute_menu_item = (MenuItem) menu.findItem(R.id.chatPage_menu_mute);
		if (database.is_channel_muted(channel_id)) {
			mute_menu_item.setTitle("Unmute");			
		} else {
			mute_menu_item.setTitle("Mute");
		}
		
		return super.onPrepareOptionsMenu(menu);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo)item.getMenuInfo(); 
		switch (item.getItemId()) {
		case R.id.chatPage_info:
			Intent info_intent = new Intent(ChatPage.this, ChatInfo.class);
			info_intent.putExtra("channel_id", channel_id);
			startActivityForResult(info_intent, 1);
			break;
		case R.id.chatPage_menu_share:
		//	ContextThemeWrapper ctw = new ContextThemeWrapper(ChatPage.this, R.style.Theme_Base);
			AlertDialog.Builder builder = new AlertDialog.Builder(ChatPage.this);
			builder.setIcon(android.R.drawable.ic_menu_info_details);
			builder.setTitle(channel_id);
			builder.setMessage("This room's ID is "+channel_id+".\n\nPeople can use this ID to join your room.\n");
			builder.setPositiveButton("Invite", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					Intent share_intent = new Intent(Intent.ACTION_SEND);
					share_intent.setType("text/plain");
					share_intent.putExtra(Intent.EXTRA_TEXT, "Join my room! http://roomz.mobi/"+ channel_id);
					
					String title = "Select an app to invite someone";
					// Create intent to show chooser
					Intent chooser = Intent.createChooser(share_intent, title);
					// Verify the intent will resolve to at least one activity
					if (chooser.resolveActivity(getPackageManager()) != null) {
					    startActivity(chooser);
					} else {
						startActivity(share_intent);
					}
					
				}
			});
			builder.setNeutralButton("Copy", new DialogInterface.OnClickListener() {
				@SuppressLint("NewApi")
				@Override
				public void onClick(DialogInterface dialog, int which) {
					int sdk = android.os.Build.VERSION.SDK_INT;
					if(sdk < android.os.Build.VERSION_CODES.HONEYCOMB) {
					    android.text.ClipboardManager clipboard = (android.text.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
					    clipboard.setText(channel_id);
					} else {
					    android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE); 
					    android.content.ClipData clip = android.content.ClipData.newPlainText("label",channel_id);
					    clipboard.setPrimaryClip(clip);
					}
					Toast.makeText(getApplicationContext(), "copied to clipboard", Toast.LENGTH_SHORT).show();
				}
			});
			builder.show();
			break;
		case R.id.chatPage_menu_mute:
			if (database.is_channel_muted(channel_id)) {
				database.update_mute_column_UNMUTE(channel_id);
				Toast.makeText(getApplicationContext(), "un-muted", Toast.LENGTH_LONG).show();
			} else {
		//		final CharSequence[] items = {" 8 Hours "," One week "," Forever "," And ever "};
				final CharSequence[] items = {" Forever "};
				new AlertDialog.Builder(ChatPage.this).setTitle("Mute")
				.setIcon(android.R.drawable.ic_lock_silent_mode)
				.setSingleChoiceItems(items, -1, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int item) {
						switch(item) {
						case 0:
							database.update_mute_column_MUTE(channel_id);
							Toast.makeText(getApplicationContext(), "muted", Toast.LENGTH_LONG).show();
			//				Toast.makeText(getApplicationContext(), "not active - only \"forever\" is active", Toast.LENGTH_LONG).show();
							break;
		/*				case 1:
							// Your code when 2nd  option seletced
							Toast.makeText(getApplicationContext(), "not active - only \"forever\" is active", Toast.LENGTH_LONG).show();
							break;
						case 2:
							// Your code when 3rd option seletced
							database.update_mute_column_MUTE(channel_id);
							Toast.makeText(getApplicationContext(), "muted", Toast.LENGTH_LONG).show();
							break;
						case 3:
							// Your code when 4th  option seletced           
							Toast.makeText(getApplicationContext(), "not active - only \"forever\" is active", Toast.LENGTH_LONG).show();
							break;*/
						}
						dialog.dismiss();   
					}
				}).create().show();
			}
			return true;
			
		default:
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		getMenuInflater().inflate(R.menu.chatpage_context_menu, menu);
	}
	
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo)item.getMenuInfo(); 
		switch (item.getItemId()) {
		case R.id.chatPage_copy_to_clipboard_MenuItem:
			String message_to_copy = database.get_message(info.id);
			int sdk = android.os.Build.VERSION.SDK_INT;
			if(sdk < android.os.Build.VERSION_CODES.HONEYCOMB) {
			    android.text.ClipboardManager clipboard = (android.text.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
			    clipboard.setText(message_to_copy);
			} else {
			    android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE); 
			    android.content.ClipData clip = android.content.ClipData.newPlainText("label",message_to_copy);
			    clipboard.setPrimaryClip(clip);
			}
			return true;
		case R.id.chatPage_delete_MenuItem:
			delete_message(info);
			return true;
		}
		return super.onContextItemSelected(item);
	}
	
	public void delete_message (final AdapterContextMenuInfo info) {
		new AlertDialog.Builder(ChatPage.this)
		.setMessage("Delete message?").setPositiveButton("Delete", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				database.delete_message(info.id);
				Cursor cursor = database.get_channel_messages(channel_id, message_num_index);
				adapter.changeCursor(cursor);
			}
		}).setNeutralButton("Cancel", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
		}).create().show();
	}
	
	private void update_last_seen_in_parse_online () {
		ParseObject	guestPointer = ParseObject.createWithoutData("Guest", database.get_guestId_in_channel(channel_id));
		guestPointer.put("last_seen", 0);
		guestPointer.saveInBackground();
	}
	
	private void update_last_seen_in_parse () {
		ParseObject	guestPointer = ParseObject.createWithoutData("Guest", database.get_guestId_in_channel(channel_id));
		guestPointer.put("last_seen", System.currentTimeMillis());
		guestPointer.saveInBackground();
	}
	
	private boolean isNetworkConnected() {
		  ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		  NetworkInfo ni = cm.getActiveNetworkInfo();
		  if (ni == null) {
		   return false;
		  } else {
			  return true;
		  }
	 }
	
}
