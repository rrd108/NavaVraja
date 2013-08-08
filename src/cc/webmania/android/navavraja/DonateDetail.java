package cc.webmania.android.navavraja;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

public class DonateDetail extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.donate_detail);
		
		Bundle extras = getIntent().getExtras();
		int detailPos = extras.getInt("detail");
		
		TextView donateTitle = (TextView) findViewById(R.id.detail_title);
		TextView donateText = (TextView) findViewById(R.id.detail_text);
		
		String[] donate_titles = getResources().getStringArray(R.array.donate_list);
		String[] donate_texts = getResources().getStringArray(R.array.donate_details);
		donateTitle.setText(donate_titles[detailPos]);
		donateText.setText(donate_texts[detailPos]);
	}
}
