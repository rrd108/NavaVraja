package cc.webmania.android.navavraja;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.TextView;

public class DonateDetail extends Activity{

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
		setTitle(donate_titles[detailPos]);
		donateTitle.setText(donate_titles[detailPos]);
		donateText.setText(donate_texts[detailPos]);
	}
	
	public void onClick(View v) {
		Log.w("Nava Vraja", "onClick(" + v.getId() + ") called");
		
		setContentView(R.layout.webview);
        final ProgressBar progressBar = (ProgressBar) findViewById(R.id.progressBar);
        WebView myWebView = (WebView) findViewById(R.id.aWeb);
		myWebView.setWebViewClient(new WebViewClient(){
			@Override
	        public void onPageFinished(WebView view, String url) {
	            super.onPageFinished(view, url);
	            progressBar.setVisibility(View.GONE);
	        }
		});
		WebSettings webSettings = myWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        myWebView.loadUrl(getString(R.string.url_paypal));
    }
}
