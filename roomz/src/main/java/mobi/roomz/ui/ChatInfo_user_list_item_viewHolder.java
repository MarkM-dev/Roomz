package mobi.roomz.ui;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import mobi.roomz.R;
import android.view.View;
import android.widget.TextView;

public class ChatInfo_user_list_item_viewHolder {

	final static DateFormat DATETIME_FORMATTER_DAY = new SimpleDateFormat("EEEE, MMM dd, yyyy HH:mm");
	final static DateFormat DATETIME_FORMATTER_TIME = new SimpleDateFormat("HH:mm");
	private TextView nickname_textView, admin_textView, last_seen_textView;
	private long timestamp;

	public ChatInfo_user_list_item_viewHolder(View view) {

		this.nickname_textView = (TextView) view.findViewById(R.id.chatInfo_item_user_textView);
		this.admin_textView = (TextView) view.findViewById(R.id.chatInfo_item_user_admin_textView);
		this.last_seen_textView = (TextView) view.findViewById(R.id.chatInfo_item_user_lastSeen_textView);
	}

	public long getTimestamp() {
		return timestamp;
	}

	public TextView getLast_seen_textView() {
		return last_seen_textView;
	}

	public void setTimeStamp_day(long timestamp) {
		this.timestamp = timestamp;
		last_seen_textView.setText("Last seen: " + DATETIME_FORMATTER_DAY.format(new Date(timestamp)));
	}
	public void setTimeStamp_time(long timestamp) {
		this.timestamp = timestamp;
		last_seen_textView.setText("Last seen: " + DATETIME_FORMATTER_TIME.format(new Date(timestamp)));
	}
	public void setTimeStamp_yesterday(long timestamp) {
		last_seen_textView.setText("Last seen: Yesterday at " + DATETIME_FORMATTER_TIME.format(new Date(timestamp)));
	}
	
	public void setTimeStamp_online() {
		last_seen_textView.setText("Last seen: Online");
	}	
	public void setTimeStamp_string(String str) {
		last_seen_textView.setText(str);
	}	
	
	public void set_user_item_textView(String nickname) {
		this.nickname_textView.setText(nickname);
	}
	public void set_user_item_admin_textView(String admin) {
		this.admin_textView.setText(admin);
	}

}
