package mobi.roomz.db;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.text.format.DateUtils;

public class DBHandler implements consts_interface {
	private DBHelper helper;
	private final static DateFormat DATETIME_FORMATTER_DAY = new SimpleDateFormat("EEEE, MMM dd, yyyy");
	public DBHandler(Context con) {
		helper = new DBHelper(con, ROOMZ_DB_NAME, null, DB_VERSION);
	}

	public void hostChannel(String channel_id, String channel_name, String nick_in_channel, String guest_id, String wallpaper, int secured) {
		SQLiteDatabase db = helper.getWritableDatabase();
		try {
			ContentValues values = new ContentValues();
			values.put(CHANNEL_ID, channel_id);
			values.put(CHANNEL_NAME, channel_name);
			values.put(NICK_IN_CHANNEL, nick_in_channel);
			values.put(GUEST_ID, guest_id);
			values.put(WALLPAPER, wallpaper);
			values.put(LAST_SEEN, System.currentTimeMillis());
			values.put(MSG_UNREAD_COUNTER, 0);
			values.put(SECURED, secured);
			values.put(MUTE, 0);
			db.insert(CHANNELS_TABLE, null, values);
			// display a system message before the first message of the day (showing date).
			long time = System.currentTimeMillis();
			
			// room FTE message.
			ContentValues values3 = new ContentValues();
			values3.put(CHANNEL_ID, channel_id);
			values3.put(NICK_NAME, PUSH_ACTION_SYSTEM_MESSAGE);
			values3.put(TIME, time);
			values3.put(MESSAGE, "Your Room ID is " + channel_id + ".\n\nTap me for more info.");
			values3.put(GUEST_ID, PUSH_ACTION_SYSTEM_MESSAGE);
			db.insert(CHANNEL_MESSAGES_TABLE, null, values3);
			

			ContentValues values1 = new ContentValues();
			values1.put(CHANNEL_ID, channel_id);
			values1.put(GENERATE_NOTIFICATION, 0);
			values1.put(PLAY_NOTIFICATION, 0);
			db.insert(NOTIFICATION_TABLE, null, values1);
			
		} catch (SQLiteException e) {
			e.printStackTrace();
		} finally {
			if (db.isOpen()) {
				db.close();
			}
		}
	}
	
	public void joinChannel(String channel_id, String channel_name, String nick_in_channel, String guest_id, String wallpaper, int secured) {
		SQLiteDatabase db = helper.getWritableDatabase();
		try {
			ContentValues values = new ContentValues();
			values.put(CHANNEL_ID, channel_id);
			values.put(CHANNEL_NAME, channel_name);
			values.put(NICK_IN_CHANNEL, nick_in_channel);
			values.put(GUEST_ID, guest_id);
			values.put(WALLPAPER, wallpaper);
			values.put(LAST_SEEN, System.currentTimeMillis());
			values.put(MSG_UNREAD_COUNTER, 0);
			values.put(SECURED, secured);
			values.put(MUTE, 0);
			db.insert(CHANNELS_TABLE, null, values);
			
			long time = System.currentTimeMillis();

			// room creation message.
			ContentValues values2 = new ContentValues();
			values2.put(CHANNEL_ID, channel_id);
			values2.put(NICK_NAME, PUSH_ACTION_SYSTEM_MESSAGE);
			values2.put(TIME, time);
			values2.put(MESSAGE, "This Room's ID is " + channel_id + ".\n\nTap me for more info.");
			values2.put(GUEST_ID, PUSH_ACTION_SYSTEM_MESSAGE);
			db.insert(CHANNEL_MESSAGES_TABLE, null, values2);
			
			ContentValues values1 = new ContentValues();
			values1.put(CHANNEL_ID, channel_id);
			values1.put(GENERATE_NOTIFICATION, 0);
			values1.put(PLAY_NOTIFICATION, 0);
			db.insert(NOTIFICATION_TABLE, null, values1);
			
		} catch (SQLiteException e) {
			e.printStackTrace();
		} finally {
			if (db.isOpen()) {
				db.close();
			}
		}
	}
	
