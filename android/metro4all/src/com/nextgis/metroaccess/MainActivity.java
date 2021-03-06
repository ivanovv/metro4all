/******************************************************************************
 * Project:  Metro4All
 * Purpose:  Routing in subway.
 * Author:   Dmitry Baryshnikov, polimax@mail.ru
 ******************************************************************************
*   Copyright (C) 2013 NextGIS
*
*    This program is free software: you can redistribute it and/or modify
*    it under the terms of the GNU General Public License as published by
*    the Free Software Foundation, either version 3 of the License, or
*    (at your option) any later version.
*
*    This program is distributed in the hope that it will be useful,
*    but WITHOUT ANY WARRANTY; without even the implied warranty of
*    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
*    GNU General Public License for more details.
*
*    You should have received a copy of the GNU General Public License
*    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ****************************************************************************/
package com.nextgis.metroaccess;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.app.ActionBar.OnNavigationListener;
import com.nextgis.metroaccess.data.DownloadData;
import com.nextgis.metroaccess.data.GraphDataItem;
import com.nextgis.metroaccess.data.MAGraph;
import com.nextgis.metroaccess.data.PortalItem;
import com.nextgis.metroaccess.data.StationItem;

import edu.asu.emit.qyan.alg.model.Path;
import edu.asu.emit.qyan.alg.model.abstracts.BaseVertex;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SpinnerAdapter;
import android.widget.Toast;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;


import static com.nextgis.metroaccess.Constants.*;

//https://code.google.com/p/k-shortest-paths/

public class MainActivity extends SherlockActivity implements OnNavigationListener {

	protected boolean m_bInterfaceLoaded;

	protected static Handler m_oGetJSONHandler;

	protected Button m_oSearchButton;
	protected MenuItem m_oSearchMenuItem;

	protected List<Pair<Integer, Integer>> m_aoDepRecentIds, m_aoArrRecentIds;
	protected int m_nDepartureStationId, m_nArrivalStationId;
	protected int m_nDeparturePortalId, m_nArrivalPortalId;

	protected List<DownloadData> m_asDownloadData;

	public static String m_sUrl = "http://metro4all.org/data/v2/";
	public static MAGraph m_oGraph;

	protected ListView m_lvListButtons;
    protected ButtonListAdapter m_laListButtons;

    protected int mCurrentItemPosition;

