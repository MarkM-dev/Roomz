package mobi.roomz;


import java.util.Calendar;

import mobi.roomz.R;
import mobi.roomz.db.consts_interface;
import mobi.roomz.ui.Main_item_viewHolder;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Typeface;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class MyCursorAdapter extends CursorAdapter implements consts_interface {

	final LayoutInflater inflator;

	public MyCursorAdapter(Context context, Cursor c) {
		super(context, c, true);
		inflator = LayoutInflater.from(context);
	}

	@Override
	public void bindView(View view, Context context, Cursor cursor) {
		Main_item_viewHolder viewHolder = (Main_item_viewHolder) view.getTag();

/////////////////////////// Time display ////////////////////////////////////////////
		long message_time = cursor.getLong(cursor.getColumnIndex(TIME));
		Calendar c = Calendar.getInstance();
		c.getTimeInMillis();
		String current_day = String.format("%te",c); // This will give date like 22 February 2012
		String current_month = String.format("%B",c);
		String current_year = String.format("%tY",c);
		
		c.setTimeInMillis(message_time);//set your saved timestamp
		String that_day=String.format("%te",c); //this will convert timestamp into format like 22 February 2012
		String that_month = String.format("%B",c);
		String that_year = String.format("%tY",c);

		if (that_day.equals(current_day) && that_month.equals(current_month) && that_year.equals(current_year)) {
			viewHolder.setTimeStamp_time(cursor.getLong(cursor.getColumnIndex(TIME)));
		} else {
			if (Integer.valueOf(that_day) == (Integer.valueOf(current_day)-1) && that_month.equals(current_month) && that_year.equals(current_year)) {
				viewHolder.setTimeStamp_String("Yesterday");
			} else {
				viewHolder.setTimeStamp_day(cursor.getLong(cursor.getColumnIndex(TIME)));
			}
		}
/////////////////////////// Time display ////////////////////////////////////////////
		
		
		
		viewHolder.setRoomName_textView(cursor.getString(cursor.getColumnIndex(CHANNEL_NAME)));
		
		// if message was sent and user hasn't seen it yet.
		if (cursor.getLong(cursor.getColumnIndex(LAST_SEEN)) < message_time) {
			if (!cursor.getString(cursor.getColumnIndex(MSG_UNREAD_COUNTER)).equals("0")) {
				viewHolder.getLastMsg_textView().setTypeface(null, Typeface.BOLD);
				viewHolder.getLastMsg_textView().setTextColor(context.getResources().getColor(R.color.Black));
				viewHolder.getUnseen_msg_counter_textView().setVisibility(1);
				viewHolder.setUnseen_msg_counter_textView(cursor.getString(cursor.getColumnIndex(MSG_UNREAD_COUNTER)));
			} else {
				viewHolder.getUnseen_msg_counter_textView().setVisibility(View.GONE);
			}
			
		} else {
			// messages seen.
			viewHolder.getLastMsg_textView().setTypeface(null, Typeface.NORMAL);
			viewHolder.getLastMsg_textView().setTextColor(context.getResources().getColor(R.color.Gray));
			viewHolder.getUnseen_msg_counter_textView().setVisibility(View.GONE);
		}
	
		String nickname = cursor.getString(cursor.getColumnIndex(NICK_NAME));
		String message = cursor.getString(cursor.getColumnIndex(MESSAGE));
		// system message.
		if (nickname.equals(PUSH_ACTION_SYSTEM_MESSAGE)) {
			viewHolder.setLastMsg_textView(message + ".");
			
			// users' messages.
		} else {
			viewHolder.setLastMsg_textView(nickname + ": "+ message);
		}
		
		if (cursor.getInt(cursor.getColumnIndex(MUTE)) == 1) {
			viewHolder.set_mute_icon_visible();
		} else {
			viewHolder.set_mute_icon_gone();
		}
		if (cursor.getInt(cursor.getColumnIndex(SECURED)) == 1) {
			viewHolder.set_secured_icon_visible();
		} else {
			viewHolder.set_secured_icon_gone();
		}
		
		
	}

	@Override
	public View newView(Context context, Cursor cursor, ViewGroup viewGroup) {
		View view = inflator.inflate(R.layout.main_item_layout, null);
		Main_item_viewHolder viewHolder = new Main_item_viewHolder(view);
		view.setTag(viewHolder);

		return view;
	}

}