	public void delete_message (long id) {
		String _id = String.valueOf(id);
		SQLiteDatabase db = helper.getWritableDatabase();
		try {
			db.delete(CHANNEL_MESSAGES_TABLE, _ID+"=?", new String [] {_id});
		} catch (SQLiteException e) {
			e.getCause();
		}finally {
			if (db.isOpen()) {
				db.close();
			}
		}
	}
	
	// channel_users_table (_id, channel_id, nickname, guest_id);
	public void addChannelUser(String channel_id, String nickname, String guest_id, int admin, long last_seen) {
		SQLiteDatabase db = helper.getWritableDatabase();
		try {
			ContentValues values = new ContentValues();
			values.put(CHANNEL_ID, channel_id);
			values.put(NICK_NAME, nickname);
			values.put(GUEST_ID, guest_id);
			values.put(ADMIN, admin);
			values.put(LAST_SEEN, last_seen);
			db.insert(CHANNEL_USERS_TABLE, null, values);
		} catch (SQLiteException e) {
			e.printStackTrace();
		} finally {
			if (db.isOpen()) {
				db.close();
			}
		}
	}
	
	public boolean check_if_joined_room (String channel_id) {
		SQLiteDatabase db = helper.getReadableDatabase();
		Cursor cursor = db.query(DBHelper.CHANNELS_TABLE, new String[] {CHANNEL_NAME}, CHANNEL_ID+"='"+channel_id+"'", null, null, null, null);
		if (cursor.moveToFirst()) {
			return true;
		} else {
			return false;
		}
	}
	
