package mobi.roomz.ui;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import mobi.roomz.R;
import android.text.SpannableStringBuilder;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.rockerhieu.emojicon.EmojiconTextView;

public class Chat_item_viewHolder {
// final static DateFormat DATETIME_FORMATTER = new SimpleDateFormat("HH:mm  dd/MM/yyyy");
	final static DateFormat DATETIME_FORMATTER = new SimpleDateFormat("HH:mm");
	private long id;
	private TextView nickname_textView;
	private TextView time_textView;
	private EmojiconTextView message_emojiconTextView;
	private long timestamp;
	private LinearLayout container_layout;
	private View nickname_underline;

	public Chat_item_viewHolder(View view) {

		this.nickname_textView = (TextView) view.findViewById(R.id.chatPage_item_nickname_textView);
		this.time_textView = (TextView) view.findViewById(R.id.chatPage_item_time_textView);
		this.message_emojiconTextView = (EmojiconTextView) view.findViewById(R.id.chatPage_item_message_emojiconTextView);
		this.container_layout = (LinearLayout) view.findViewById(R.id.chatPage_item_container_layout);
		this.nickname_underline = (View) view.findViewById(R.id.chatPage_nickname_underline);
	}

	public long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
		time_textView.setText(DATETIME_FORMATTER.format(new Date(timestamp)));
	}

	public void setTimeStamp_sending () {
		time_textView.setText("sending..");
	}
	
	public void setTimeStamp_failed () {
		time_textView.setText("Failed");
	}
	
	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public void setNickname_textView(String roomName_textView) {
		this.nickname_textView.setText(roomName_textView);
	}

	public TextView getNickname_textView() {
		return nickname_textView;
	}

	public void setTime_textView(String time_textView) {
		this.time_textView.setText(time_textView);
	}

	public TextView getTime_textView() {
		return time_textView;
	}

	public void setMessage_textView(String message) {
		this.message_emojiconTextView.setText(message);
	}

	public void setMessage_spannable_textView(SpannableStringBuilder message) {
		this.message_emojiconTextView.setText(message);
	}
	
	public TextView getMessage_textView() {
		return message_emojiconTextView;
	}

	public LinearLayout getContainer_layout() {
		return container_layout;
	}

	public void setContainer_layout(LinearLayout container_layout) {
		this.container_layout = container_layout;
	}

	public View getNickname_underline() {
		return nickname_underline;
	}

	public void setNickname_underline(View nickname_underline) {
		this.nickname_underline = nickname_underline;
	}

}
