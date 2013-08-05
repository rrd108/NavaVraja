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
import android.support.v4.app.ListFragment;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.TextView;

public class NavaVraja extends FragmentActivity implements ActionBar.TabListener {
	
	private static final long minTime = 2000;		//2000 milliseconds = 2sec
	private float minDistance = 10;					//10 meters

	private double rsLng = 17.700382;
	private double rsLat = 46.567255;
	
	private AppSectionsPagerAdapter azAppSectionsPagerAdapter;
    private ViewPager aViewPager;
	private static String provider;
	private LocationManager locationManager;
	private static CharSequence distance;
	private static TextView myLocationTextView = null;
	private static TextView infoTextView = null;
	private Boolean gpsRequestAsked = false;
	private static int enabledProviders = 0;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.w("Nava Vraja", "onCreate() called");
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		
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
		
		String context = Context.LOCATION_SERVICE;
		locationManager = (LocationManager)getSystemService(context);
				
		provider = setProvider();
		
		Log.w("Nava Vraja", "provider: " + provider);
		locationManager.requestLocationUpdates(provider, minTime, minDistance, locationListener);
		Location location = locationManager.getLastKnownLocation(provider);
		distance = getDistanceFromRS(location);
	}

	protected String setProvider() {
		Log.w("Nava Vraja", "setProvider() called");
		String provider = null;
		if(locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)){
			provider = locationManager.NETWORK_PROVIDER;		//first network, as this is fast
			enabledProviders++;
		}
		if(locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)){
			if(enabledProviders == 0)	//network is disabled, gps is enabled
				provider = locationManager.GPS_PROVIDER;
			enabledProviders++;
		}
		if(enabledProviders <= 0){
			//none of the providers enabled, ask the user to enable at least network
			provider = locationManager.NETWORK_PROVIDER;		//listen to network better chance this will be enabled
			requestUserEnableLocationService(locationManager.NETWORK_PROVIDER);
		}
		return provider;
	}

	private final LocationListener locationListener = new LocationListener() {
		
		/*
		 * Esetek:
		 * 		1. network +, gps +		distance > 5km network, distance < 5km gps
		 * 		2. network +, gps -		distance > 5km network, distance < 5km request enable gps
		 * 		3. network -, gps +		use gps
		 * 		4. network -, gps -		request enabling and add an info box at the bottom that first tab will not work
		 */
		
		public void onLocationChanged(Location location) {
			Log.w("Nava Vraja","onLocationChanged() called");
			getDistanceFromRS(location);
		}
		
		public void onProviderDisabled(String provider){
			Log.w("Nava Vraja", "onProviderDisabled(" + provider + ")");
			enabledProviders--;
			if(enabledProviders <= 0){
				if(infoTextView != null)
					infoTextView.setText(R.string.no_location_tap_here);
			}
		}
		
		public void onProviderEnabled(String provider){
			Log.w("Nava Vraja", "onProviderEnabled(" + provider + ")");
			enabledProviders++;
			Location location = locationManager.getLastKnownLocation(provider);
			distance = getDistanceFromRS(location);
		}
		
		public void onStatusChanged(String provider, int status, Bundle extras){
			Log.w("Nava Vraja", "onStatusChanged(" + provider + ")");
		}
	};
	
	@Override
	protected void onRestart(){
		super.onRestart();
		provider = setProvider();
	}
	
	@Override
	protected void onStop(){
		Log.w("Nava Vraja", "onStop() called");
		//locationManager.removeUpdates(locationListener);
		super.onStop();
	}
	
	public void onClick(View v) {
		Log.w("Nava Vraja", "onClick(" + v.getId() + ") called");
		if(v.getId() == R.id.infoText)
			requestUserEnableLocationService(locationManager.NETWORK_PROVIDER);
    }  

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		Log.w("Nava Vraja", "onCreateOptionsMenu() called");
		getMenuInflater().inflate(R.menu.nava_vraja, menu);
		return true;
	}
	
	private CharSequence getDistanceFromRS(Location location) {
		Log.w("Nava Vraja", "getDistanceFromRS() called");
		CharSequence dist = "";
		double lat = 0;
		double lon = 0;
		if (location != null) {
			lat = location.getLatitude();
			lon = location.getLongitude();
			Log.w("Nava Vraja", "Lat/Lon: " + lat + " / " + lon);
			
			try {
				float[] results = new float[1];
				Location.distanceBetween(rsLat, rsLng, lat, lon, results);
				
				int distance = Math.round(results[0]);
				if(distance < 5000){
					//ha 5km-en belül van és ki van kapcsolva a gps akkor alul azt mondja, hogy no loation available
					if(!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)){
						requestUserEnableLocationService(LocationManager.GPS_PROVIDER);
					}
					locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
				}
				if(distance > 1000){
					dist = distance/1000 + " km";
				}
				else{
					dist = distance + " m";
				}
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		else{
			dist = getString(R.string.dontknow);
		}
        if (myLocationTextView != null){
        	myLocationTextView.setText(dist);
    		infoTextView.setText(provider + " Lat/Lon: " + lat + " / " + lon + "\n" + enabledProviders);
        }
		return dist;
	}
			
    private void requestUserEnableLocationService(String provider) {
    	if(!gpsRequestAsked){
    		gpsRequestAsked = true;
	    	Log.w("Nava Vraja", "requestUserEnableLocationService(" + provider + ") called");
			final AlertDialog.Builder builder = new AlertDialog.Builder(this);
		    builder.setMessage(getString(R.string.requestUserEnabling, provider))
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
        	Log.w("Nava Vraja", "getItem() called");
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
    public static class TheSectionFragment extends Fragment {

        public static final String ARG_SECTION_NUMBER = "section_number";
		private ProgressBar progressBar;

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        	Log.w("Nava Vraja", "onCreateView() called");
        	Bundle args = getArguments();
            View rootView = null;
			WebView myWebView;
			WebSettings webSettings = null; 
			
			String url;
			switch(args.getInt(ARG_SECTION_NUMBER)){
				case 0:
	                rootView = inflater.inflate(R.layout.your_distance, container, false);
	                
	                myLocationTextView = ((TextView) rootView.findViewById(R.id.myLocationText));
	                myLocationTextView.setText(distance);
	                
	                infoTextView = ((TextView) rootView.findViewById(R.id.infoText));
	                if(enabledProviders <= 0){
	                	infoTextView.setText(R.string.no_location_tap_here);
	                }
	                else{
	                	infoTextView.setText("");
	                }
	                break;
				case 1:
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
	    			myWebView.loadUrl(url);
	                break;
				case 3:
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
					rootView = inflater.inflate(R.layout.donate, container, false);
					//rootView.findViewById(R.id.demo_collection_button).setOnClickListener(new View.OnClickListener() {
					//    @Override
					//    public void onClick(View view) {
					//        Intent intent = new Intent(getActivity(), Donate.class);
					//        startActivity(intent);
					//    }
					//});
					
					/*String[] values = new String[] { "Android", "iPhone", "WindowsMobile",
							"Blackberry", "WebOS", "Ubuntu", "Windows7", "Max OS X",
							"Linux", "OS/2" };
					ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(),
							android.R.layout.simple_list_item_1, values);
					setListAdapter(adapter);
					rootView.findViewById(android.R.id.list).setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View view) {
							Log.w("Nava Vraja", "case 4 : onClick(" + view.getId() + ") called");
						}
					});*/
					break;				
	        }
            return rootView;
        }
    }    
}