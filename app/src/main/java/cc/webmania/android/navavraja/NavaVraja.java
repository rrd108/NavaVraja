package cc.webmania.android.navavraja;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.format.Time;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

public class NavaVraja extends FragmentActivity implements ActionBar.TabListener{
	
	private static final long minTime = 0;		//2000 milliseconds = 2sec
	private float minDistance = 0;					//10 meters

	private double rsLng = 17.700364;
	private double rsLat = 46.567181;
	
	private AppSectionsPagerAdapter azAppSectionsPagerAdapter;
    private ViewPager aViewPager;
	private static String provider;
	private LocationManager locationManager;
	private static int distance = -1;
	private static TextView myLocationTextView = null;
	private static TextView infoTextView = null;
	private Time gpsRequestAsked;
	private static Context context;
	private static boolean enabled_network = false;
	private static boolean enabled_gps = false;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.w("Nava Vraja", "onCreate() called");
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		context = getApplicationContext();
		gpsRequestAsked = new Time();
		
		createLocationStuff();
        createTabNavigation();
	}

	protected void createLocationStuff() {
		String context = Context.LOCATION_SERVICE;
		locationManager = (LocationManager) getSystemService(context);				
		initiateLocationListener();
	}

	protected void initiateLocationListener() {
		provider = setProvider();
		Log.w("Nava Vraja", "provider: " + provider);
		if(provider != null){
			locationManager.requestLocationUpdates(provider, minTime, minDistance, locationListener);
			Location location = locationManager.getLastKnownLocation(provider);
			distance = getDistanceFromRS(location);
		}
		distanceChanged(distance);
		setInfoText(infoTextView);
	}

	protected String setProvider() {
		Log.w("Nava Vraja", "setProvider() called");
		String providr = null;
		if(locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)){
			enabled_network = true;
		}
		if(locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)){
			enabled_gps = true;
		}
		if(enabled_network){
			providr = locationManager.NETWORK_PROVIDER;	//first network, as this is fast 
		}
		else if(enabled_gps){
			providr = locationManager.GPS_PROVIDER;
		}
		Log.w("Nava Vraja", "setProvider() returned " + providr);
		return providr;
	}
	
	private final LocationListener locationListener = new LocationListener() {
				
		public void onLocationChanged(Location location) {
			Log.w("Nava Vraja","onLocationChanged() called");
			distance = getDistanceFromRS(location);
			distanceChanged(distance);
		}
		
		public void onProviderDisabled(String providr){
			Log.w("Nava Vraja", "onProviderDisabled(" + providr + ")");
			if(providr == LocationManager.NETWORK_PROVIDER)
				enabled_network = false;
			if(providr == LocationManager.GPS_PROVIDER)
				enabled_gps = false;
			if(!enabled_network && !enabled_gps){
				if(infoTextView != null)
					infoTextView.setText(R.string.no_location_tap_here);
			}
		}
		
		public void onProviderEnabled(String providr){
			Log.w("Nava Vraja", "onProviderEnabled(" + providr + ")");
			//providr = setProvider();
		}
		
		public void onStatusChanged(String providr, int status, Bundle extras){
			Log.w("Nava Vraja", "onStatusChanged(" + providr + ")");
		}
	};
	
	private int getDistanceFromRS(Location location) {
		Log.w("Nava Vraja", "getDistanceFromRS() called");
		double lat = 0;
		double lon = 0;
		int dist = -1;
		if (location != null) {
			lat = location.getLatitude();
			lon = location.getLongitude();
			Log.w("Nava Vraja", "Lat/Lon: " + lat + " / " + lon);
			
			try {
				float[] results = new float[1];
				Location.distanceBetween(rsLat, rsLng, lat, lon, results);
				
				dist = Math.round(results[0]);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return dist;
	}

	protected void distanceChanged(int dist) {
        if (myLocationTextView != null){
        	String d;
        	if(dist == -1)
        		d = getString(R.string.dontknow);
        	else if(dist > 1000){
        		d = dist/1000 + " km";
        	}
        	else{
        		d = dist + " m";
        		//we reccomend to switch to gps if not use that already
        		if(provider != LocationManager.GPS_PROVIDER){
        			requestUserEnableLocationService(LocationManager.GPS_PROVIDER);
        		}
        	}
        	myLocationTextView.setText(d);
        }
	}
			
	protected static void setInfoText(TextView info) {
		if (info != null) {
			if (!enabled_network && !enabled_gps) {
				info.setText(context.getString(R.string.no_location_tap_here));
			} else if (enabled_network) {
				info.setText(context.getString(R.string.network_in_use));
			} else {
				info.setText(context.getString(R.string.gps_in_use));
			}
		}
	}

	private void requestUserEnableLocationService(String providr) {
		Time now = new Time();
		long nim = now.toMillis(true);
		nim = gpsRequestAsked.toMillis(true) - nim;
		if(providr == LocationManager.NETWORK_PROVIDER || 
				(providr == LocationManager.GPS_PROVIDER && nim > 60*60*1000)){
	    	Log.w("Nava Vraja", "requestUserEnableLocationService(" + providr + ") called");
	    	gpsRequestAsked.setToNow();
			final AlertDialog.Builder builder = new AlertDialog.Builder(this);
		    builder.setMessage(getString(R.string.requestUserEnabling, providr))
		           .setCancelable(false)
		           .setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
		               public void onClick(final DialogInterface dialog, final int id) {
		                   startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
		               }
		           })
		           .setNegativeButton(getString(R.string.no), new DialogInterface.OnClickListener() {
		               public void onClick(final DialogInterface dialog, final int id) {
		                    dialog.cancel();
		               }
		           });
		    final AlertDialog alert = builder.create();
		    alert.show();
		}
	}
	
	@Override
	protected void onResume(){
		Log.w("Nava Vraja", "onResume() called");
		super.onResume();
		initiateLocationListener();
	}
	
	@Override
	protected void onStop(){
		Log.w("Nava Vraja", "onStop() called");
		super.onStop();
	}
	
	public void onClick(View v) {
		Log.w("Nava Vraja", "onClick(" + v.getId() + ") called");
		if(v.getId() == R.id.infoText && !enabled_network && !enabled_gps)
			requestUserEnableLocationService(locationManager.NETWORK_PROVIDER);
    }  

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		Log.w("Nava Vraja", "onCreateOptionsMenu() called");
		getMenuInflater().inflate(R.menu.nava_vraja, menu);
		return true;
	}
	

	//-----------------------------------------------------
	
	
	protected void createTabNavigation() {
		// Create the adapter that will return a fragment for each of the three primary sections
        // of the app.
        azAppSectionsPagerAdapter = new AppSectionsPagerAdapter(getSupportFragmentManager(), getApplicationContext());
        // Set up the action bar.
        final ActionBar actionBar = getActionBar();
        // Specify that the Home/Up button should not be enabled, since there is no hierarchical
        // parent.
        actionBar.setHomeButtonEnabled(false);
        // Specify that we will be displaying tabs in the action bar.
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        // Set up the ViewPager, attaching the adapter and setting up a listener for when the
        // user swipes between sections.
        aViewPager = (ViewPager) findViewById(R.id.pager);
        aViewPager.setAdapter(azAppSectionsPagerAdapter);
        aViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                // When swiping between different app sections, select the corresponding tab.
                // We can also use ActionBar.Tab#select() to do this if we have a reference to the
                // Tab.
                actionBar.setSelectedNavigationItem(position);
            }
        });
        // For each of the sections in the app, add a tab to the action bar.
        for (int i = 0; i < azAppSectionsPagerAdapter.getCount(); i++) {
            // Create a tab with text corresponding to the page title defined by the adapter.
            // Also specify this Activity object, which implements the TabListener interface, as the
            // listener for when this tab is selected.
            actionBar.addTab(
                    actionBar.newTab()
                            .setText(azAppSectionsPagerAdapter.getPageTitle(i))
                            .setTabListener(this));
        }
	}

    
	@Override
    public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
		Log.w("Nava Vraja", "onTabUnselected() called");
    }

    @Override
    public void onTabSelected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
    	Log.w("Nava Vraja", "onTabSelected() called");
        // When the given tab is selected, switch to the corresponding page in the ViewPager.
        aViewPager.setCurrentItem(tab.getPosition());
    }

    @Override
    public void onTabReselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to one of the primary
     * sections of the app.
     */
    public static class AppSectionsPagerAdapter extends FragmentPagerAdapter {

        private Context context;

		public AppSectionsPagerAdapter(FragmentManager fm, Context ctx) {
            super(fm);
            context = ctx;
            Log.w("Nava Vraja", "AppSectionsPagerAdapter() called");
        }

        @Override
        public Fragment getItem(int i) {
        	Log.w("Nava Vraja", "getItem("+i+") called");
            // The other sections of the app are dummy placeholders.
            Fragment fragment = new TheSectionFragment();
            Bundle args = new Bundle();
            args.putInt(TheSectionFragment.ARG_SECTION_NUMBER, i);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public int getCount() {
        	Log.w("Nava Vraja", "getCount() called");
            return 5;
        }

        @Override
        public CharSequence getPageTitle(int position) {
        	Log.w("Nava Vraja", "getPageTitle() called");
        	String pageTitle = "error";
        	Resources r = context.getResources();
        	String pageTitles[] = {
        			r.getString(R.string.distance),
        			r.getString(R.string.darshan),
        			r.getString(R.string.podcast),
        			r.getString(R.string.blog),
        			r.getString(R.string.donate)
        			};
          return pageTitles[position];
        }
    }

    /**
     * A fragment representing a section of the app
     */
    public static class TheSectionFragment extends Fragment implements OnItemClickListener{

        public static final String ARG_SECTION_NUMBER = "section_number";
		private ProgressBar progressBar;
		private ListView donateList;

		LayoutInflater layoutInflater;
		ViewGroup theContainer;

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        	Log.w("Nava Vraja", "onCreateView() called");
        	Bundle args = getArguments();
            View rootView = null;
			WebView myWebView;
			WebSettings webSettings = null; 
			String url;
        	layoutInflater = inflater;
        	theContainer = container;

			switch(args.getInt(ARG_SECTION_NUMBER)){
				case 0:
					//distance
	                rootView = inflater.inflate(R.layout.your_distance, container, false);
	                
	                myLocationTextView = ((TextView) rootView.findViewById(R.id.myLocationText));
	                if(distance != -1)
	                	myLocationTextView.setText(String.valueOf(distance));
	                else
	                	myLocationTextView.setText(getString(R.string.dontknow));
	                
	                infoTextView = ((TextView) rootView.findViewById(R.id.infoText));
	                setInfoText(infoTextView);
	                break;
				case 1:
					//darshan
	                rootView = inflater.inflate(R.layout.webview, container, false);
	                progressBar = (ProgressBar) rootView.findViewById(R.id.progressBar);
	                myWebView = (WebView) rootView.findViewById(R.id.aWeb);
	    			myWebView.setWebViewClient(new WebViewClient(){
	    				@Override
	    		        public void onPageFinished(WebView view, String url) {
	    		            super.onPageFinished(view, url);
	    		            progressBar.setVisibility(View.GONE);
	    		        }
	    			});
	    			webSettings = myWebView.getSettings();
	                webSettings.setJavaScriptEnabled(true);
	                myWebView.loadUrl(getString(R.string.url_darshan));
	                /*
	                 * http://www.flickr.com/services/api/flickr.photosets.getPhotos.html
	                 * http://www.flickr.com/services/api/flickr.photos.getInfo.html
	                 * http://www.flickr.com/services/api/flickr.photos.getSizes.html
	                 * */
	                break;				
				case 2:
					//podcast
	                rootView = inflater.inflate(R.layout.webview, container, false);
	                progressBar = (ProgressBar) rootView.findViewById(R.id.progressBar);
	                myWebView = (WebView) rootView.findViewById(R.id.aWeb);
	    			webSettings = myWebView.getSettings();
	                webSettings.setJavaScriptEnabled(true);
		
	                url = getString(R.string.url_podcast);
	    			myWebView.setWebViewClient(new WebViewClient(){
	    				@Override
	    				public boolean shouldOverrideUrlLoading(WebView view, String url) {
	    					if (url.endsWith(".mp3")) {
	    						Intent i = new Intent(Intent.ACTION_VIEW);
	    						i.setDataAndType(Uri.parse(url), "audio/*");
	    						startActivity(i);
	    						return true;
	    					} else {
	    						view.loadUrl(url);
	    						return true;
	    					}
	    				}
	    				@Override
	    		        public void onPageFinished(WebView view, String url) {
	    		            super.onPageFinished(view, url);
	    		            progressBar.setVisibility(View.GONE);
	    		        }
	    			});
	    			//myWebView.clearCache(true);
	    			myWebView.loadUrl(url);
	                break;
				case 3:
					//blog
	                rootView = inflater.inflate(R.layout.webview, container, false);
	                progressBar = (ProgressBar) rootView.findViewById(R.id.progressBar);
	                myWebView = (WebView) rootView.findViewById(R.id.aWeb);
	    			myWebView.setWebViewClient(new WebViewClient(){
	    				@Override
	    		        public void onPageFinished(WebView view, String url) {
	    		            super.onPageFinished(view, url);
	    		            progressBar.setVisibility(View.GONE);
	    		        }
	    			});
	    			webSettings = myWebView.getSettings();
	                //webSettings.setJavaScriptEnabled(true);
	                myWebView.loadUrl(getString(R.string.url_blog));
	                break;				
				case 4:
					//donate
					rootView = inflater.inflate(R.layout.donate, container, false);
					donateList = (ListView) rootView.findViewById(android.R.id.list);
					String[] values = getResources().getStringArray(R.array.donate_list);
					ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_1, values);
					donateList.setAdapter(adapter);
					donateList.setOnItemClickListener(this);
					break;				
	        }
            return rootView;
        }

    	@Override
    	public void onItemClick(AdapterView<?> listView, View textView, int position, long arg) {
    		Log.w("Nava Vraja", "onItemClick(" + position + ": " + donateList.getItemAtPosition(position) + ") called");
    		Intent intent = new Intent("cc.webmania.android.navavraja.DONATE_DETAIL");
   			intent.putExtra("detail", position);
    		startActivity(intent);
    	}
    }
}