	public boolean is_admin (String guest_id) {
		SQLiteDatabase db = helper.getReadableDatabase();
		Cursor cursor = db.query(DBHelper.CHANNEL_USERS_TABLE, new String[] {ADMIN}, GUEST_ID+"='"+guest_id+"'", null, null, null, null);
		try {
			cursor.moveToFirst();
			if (cursor.getInt(0) == 1) {
				return true;
			} else {
				return false;
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
	}
	
	public boolean user_exists_in_channel (String guest_id, String channel_id) {
		SQLiteDatabase db = helper.getReadableDatabase();
		Cursor cursor = db.query(DBHelper.CHANNEL_USERS_TABLE, new String[] {NICK_NAME}, GUEST_ID+"='"+guest_id+"' AND " + CHANNEL_ID+"='"+channel_id+"'", null, null, null, null);
		if (cursor.moveToFirst()) {
			return true;
		} else {
			return false;
		}
	}
	
	public boolean check_if_new_nickname_is_same (String channel_id, String new_nickname) {
		String nickname;
		SQLiteDatabase db = helper.getReadableDatabase();
		Cursor cursor = db.query(DBHelper.CHANNELS_TABLE, new String[] {NICK_IN_CHANNEL}, CHANNEL_ID+"='"+channel_id+"'", null, null, null, null);
		if (cursor.moveToFirst()) {
			nickname = cursor.getString(0);
			if (new_nickname.equals(nickname)) {
				return true;
			} else {
				return false;
			}
		} else {
			return false;
		}
	}
	
	public boolean check_if_nickname_exists_in_channel (String channel_id, String new_nickname) {
		String pulled_channel_id;
		SQLiteDatabase db = helper.getReadableDatabase();
		Cursor cursor = db.query(DBHelper.CHANNEL_USERS_TABLE, new String[] {CHANNEL_ID}, NICK_NAME+"='"+new_nickname+"' COLLATE NOCASE ", null, null, null, null);
		if (cursor.moveToFirst()) {
			pulled_channel_id = cursor.getString(0);
			if (pulled_channel_id.equals(channel_id)) {
				return true;
			} else {
				return false;
			}
		} else {
			return false;
		}
	}
	
	/*public boolean check_if_nickname_exists_in_channel (String channel_id, String new_nickname) {
		String _channel_id;
		SQLiteDatabase db = helper.getReadableDatabase();
		Cursor cursor = db.query(DBHelper.CHANNEL_USERS_TABLE, new String[] {channel_id}, NICK_NAME+"='"+new_nickname+"'", null, null, null, null);
		if (cursor.moveToFirst()) {
			_channel_id = cursor.getString(0);
			if (_channel_id.equals(channel_id)) {
				return true;
			} else {
				return false;
			}
		} else {
			return false;
		}
	}*/
	
	public void delete_users_table (String channel_id) {
		SQLiteDatabase db = helper.getWritableDatabase();
		try {
			db.delete(CHANNEL_USERS_TABLE, CHANNEL_ID+"=?", new String [] {channel_id});
		} catch (SQLiteException e) {
			e.getCause();
		}finally {
			if (db.isOpen()) {
				db.close();
			}
		}
	}
	
	public void clear_chat_messages (String channel_id) {
		SQLiteDatabase db = helper.getWritableDatabase();
		try {
			db.delete(CHANNEL_MESSAGES_TABLE, CHANNEL_ID+"=?", new String [] {channel_id});
			// room creation message.
			
			long time = System.currentTimeMillis();
			
			ContentValues values2 = new ContentValues();
			values2.put(CHANNEL_ID, channel_id);
			values2.put(NICK_NAME, PUSH_ACTION_SYSTEM_MESSAGE);
			values2.put(TIME, time);
			values2.put(MESSAGE, "This Room's ID is " + channel_id + ".\n\nTap me for more info.");
			values2.put(GUEST_ID, PUSH_ACTION_SYSTEM_MESSAGE);
			db.insert(CHANNEL_MESSAGES_TABLE, null, values2);
			
		} catch (SQLiteException e) {
			e.getCause();
		}finally {
			if (db.isOpen()) {
				db.close();
			}
		}
	}
	
	public void clear_chat_messages_by_admin (String channel_id) {
		SQLiteDatabase db = helper.getWritableDatabase();
		try {
			db.delete(CHANNEL_MESSAGES_TABLE, CHANNEL_ID+"=?", new String [] {channel_id});
			// room creation message.
			
			long time = System.currentTimeMillis();
			
			ContentValues values2 = new ContentValues();
			values2.put(CHANNEL_ID, channel_id);
			values2.put(NICK_NAME, PUSH_ACTION_SYSTEM_MESSAGE);
			values2.put(TIME, time);
			values2.put(MESSAGE, "This Room's ID is " + channel_id + ".\n\nTap me for more info.");
			values2.put(GUEST_ID, PUSH_ACTION_SYSTEM_MESSAGE);
			db.insert(CHANNEL_MESSAGES_TABLE, null, values2);
			
			ContentValues values3 = new ContentValues();
			values3.put(CHANNEL_ID, channel_id);
			values3.put(NICK_NAME, PUSH_ACTION_SYSTEM_MESSAGE);
			values3.put(TIME, time+1);
			values3.put(MESSAGE, "Messages were cleared by the host");
			values3.put(GUEST_ID, PUSH_ACTION_SYSTEM_MESSAGE);
			db.insert(CHANNEL_MESSAGES_TABLE, null, values3);
			
			
		} catch (SQLiteException e) {
			e.getCause();
		}finally {
			if (db.isOpen()) {
				db.close();
			}
		}
	}
	
	public void leave_room (String channel_id) {
		SQLiteDatabase db = helper.getWritableDatabase();
		try {
			db.delete(CHANNELS_TABLE, CHANNEL_ID+"=?", new String [] {channel_id});
			db.delete(CHANNEL_MESSAGES_TABLE, CHANNEL_ID+"=?", new String [] {channel_id});
			db.delete(CHANNEL_USERS_TABLE, CHANNEL_ID+"=?", new String [] {channel_id});
		} catch (SQLiteException e) {
			e.getCause();
		}finally {
			if (db.isOpen()) {
				db.close();
			}
		}
	}
	
	public long addMessageToChannel(String channel_id, String nickname, String message, String guest_id, int sent, long message_time_in_miliseconds) {
		SQLiteDatabase db = helper.getWritableDatabase();
		long id = -1;
		try {
			ContentValues values = new ContentValues();
			values.put(CHANNEL_ID, channel_id);
			values.put(NICK_NAME, nickname);
			values.put(TIME, message_time_in_miliseconds);
			values.put(MESSAGE, message);
			values.put(GUEST_ID, guest_id);
			values.put(SENT, 0);
			id = db.insert(CHANNEL_MESSAGES_TABLE, null, values);
			return id;
		} catch (SQLiteException e) {
			e.printStackTrace();
		} finally {
			if (db.isOpen()) {
				db.close();
			}
		}
		return id;
	}
	
	/*
	 * set the "sent" status of the message in the database.
	 * 0 - not sent.
	 * 1 - sent.
	 * 2 - failed.
	 */
	public void message_sent_update (long id, int sent) {
		String _id = String.valueOf(id);
		SQLiteDatabase db = helper.getWritableDatabase();
		try {
			ContentValues values = new ContentValues();
			values.put(SENT, sent);
			db.update(CHANNEL_MESSAGES_TABLE, values, "_id=?", new String [] {_id});
		} catch (SQLiteException e) {
			e.printStackTrace();
		} finally {
			if (db.isOpen()) {
				db.close();
			}
		}
	}
	
	// 1 =  play.
	// 0 = don't play.
	public void generate_notification_update (String channel_id, int num) {
		SQLiteDatabase db = helper.getWritableDatabase();
		try {
			ContentValues values = new ContentValues();
			values.put(GENERATE_NOTIFICATION, num);
			db.update(NOTIFICATION_TABLE, values, ""+CHANNEL_ID+"=?", new String [] {channel_id});
		} catch (SQLiteException e) {
			e.printStackTrace();
		} finally {
			if (db.isOpen()) {
				db.close();
			}
		}
	}
	
	// 1 =  play.
		// 0 = don't play.
	public Integer can_generate_notification (String channel_id) {
		SQLiteDatabase db = helper.getReadableDatabase();
		Cursor cursor = db.query(NOTIFICATION_TABLE, new String[] {GENERATE_NOTIFICATION}, CHANNEL_ID+"='"+channel_id+"'", null, null, null, null);
		if (cursor.moveToFirst()) {
			return cursor.getInt(0);
		} else {
			return 0;
		}
	}
	
	// 1 =  play.
	// 0 = don't play.
	public void play_notification_sound_update (String channel_id, int num) {
		SQLiteDatabase db = helper.getWritableDatabase();
		try {
			ContentValues values = new ContentValues();
			values.put(PLAY_NOTIFICATION, num);
			db.update(NOTIFICATION_TABLE, values, ""+CHANNEL_ID+"=?", new String [] {channel_id});
		} catch (SQLiteException e) {
			e.printStackTrace();
		} finally {
			if (db.isOpen()) {
				db.close();
			}
		}
	}
	
	// 1 =  play.
	// 0 = don't play.
	public Integer play_notification_sound (String channel_id) {
		SQLiteDatabase db = helper.getReadableDatabase();
		Cursor cursor = db.query(NOTIFICATION_TABLE, new String[] {PLAY_NOTIFICATION}, CHANNEL_ID+"='"+channel_id+"'", null, null, null, null);
		if (cursor.moveToFirst()) {
			return cursor.getInt(0);
		} else {
			return 0;
		}
	}
	
	public Cursor get_all_channels() {
		SQLiteDatabase db = helper.getReadableDatabase();
		Cursor cursor;
		cursor = db.rawQuery("SELECT c."+_ID+", c."+CHANNEL_NAME+", c."+LAST_SEEN+", c."+MSG_UNREAD_COUNTER+", c."+SECURED+", c."+MUTE+", cm."+NICK_NAME+", max(cm."+TIME+") as time, cm."+MESSAGE+" FROM "+CHANNELS_TABLE+" c, "+CHANNEL_MESSAGES_TABLE+" cm where c."+CHANNEL_ID+"=cm."+CHANNEL_ID+" group by c."+CHANNEL_ID+" order by " + TIME + " desc", null);
		return cursor;
	}
	
	public Cursor get_channel_messages(String channel_id, int message_num_index) {
		SQLiteDatabase db = helper.getReadableDatabase();
	//	Cursor cursor;
		//cursor = db.rawQuery("SELECT * FROM "+CHANNEL_MESSAGES_TABLE+" where "+CHANNEL_ID+"='"+channel_id+"' order by "+TIME+" ASC", null);
		 Cursor cursor = db.rawQuery("select * from (select * from "+CHANNEL_MESSAGES_TABLE+" where "+CHANNEL_ID+"='"+channel_id+"' order by "+TIME+" DESC limit "+message_num_index+"00) order by "+_ID+" ASC", null);
		return cursor;
	}

	public String get_nickname_in_channel(String channel_id) {
		SQLiteDatabase db = helper.getReadableDatabase();
		Cursor cursor = db.query(DBHelper.CHANNELS_TABLE, new String[] {NICK_IN_CHANNEL}, CHANNEL_ID+"='"+channel_id+"'", null, null, null, null);
		if (cursor.moveToFirst()) {
			return cursor.getString(0);
		} else {
			return null;
		}
	}
	
	public String get_wallpaper_value(String channel_id) {
		SQLiteDatabase db = helper.getReadableDatabase();
		Cursor cursor = db.query(DBHelper.CHANNELS_TABLE, new String[] {WALLPAPER}, CHANNEL_ID+"='"+channel_id+"'", null, null, null, null);
		if (cursor.moveToFirst()) {
			return cursor.getString(0);
		} else {
			return null;
		}
	}
	
	public String get_channel_name(String channel_id) {
		SQLiteDatabase db = helper.getReadableDatabase();
		Cursor cursor = db.query(DBHelper.CHANNELS_TABLE, new String[] {CHANNEL_NAME}, CHANNEL_ID+"='"+channel_id+"'", null, null, null, null);
		if (cursor.moveToFirst()) {
			return cursor.getString(0);
		} else {
			return null;
		}
	}
	
	public String get_channel_id (long id) {
		SQLiteDatabase db = helper.getReadableDatabase();
		Cursor cursor = db.query(DBHelper.CHANNELS_TABLE, new String[] {CHANNEL_ID}, _ID+"="+id, null, null, null, null);
		if (cursor.moveToFirst()) {
			return cursor.getString(0);
		} else {
			return null;
		}
	}
	
	public String get_guestId_in_channel(String channel_id) {
		SQLiteDatabase db = helper.getReadableDatabase();
		Cursor cursor = db.query(DBHelper.CHANNELS_TABLE, new String[] {GUEST_ID}, CHANNEL_ID+"='"+channel_id+"'", null, null, null, null);
		if (cursor.moveToFirst()) {
			return cursor.getString(0);
		} else {
			return null;
		}
	}
	
	public String[] get_nicknames_in_channel_ForActv(String channel_id) {
		List<String> nicknames = new ArrayList<String>();
		SQLiteDatabase db = helper.getReadableDatabase();
		Cursor cursor = db.query(DBHelper.CHANNEL_USERS_TABLE, new String[] {NICK_NAME}, CHANNEL_ID+"='"+channel_id+"'", null, null, null, null);
		if (cursor.moveToFirst()) {
			do {
				nicknames.add("@" + cursor.getString(cursor.getColumnIndex(NICK_NAME)));
			} while (cursor.moveToNext());
		}
		return nicknames.toArray(new String[nicknames.size()]);
	}
	
	public void initialize_notification_play_value () {
		SQLiteDatabase db = helper.getWritableDatabase();
		try {
			ContentValues values = new ContentValues();
			values.put(PLAY_NOTIFICATION, 0);
			db.insert(NOTIFICATION_TABLE, null, values);
		} catch (SQLiteException e) {
			e.printStackTrace();
		} finally {
			if (db.isOpen()) {
				db.close();
			}
		}
	}
	
	public void change_nickname_in_channel(String channel_id, String new_nickname) {
		SQLiteDatabase db = helper.getWritableDatabase();
		try {
			ContentValues values = new ContentValues();
			values.put(NICK_IN_CHANNEL, new_nickname);
			db.update(CHANNELS_TABLE, values, CHANNEL_ID+"=?", new String [] {channel_id});
		} catch (SQLiteException e) {
			e.printStackTrace();
		} finally {
			if (db.isOpen()) {
				db.close();
			}
		}
	}
	
	/*
	 * updated when the user was last on chat page (room based).
	 */
	public void update_CHANNELS_TABLE_last_seen_column(String channel_id) {
		SQLiteDatabase db = helper.getWritableDatabase();
		try {
			ContentValues values = new ContentValues();
			values.put(LAST_SEEN, System.currentTimeMillis());
			db.update(CHANNELS_TABLE, values, CHANNEL_ID+"=?", new String [] {channel_id});
		} catch (SQLiteException e) {
			e.printStackTrace();
		} finally {
			if (db.isOpen()) {
				db.close();
			}
		}
	}
	public void update_mute_column_MUTE(String channel_id) {
		SQLiteDatabase db = helper.getWritableDatabase();
		try {
			ContentValues values = new ContentValues();
			values.put(MUTE, 1);
			db.update(CHANNELS_TABLE, values, CHANNEL_ID+"=?", new String [] {channel_id});
		} catch (SQLiteException e) {
			e.printStackTrace();
		} finally {
			if (db.isOpen()) {
				db.close();
			}
		}
	}
	
	public void update_mute_column_UNMUTE(String channel_id) {
		SQLiteDatabase db = helper.getWritableDatabase();
		try {
			ContentValues values = new ContentValues();
			values.put(MUTE, 0);
			db.update(CHANNELS_TABLE, values, CHANNEL_ID+"=?", new String [] {channel_id});
		} catch (SQLiteException e) {
			e.printStackTrace();
		} finally {
			if (db.isOpen()) {
				db.close();
			}
		}
	}
	
	public boolean is_channel_muted (String channel_id) {
		SQLiteDatabase db = helper.getReadableDatabase();
		Cursor cursor = db.query(DBHelper.CHANNELS_TABLE, new String[] {MUTE}, CHANNEL_ID+"='"+channel_id+"'", null, null, null, null);
		try {
			cursor.moveToFirst();
			if (cursor.getInt(0) == 1) {
				return true;
			} else {
				return false;
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
	}
	
	public void update_secured_column_SECURE(String channel_id) {
		SQLiteDatabase db = helper.getWritableDatabase();
		try {
			ContentValues values = new ContentValues();
			values.put(SECURED, 1);
			db.update(CHANNELS_TABLE, values, CHANNEL_ID+"=?", new String [] {channel_id});
		} catch (SQLiteException e) {
			e.printStackTrace();
		} finally {
			if (db.isOpen()) {
				db.close();
			}
		}
	}
	
	public void update_secured_column_UNSECURE(String channel_id) {
		SQLiteDatabase db = helper.getWritableDatabase();
		try {
			ContentValues values = new ContentValues();
			values.put(SECURED, 0);
			db.update(CHANNELS_TABLE, values, CHANNEL_ID+"=?", new String [] {channel_id});
		} catch (SQLiteException e) {
			e.printStackTrace();
		} finally {
			if (db.isOpen()) {
				db.close();
			}
		}
	}
	public boolean is_channel_secured (String channel_id) {
		SQLiteDatabase db = helper.getReadableDatabase();
		Cursor cursor = db.query(DBHelper.CHANNELS_TABLE, new String[] {SECURED}, CHANNEL_ID+"='"+channel_id+"'", null, null, null, null);
		try {
			cursor.moveToFirst();
			if (cursor.getInt(0) == 1) {
				return true;
			} else {
				return false;
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
	}
	
	public void increment_CHANNELS_TABLE_msg_unseen_counter_column(String channel_id) {
		int counter = get_CHANNELS_TABLE_msg_unseen_counter_column(channel_id) + 1;
		SQLiteDatabase db = helper.getWritableDatabase();
		try {
			ContentValues values = new ContentValues();
			values.put(MSG_UNREAD_COUNTER, counter);
			db.update(CHANNELS_TABLE, values, CHANNEL_ID+"=?", new String [] {channel_id});
		} catch (SQLiteException e) {
			e.printStackTrace();
		} finally {
			if (db.isOpen()) {
				db.close();
			}
		}
	}
	
	public int get_CHANNELS_TABLE_msg_unseen_counter_column(String channel_id) {
		SQLiteDatabase db = helper.getReadableDatabase();
		Cursor cursor = db.query(DBHelper.CHANNELS_TABLE, new String[] {MSG_UNREAD_COUNTER}, CHANNEL_ID+"='"+channel_id+"'", null, null, null, null);
		if (cursor.moveToFirst()) {
			return cursor.getInt(0);
		} else {
			return 0;
		}
	}
	
	public void reset_CHANNELS_TABLE_msg_unseen_counter_column(String channel_id) {
		SQLiteDatabase db = helper.getWritableDatabase();
		try {
			ContentValues values = new ContentValues();
			values.put(MSG_UNREAD_COUNTER, 0);
			db.update(CHANNELS_TABLE, values, CHANNEL_ID+"=?", new String [] {channel_id});
		} catch (SQLiteException e) {
			e.printStackTrace();
		} finally {
			if (db.isOpen()) {
				db.close();
			}
		}
	}
	
	public void change_roommate_nickname_in_channel(String guest_id, String new_nickname) {
		SQLiteDatabase db = helper.getWritableDatabase();
		try {
			ContentValues values = new ContentValues();
			values.put(NICK_NAME, new_nickname);
			db.update(CHANNEL_USERS_TABLE, values, GUEST_ID+"=?", new String [] {guest_id});
			
			// change nickname in all messages by room-mate.
			ContentValues values2 = new ContentValues();
			values2.put(NICK_NAME, new_nickname);
			db.update(CHANNEL_MESSAGES_TABLE, values2, GUEST_ID+"=?", new String [] {guest_id});
		} catch (SQLiteException e) {
			e.printStackTrace();
		} finally {
			if (db.isOpen()) {
				db.close();
			}
		}
	}
	
	public void change_room_name(String channel_id, String new_room_name) {
		SQLiteDatabase db = helper.getWritableDatabase();
		try {
			ContentValues values = new ContentValues();
			values.put(CHANNEL_NAME, new_room_name);
			db.update(CHANNELS_TABLE, values, CHANNEL_ID+"=?", new String [] {channel_id});
		} catch (SQLiteException e) {
			e.printStackTrace();
		} finally {
			if (db.isOpen()) {
				db.close();
			}
		}
	}
	// wallpaper value = "name.jpg".
	public void update_wallpaper_value(String channel_id, String new_wallpaper_value) {
		SQLiteDatabase db = helper.getWritableDatabase();
		try {
			ContentValues values = new ContentValues();
			values.put(WALLPAPER, new_wallpaper_value);
			db.update(CHANNELS_TABLE, values, CHANNEL_ID+"=?", new String [] {channel_id});
		} catch (SQLiteException e) {
			e.printStackTrace();
		} finally {
			if (db.isOpen()) {
				db.close();
			}
		}
	}
	
	public String get_message(long _id) {
		SQLiteDatabase db = helper.getReadableDatabase();
		Cursor cursor = db.query(CHANNEL_MESSAGES_TABLE, new String[] {MESSAGE}, _ID+"='"+_id+"'", null, null, null, null);
		if (cursor.moveToFirst()) {
			return cursor.getString(0);
		} else {
			return null;
		}
	}
	
	public String get_nickname(long _id) {
		SQLiteDatabase db = helper.getReadableDatabase();
		Cursor cursor = db.query(CHANNEL_USERS_TABLE, new String[] {NICK_NAME}, _ID+"='"+_id+"'", null, null, null, null);
		if (cursor.moveToFirst()) {
			return cursor.getString(0);
		} else {
			return null;
		}
	}
	
	public String get_guestId(long _id) {
		SQLiteDatabase db = helper.getReadableDatabase();
		Cursor cursor = db.query(CHANNEL_USERS_TABLE, new String[] {GUEST_ID}, _ID+"='"+_id+"'", null, null, null, null);
		if (cursor.moveToFirst()) {
			return cursor.getString(0);
		} else {
			return null;
		}
	}
	
	public Cursor get_all_users_in_channel(String channel_id) {
		SQLiteDatabase db = helper.getReadableDatabase();
		Cursor cursor;
		cursor = db.rawQuery("SELECT * FROM "+CHANNEL_USERS_TABLE+" where "+CHANNEL_ID+"='"+channel_id+"' order by "+NICK_NAME+" asc", null);
		return cursor;
	}
	
	// checks if previous message is from today.
	public boolean first_message_of_day(String channel_id) {
		SQLiteDatabase db = helper.getReadableDatabase();
		Cursor cursor = db.rawQuery("SELECT "+TIME+" FROM "+CHANNEL_MESSAGES_TABLE+" where "+CHANNEL_ID+"='"+channel_id+"' order by "+_ID+" DESC limit 1", null);
		if (cursor.moveToFirst()) {
			long last_message_time = cursor.getLong(0);
			if (DateUtils.isToday(last_message_time)) {
				return false;
			} else {
				return true;
			}
		} else {
			return true;
		}
	}
	
/*	public String get_message_id (String channel_id) {
		String message_id;
		SQLiteDatabase db = helper.getReadableDatabase();
		Cursor cursor = db.query(DBHelper.CHANNEL_MESSAGES_TABLE, new String[] {_ID}, CHANNEL_ID+"="+channel_id, null, null, null, null);
		if (cursor.moveToFirst()) {
			message_id = cursor.getString(0);
			return message_id;
		} else {
			return null;
		}
	}*/
	
	/*
	public Integer is_message_sent (long id) {
		SQLiteDatabase db = helper.getReadableDatabase();
		Cursor cursor = db.query(CHANNEL_MESSAGES_TABLE, new String[] {SENT}, "_id="+id+"", null, null, null, null);
		if (cursor.moveToFirst()) {
			return cursor.getInt(0);
		} else {
			return 0;
		}
	}*/
	
	/*SELECT cm._id, c.channel_name, cm.nickname, max(cm.time), cm.message
	FROM channels_table c, channel_messages_table cm
	where c.channel_id=cm.channel_id
	group by c.channel_id*/
	
	/*SELECT cm._id, c.channel_name, cm.nickname, max(cm.time) as time, cm.message
	FROM channels_table c, channel_messages_table cm
	where c.channel_id=cm.channel_id
	group by c.channel_id*/
}