    List<String> mCities;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.empty_activity_main);
		m_nDepartureStationId = m_nArrivalStationId = -1;
		m_nDeparturePortalId = m_nArrivalPortalId = -1;

		m_aoDepRecentIds = new ArrayList<Pair<Integer, Integer>>();
		m_aoArrRecentIds = new ArrayList<Pair<Integer, Integer>>();
        mCities = new ArrayList<String>();

		m_bInterfaceLoaded = false;

        // initialize the default settings
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		m_sUrl = prefs.getString(PreferencesActivity.KEY_PREF_DOWNLOAD_PATH, m_sUrl);

		String sCurrentCity = prefs.getString(PreferencesActivity.KEY_PREF_CITY, "");
		String sCurrentCityLang = prefs.getString(PreferencesActivity.KEY_PREF_CITYLANG, Locale.getDefault().getLanguage());
		m_oGraph = new MAGraph(this.getBaseContext(), sCurrentCity, getExternalFilesDir(null), sCurrentCityLang);

		//create downloading queue empty initially
		m_asDownloadData = new ArrayList<DownloadData>();

		CreateHandler();

		//check for data exist
		if(IsRoutingDataExist()){
			//else check for updates
			CheckForUpdates();
		}
		else{
			//ask to download data
			GetRoutingData();
		}

	}

	protected void CreateHandler(){

		m_oGetJSONHandler = new Handler() {
            public void handleMessage(Message msg) {
            	super.handleMessage(msg);

            	Bundle resultData = msg.getData();
            	boolean bHaveErr = resultData.getBoolean(BUNDLE_ERRORMARK_KEY);
            	int nEventSource = resultData.getInt(BUNDLE_EVENTSRC_KEY);
            	String sPayload = resultData.getString(BUNDLE_PAYLOAD_KEY);

            	if(bHaveErr){
            		MainActivity.this.ErrMessage(resultData.getString(BUNDLE_MSG_KEY));
	            	switch(nEventSource){
	            	case 1://get remote meta
	            		File file = new File(getExternalFilesDir(null), REMOTE_METAFILE);
	            		sPayload = readFromFile(file);
	            		break;
	            	case 2:
	            		if(IsRoutingDataExist())
	                    	LoadInterface();
	            		break;
            		default:
            			return;
	            	}
            	}

            	switch(nEventSource){
            	case 1://get remote meta
            		if(IsRoutingDataExist()){
            			//check if updates available
            			CheckUpdatesAvailable(sPayload);
            		}
            		else{
            			AskForDownloadData(sPayload);
            		}
            		break;
            	case 2:
            		if(m_asDownloadData.isEmpty()){
            			m_oGraph.FillRouteMetadata();
            			LoadInterface();
            		}
            		else{
            			OnDownloadData();
            		}
            		break;
            	}
            }
        };
	}

	protected void LoadInterface(){

        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String sCurrentCity = prefs.getString(PreferencesActivity.KEY_PREF_CITY, m_oGraph.GetCurrentCity());

        if (sCurrentCity == null)
            return;

        if(sCurrentCity.length() < 2){
        	//find first city and load it
        	m_oGraph.SetFirstCityAsCurrent();
        }
        else{
        	m_oGraph.SetCurrentCity( sCurrentCity );
        }

        if(!m_oGraph.IsValid())
        	return;

		m_bInterfaceLoaded = true;
		setContentView(R.layout.activity_main);

        fillActionBarList();

		m_lvListButtons = (ListView)findViewById(R.id.lvButtList);
		m_laListButtons = new ButtonListAdapter(this);
		// set adapter to list view
		m_lvListButtons.setAdapter(m_laListButtons);
		m_lvListButtons.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
	        	switch(position){
	        	case 0: //from
	        		onSelectDepatrure();
	        		break;
	        	case 1: //to
	        		onSelectArrival();
	        		break;
	        	case 2:
	        		onSettings();
	        		break;
	        	}
	        }
	    });


		m_oSearchButton = (Button) findViewById(R.id.btSearch);
		m_oSearchButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            	onSearch();
             }
        });
		m_oSearchButton.setEnabled(false);

    	if(m_oSearchButton != null)
    		m_oSearchButton.setEnabled(false);
    	if(m_oSearchMenuItem != null)
    		m_oSearchMenuItem.setEnabled(false);

    	if(!m_oGraph.IsValid()){
    		MainActivity.this.ErrMessage( m_oGraph.GetLastError());
    	}
    	else{
    		UpdateUI();
    	}

	}

    protected void fillActionBarList() {
        ActionBar actionBar = getSupportActionBar();
        Context context = actionBar.getThemedContext();
        ArrayList<String> items = new ArrayList<String>();
        int nCurrentCity = 0;
        mCities.clear();
        final List<GraphDataItem> city_list = new ArrayList<GraphDataItem>(m_oGraph.GetRouteMetadata().values());
        for(int i = 0; i < city_list.size(); i++){
            items.add(city_list.get(i).GetLocaleName());
            mCities.add(city_list.get(i).GetPath());
            if(m_oGraph.GetCurrentCity().equals(city_list.get(i).GetPath())){
                nCurrentCity = i;
            }
        }

        //ArrayAdapter<CharSequence> adapter = new ArrayAdapter<CharSequence>(context, R.layout.sherlock_spinner_dropdown_item, items.toArray(new String[items.size()]));
//ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(context, R.array.views, R.layout.sherlock_spinner_dropdown_item);
        ArrayAdapter<CharSequence> adapter = new ArrayAdapter<CharSequence>(context, R.layout.citydropdown, items.toArray(new String[items.size()]));
        adapter.setDropDownViewResource(R.layout.sherlock_spinner_dropdown_item);
        actionBar.setDisplayShowTitleEnabled(false);
        actionBar.setNavigationMode(com.actionbarsherlock.app.ActionBar.NAVIGATION_MODE_LIST);
        actionBar.setListNavigationCallbacks((SpinnerAdapter) adapter, this);
        actionBar.setSelectedNavigationItem(nCurrentCity);

        mCurrentItemPosition = nCurrentCity;
    }

    protected void onSettings() {
        Intent intentSet = new Intent(this, PreferencesActivity.class);
        //intentSet.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);        
        //Bundle bundle = new Bundle();
        //bundle.putParcelable(BUNDLE_METAMAP_KEY, m_oGraph);
        //intentSet.putExtras(bundle);            
        startActivityForResult(intentSet, PREF_RESULT);
	}

	@Override
	public boolean onCreateOptionsMenu(com.actionbarsherlock.view.Menu menu) {
		/*m_oSearchMenuItem = menu.add(com.actionbarsherlock.view.Menu.NONE, MENU_SEARCH, com.actionbarsherlock.view.Menu.NONE, R.string.sSearch)
		.setIcon(R.drawable.ic_action_search);
		m_oSearchMenuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
		m_oSearchMenuItem.setEnabled(false);
		m_oSearchMenuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);	
		 */
		menu.add(com.actionbarsherlock.view.Menu.NONE, MENU_SETTINGS, com.actionbarsherlock.view.Menu.NONE, R.string.sSettings)
       .setIcon(R.drawable.ic_action_settings)
       .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);

		menu.add(com.actionbarsherlock.view.Menu.NONE, MENU_ABOUT, com.actionbarsherlock.view.Menu.NONE, R.string.sAbout)
		.setIcon(R.drawable.ic_action_about)
		.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);

		return true;
