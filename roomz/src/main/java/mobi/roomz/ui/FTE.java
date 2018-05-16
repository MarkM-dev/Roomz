package mobi.roomz.ui;

import mobi.roomz.R;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

public class FTE extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.fte);

		// full-screen.
		this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
		
		TextView fte_join_textView = (TextView) findViewById(R.id.fte_join_textView);
		TextView fte_host_textView = (TextView) findViewById(R.id.fte_host_textView);
		
		fte_join_textView.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent join_intent = new Intent(FTE.this, JoinPage.class);
				startActivity(join_intent);
				finish();
			}
		});
		fte_host_textView.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent host_intent = new Intent(FTE.this, HostPage.class);
				startActivity(host_intent);
				finish();
			}
		});

	}
	
	@Override
	public void onBackPressed() {
		super.onBackPressed();
		Intent intent = new Intent(Intent.ACTION_MAIN);
		intent.addCategory(Intent.CATEGORY_HOME);
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		startActivity(intent);
	}

}