//		return super.onCreateOptionsMenu(menu);
	}

	@Override
    public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
        case android.R.id.home:
            return false;
        case MENU_SEARCH:
        	onSearch();
        	return true;
        case MENU_SETTINGS:
            // app icon in action bar clicked; go home
        	onSettings();
            return true;
        case MENU_ABOUT:
            Intent intentAbout = new Intent(this, AboutActivity.class);
            intentAbout.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intentAbout);
            return true;
        }
		return super.onOptionsItemSelected(item);
	}

	protected void CheckForUpdates(){
		MetaDownloader uploader = new MetaDownloader(MainActivity.this, getResources().getString(R.string.sDownLoading), m_oGetJSONHandler, true);
		uploader.execute(GetDownloadURL() + META);
	}

	protected void GetRoutingData(){
		MetaDownloader loader = new MetaDownloader(MainActivity.this, getResources().getString(R.string.sDownLoading), m_oGetJSONHandler, true);
		loader.execute(GetDownloadURL() + META);
	}

	//check if data for routing is downloaded
	protected boolean IsRoutingDataExist(){
		return m_oGraph.IsRoutingDataExist();
	}

	protected void CheckUpdatesAvailable(String sJSON){

		m_oGraph.OnUpdateMeta(sJSON, true);
		final List<GraphDataItem> items = m_oGraph.HasChanges();

		int count = items.size();
		if(count < 1){
			LoadInterface();
			return;
		}
		final boolean[] checkedItems = new boolean[count];
	    for(int i = 0; i < count; i++){
	    	checkedItems[i] = true;
	    }

	    final CharSequence[] checkedItemStrings = new CharSequence[count];
	    for(int i = 0; i < count; i++){
	    	checkedItemStrings[i] = items.get(i).GetFullName();
	    }

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.sUpdateAvaliable)
		.setMultiChoiceItems(checkedItemStrings, checkedItems,
				new DialogInterface.OnMultiChoiceClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which, boolean isChecked) {
				checkedItems[which] = isChecked;
			}
		})
		.setPositiveButton(R.string.sDownload,
				new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int id) {

				m_asDownloadData.clear();

				for (int i = 0; i < checkedItems.length; i++) {
					if (checkedItems[i]){
						m_asDownloadData.add(new DownloadData(MainActivity.this, items.get(i), GetDownloadURL() + items.get(i).GetPath() + ".zip", m_oGetJSONHandler));
					}
				}

				OnDownloadData();

			}
		})

		.setNegativeButton(R.string.sCancel,
				new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int id) {
				LoadInterface();
				dialog.cancel();
			}
		});
		builder.create();
		builder.show();
    }

	protected void AskForDownloadData(String sJSON){
		//ask user for download
		m_oGraph.OnUpdateMeta(sJSON, false);
		final List<GraphDataItem> items = m_oGraph.HasChanges();

	    int count = items.size();
	    if(count == 0)
	    	return;

	    final boolean[] checkedItems = new boolean[count];
	    for(int i = 0; i < count; i++){
	    	checkedItems[i] = true;
	    }

	    final CharSequence[] checkedItemStrings = new CharSequence[count];
	    for(int i = 0; i < count; i++){
	    	checkedItemStrings[i] = items.get(i).GetFullName();
	    }

	    AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.sSelectDataToDownload)
			   .setMultiChoiceItems(checkedItemStrings, checkedItems,
						new DialogInterface.OnMultiChoiceClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which, boolean isChecked) {
								checkedItems[which] = isChecked;
							}
						})
				.setPositiveButton(R.string.sDownload,
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int id) {

								m_asDownloadData.clear();

								for (int i = 0; i < checkedItems.length; i++) {
									if (checkedItems[i]){
										m_asDownloadData.add(new DownloadData(MainActivity.this, items.get(i), GetDownloadURL() + items.get(i).GetPath() + ".zip", m_oGetJSONHandler));
									}
								}
								OnDownloadData();
							}
						})

				.setNegativeButton(R.string.sCancel,
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int id) {
								dialog.cancel();

							}
						});
		builder.create();
		builder.show();
	}

	protected void OnDownloadData(){
		if(m_asDownloadData.isEmpty())
			return;
		DownloadData data = m_asDownloadData.get(0);
		m_asDownloadData.remove(0);

		data.OnDownload();
	}

	public static boolean writeToFile(File filePath, String sData){
		try{
			FileOutputStream os = new FileOutputStream(filePath, false);
			OutputStreamWriter outputStreamWriter = new OutputStreamWriter(os);
	        outputStreamWriter.write(sData);
	        outputStreamWriter.close();
	        return true;
		}
		catch(IOException e){
			return false;
		}
	}

	public static String readFromFile(File filePath) {

	    String ret = "";

	    try {
	    	FileInputStream inputStream = new FileInputStream(filePath);

	        if ( inputStream != null ) {
	            InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
	            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
	            String receiveString = "";
	            StringBuilder stringBuilder = new StringBuilder();

	            while ( (receiveString = bufferedReader.readLine()) != null ) {
	                stringBuilder.append(receiveString);
	            }

	            inputStream.close();
	            ret = stringBuilder.toString();
	        }
	    }
	    catch (FileNotFoundException e) {
	    	e.printStackTrace();
	    } catch (IOException e) {
	    	e.printStackTrace();
	    }

	    return ret;
	}

	@Override
	protected void onPause() {
		final SharedPreferences.Editor edit = PreferenceManager.getDefaultSharedPreferences(this).edit();

		//store departure and arrival

		edit.putInt("dep_" + BUNDLE_STATIONID_KEY, m_nDepartureStationId);
		edit.putInt("arr_" + BUNDLE_STATIONID_KEY, m_nArrivalStationId);
		edit.putInt("dep_" + BUNDLE_PORTALID_KEY, m_nDeparturePortalId);
		edit.putInt("arr_" + BUNDLE_PORTALID_KEY, m_nArrivalPortalId);

		int nbeg = m_aoDepRecentIds.size() < MAX_RECENT_ITEMS ? 0 : m_aoDepRecentIds.size() - MAX_RECENT_ITEMS;
		int nsize = m_aoDepRecentIds.size() - nbeg;
		int counter = 0;
		for(int i = nbeg; i < nsize; i++){
			edit.putInt("recent_dep_" + BUNDLE_STATIONID_KEY+counter, m_aoDepRecentIds.get(i).first);
			edit.putInt("recent_dep_" + BUNDLE_PORTALID_KEY+counter, m_aoDepRecentIds.get(i).second);

			counter++;
		}
		edit.putInt("recent_dep_counter",counter);

		nbeg = m_aoArrRecentIds.size() < MAX_RECENT_ITEMS ? 0 : m_aoArrRecentIds.size() - MAX_RECENT_ITEMS;
		nsize = m_aoArrRecentIds.size() - nbeg;
		counter = 0;
		for(int i = nbeg; i < nsize; i++){
			edit.putInt("recent_arr_" + BUNDLE_STATIONID_KEY+counter, m_aoArrRecentIds.get(i).first);
			edit.putInt("recent_arr_" + BUNDLE_PORTALID_KEY+counter, m_aoArrRecentIds.get(i).second);

			counter++;
		}
		edit.putInt("recent_arr_counter",counter);

		edit.commit();

		super.onPause();
	}

	@Override
	protected void onResume() {
		super.onResume();
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

	    m_nDepartureStationId = prefs.getInt("dep_"+BUNDLE_STATIONID_KEY, -1);
	    m_nArrivalStationId = prefs.getInt("arr_"+BUNDLE_STATIONID_KEY, -1);
	    m_nDeparturePortalId = prefs.getInt("dep_"+BUNDLE_PORTALID_KEY, -1);
	    m_nArrivalPortalId = prefs.getInt("arr_"+BUNDLE_PORTALID_KEY, -1);

		int size = prefs.getInt("recent_dep_counter", 0);
		for(int i = 0; i < size; i++){
			int nB = prefs.getInt("recent_dep_"+BUNDLE_STATIONID_KEY+i, -1);
			int nE = prefs.getInt("recent_dep_"+BUNDLE_PORTALID_KEY+i, -1);

			Pair<Integer, Integer> pair = Pair.create(nB, nE);
			if(!m_aoDepRecentIds.contains(pair)){
				m_aoDepRecentIds.add(Pair.create(nB, nE));
			}
		}

		size = prefs.getInt("recent_arr_counter", 0);
		for(int i = 0; i < size; i++){
			int nB = prefs.getInt("recent_arr_"+BUNDLE_STATIONID_KEY+i, -1);
			int nE = prefs.getInt("recent_arr_"+BUNDLE_PORTALID_KEY+i, -1);
			Pair<Integer, Integer> pair = Pair.create(nB, nE);
			if(!m_aoArrRecentIds.contains(pair)){
				m_aoArrRecentIds.add(Pair.create(nB, nE));
			}
		}


		//check if routing data changed
		m_oGraph.FillRouteMetadata();

		if(m_bInterfaceLoaded){
			if(m_oGraph.IsEmpty()){
				if(IsRoutingDataExist()){
					LoadInterface();
				}
			}
			UpdateUI();
		}
	}

	protected void 	onSelectDepatrure(){
	    Intent intent = new Intent(this, SelectStationActivity.class);
	    Bundle bundle = new Bundle();
	    bundle.putInt(BUNDLE_EVENTSRC_KEY, DEPARTURE_RESULT);
        //bundle.putSerializable(BUNDLE_STATIONMAP_KEY, (Serializable) mmoStations);
        bundle.putBoolean(BUNDLE_ENTRANCE_KEY, true);
	    intent.putExtras(bundle);
	    startActivityForResult(intent, DEPARTURE_RESULT);
	}

	protected void 	onSelectArrival(){
	    Intent intent = new Intent(this, SelectStationActivity.class);
	    Bundle bundle = new Bundle();
	    bundle.putInt(BUNDLE_EVENTSRC_KEY, ARRIVAL_RESULT);
        //bundle.putSerializable(BUNDLE_STATIONMAP_KEY, (Serializable) mmoStations);
        bundle.putBoolean(BUNDLE_ENTRANCE_KEY, false);
	    intent.putExtras(bundle);
	    startActivityForResult(intent, ARRIVAL_RESULT);
	}

	@Override
	  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
	    /*if (resultCode != RESULT_OK) {
	    	return;
	    }*/

    	int nStationId = -1;
    	int nPortalId = -1;

    	if(data != null){
    		nStationId = data.getIntExtra(BUNDLE_STATIONID_KEY, -1);
    		nPortalId = data.getIntExtra(BUNDLE_PORTALID_KEY, -1);
    	}


		final SharedPreferences.Editor edit = PreferenceManager.getDefaultSharedPreferences(this).edit();

	    switch(requestCode){
	    case DEPARTURE_RESULT:
	    	m_aoDepRecentIds.add(Pair.create(nStationId, nPortalId));
	       	m_nDepartureStationId = nStationId;
	    	m_nDeparturePortalId = nPortalId;
			edit.putInt("dep_"+BUNDLE_STATIONID_KEY, m_nDepartureStationId);
			edit.putInt("dep_"+BUNDLE_PORTALID_KEY, m_nDeparturePortalId);
	       	break;
	    case ARRIVAL_RESULT:
	    	m_aoArrRecentIds.add(Pair.create(nStationId, nPortalId));
	    	m_nArrivalStationId = nStationId;
	    	m_nArrivalPortalId = nPortalId;
			edit.putInt("arr_"+BUNDLE_STATIONID_KEY, m_nArrivalStationId);
			edit.putInt("arr_"+BUNDLE_PORTALID_KEY, m_nArrivalPortalId);
	    	break;
	    case PREF_RESULT:
	    	break;
    	default:
    		break;
	    }

	    edit.commit();

	    if(m_bInterfaceLoaded){
	    	UpdateUI();
    	}
	    else{
	    	LoadInterface();
    	}
	}

	protected void UpdateUI(){

        //update current city
        fillActionBarList();

		if(m_oGraph.HasStations()){
	    	StationItem dep_sit = m_oGraph.GetStation(m_nDepartureStationId);
	    	String sNotSet = getString(R.string.sNotSet);
	    	if(dep_sit != null && m_laListButtons != null){
	    		m_laListButtons.setFromStationName(dep_sit.GetName());
	    		PortalItem pit = dep_sit.GetPortal(m_nDeparturePortalId);
	    		if(pit != null){
	    			m_laListButtons.setFromEntranceName(pit.GetName());
	    		}
	    		else{
	    			m_laListButtons.setFromEntranceName(sNotSet);
	    			m_nDeparturePortalId = -1;
	    		}
	    	}
	    	else{
	    		m_laListButtons.setFromStationName(sNotSet);
	    		m_laListButtons.setFromEntranceName(sNotSet);
	    		m_nDepartureStationId = -1;
	    	}

	    	StationItem arr_sit = m_oGraph.GetStation(m_nArrivalStationId);
	    	if(arr_sit != null && m_laListButtons != null){
	    		m_laListButtons.setToStationName(arr_sit.GetName());
	    		PortalItem pit = arr_sit.GetPortal(m_nArrivalPortalId);
	    		if(pit != null){
	    			m_laListButtons.setToEntranceName(pit.GetName());
	    		}
	    		else{
	    			m_laListButtons.setToEntranceName(sNotSet);
	    			m_nArrivalPortalId = -1;
	    		}
	    	}
	    	else{
	    		m_laListButtons.setToStationName(sNotSet);
	    		m_laListButtons.setToEntranceName(sNotSet);
	    		m_nArrivalStationId = -1;
	    	}
		}

	    if(m_nDepartureStationId != m_nArrivalStationId && m_nDepartureStationId != -1 && m_nArrivalStationId != -1 && m_nDeparturePortalId != -1 && m_nArrivalPortalId != -1){
	    	if(m_oSearchButton != null)
	    		m_oSearchButton.setEnabled(true);
    		if(m_oSearchMenuItem != null)
	    		m_oSearchMenuItem.setEnabled(true);
	    }
	    else{
	    	if(m_oSearchButton != null)
	    		m_oSearchButton.setEnabled(false);
	    	if(m_oSearchMenuItem != null)
	    		m_oSearchMenuItem.setEnabled(false);
	    }

	    if(m_laListButtons != null)
	    	m_laListButtons.notifyDataSetChanged();
	}

	protected void onSearch(){

		final ProgressDialog progressDialog = new ProgressDialog(this);
		progressDialog.setCancelable(false);
		progressDialog.setIndeterminate(true);
		progressDialog.setMessage(getString(R.string.sSearching));
		progressDialog.show();

		new Thread() {

			public void run() {

				//BellmanFordShortestPath
				/*List<DefaultWeightedEdge> path = BellmanFordShortestPath.findPathBetween(mGraph, stFrom.getId(), stTo.getId());
				if(path != null){
					for(DefaultWeightedEdge edge : path) {
		                	Log.d("Route", mmoStations.get(mGraph.getEdgeSource(edge)) + " - " + mmoStations.get(mGraph.getEdgeTarget(edge)) + " " + edge);
		                }
				}*/
				//DijkstraShortestPath
				/*List<DefaultWeightedEdge> path = DijkstraShortestPath.findPathBetween(mGraph, stFrom.getId(), stTo.getId());
				if(path != null){
					for(DefaultWeightedEdge edge : path) {
		                	Log.d("Route", mmoStations.get(mGraph.getEdgeSource(edge)) + " - " + mmoStations.get(mGraph.getEdgeTarget(edge)) + " " + edge);
		                }
				}*/
		        //KShortestPaths
				/*
				KShortestPaths<Integer, DefaultWeightedEdge> kPaths = new KShortestPaths<Integer, DefaultWeightedEdge>(mGraph, stFrom.getId(), 2);
		        List<GraphPath<Integer, DefaultWeightedEdge>> paths = null;
		        try {
		            paths = kPaths.getPaths(stTo.getId());
		            for (GraphPath<Integer, DefaultWeightedEdge> path : paths) {
		                for (DefaultWeightedEdge edge : path.getEdgeList()) {
		                	Log.d("Route", mmoStations.get(mGraph.getEdgeSource(edge)) + " - " + mmoStations.get(mGraph.getEdgeTarget(edge)) + " " + edge);
		                }
		                Log.d("Route", "Weight: " + path.getWeight());
		            }
		        } catch (IllegalArgumentException e) {
		        	e.printStackTrace();
		        }*/

				//YenTopKShortestPaths

			    final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
			    int nMaxRouteCount = prefs.getInt(PreferencesActivity.KEY_PREF_MAX_ROUTE_COUNT, 3);

				List<Path> shortest_paths_list = m_oGraph.GetShortestPaths(m_nDepartureStationId, m_nArrivalStationId, nMaxRouteCount);

				if(shortest_paths_list.size() == 0){
					//MainActivity.this.ErrMessage(R.string.sCannotGetPath);
					//Toast.makeText(MainActivity.this, R.string.sCannotGetPath, Toast.LENGTH_SHORT).show();
					Log.d(TAG, MainActivity.this.getString(R.string.sCannotGetPath));
				}
				else {
			        Intent intentView = new Intent(MainActivity.this, com.nextgis.metroaccess.StationListView.class);
			        //intentView.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);

			        int nCounter = 0;
			        Bundle bundle = new Bundle();
			        bundle.putInt("dep_" + BUNDLE_PORTALID_KEY, m_nDeparturePortalId);
			        bundle.putInt("arr_" + BUNDLE_PORTALID_KEY, m_nArrivalPortalId);

			        for (Path path : shortest_paths_list) {
						ArrayList<Integer> IndexPath = new  ArrayList<Integer>();
						Log.d(TAG, "Route# " + nCounter);
			            for (BaseVertex v : path.get_vertices()) {
			            	IndexPath.add(v.get_id());
			            	Log.d(TAG, "<" + m_oGraph.GetStation(v.get_id()));
			            }
			            intentView.putIntegerArrayListExtra(BUNDLE_PATH_KEY + nCounter, IndexPath);
			            nCounter++;
			        }

			        bundle.putInt(BUNDLE_PATHCOUNT_KEY, nCounter);
			        //bundle.putSerializable(BUNDLE_STATIONMAP_KEY, (Serializable) mmoStations);
			        //bundle.putSerializable(BUNDLE_CROSSESMAP_KEY, (Serializable) mmoCrosses);

					intentView.putExtras(bundle);

			        MainActivity.this.startActivity(intentView);

				}

				progressDialog.dismiss();

			}

		}.start();

	}

	public static String GetDownloadURL(){
		return m_sUrl;
	}

	public static String GetRouteDataDir(){
		return ROUTE_DATA_DIR;
	}

	public static String GetMetaFileName(){
		return META;
	}

	public static String GetRemoteMetaFile(){
		return REMOTE_METAFILE;
	}

	public static MAGraph GetGraph(){
		return m_oGraph;
	}

	public static void SetDownloadURL(String sURL){
		m_sUrl = sURL;
	}

	public void ErrMessage(String sErrMsg){
		Toast.makeText(this, sErrMsg, Toast.LENGTH_SHORT).show();
	}

	public void ErrMessage(int nErrMsg){
		Toast.makeText(this, getString(nErrMsg), Toast.LENGTH_SHORT).show();
	}

    /**
     * This method is called whenever a navigation item in your action bar
     * is selected.
     *
     * @param itemPosition Position of the item clicked.
     * @param itemId       ID of the item clicked.
     * @return True if the event was handled, false otherwise.
     */
    @Override
    public boolean onNavigationItemSelected(int itemPosition, long itemId) {
        if(itemPosition != mCurrentItemPosition) {
            m_oGraph.SetCurrentCity(mCities.get(itemPosition));
            m_laListButtons.clear();
            m_nDepartureStationId = -1;
            m_nArrivalStationId = -1;
            m_nDeparturePortalId = -1;
            m_nArrivalPortalId = -1;

            final SharedPreferences.Editor edit = PreferenceManager.getDefaultSharedPreferences(this).edit();

            edit.putString(PreferencesActivity.KEY_PREF_CITY, m_oGraph.GetCurrentCity());

            edit.commit();
        }
        return true;
    }
}
