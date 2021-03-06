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


public interface Constants {

    public final static String TAG = "metro4all";

    public final static String META = "meta.json";
    final static String REMOTE_METAFILE = "remotemeta_v2.json";
    final static String ROUTE_DATA_DIR = "rdata_v2";

    public final static String CSV_CHAR = ";";

    final static String BUNDLE_MSG_KEY = "msg";
    final static String BUNDLE_PAYLOAD_KEY = "json";
    final static String BUNDLE_ERRORMARK_KEY = "error";
    final static String BUNDLE_EVENTSRC_KEY = "eventsrc";
    final static String BUNDLE_ENTRANCE_KEY = "in";
    final static String BUNDLE_PATHCOUNT_KEY = "pathcount";
    final static String BUNDLE_PATH_KEY = "path_";
    final static String BUNDLE_STATIONMAP_KEY = "stationmap";
    final static String BUNDLE_CROSSESMAP_KEY = "crossmap";
    final static String BUNDLE_STATIONID_KEY = "stationid";
    final static String BUNDLE_PORTALID_KEY = "portalid";
    final static String BUNDLE_METAMAP_KEY = "metamap";

    public final static int MENU_SEARCH = 3;
    public final static int MENU_SETTINGS = 4;
    public final static int MENU_ABOUT = 5;

    public final static int DEPARTURE_RESULT = 1;
    public final static int ARRIVAL_RESULT = 2;
    public final static int PREF_RESULT = 3;
    public final static int PORTAL_MAP_RESULT = 4;
    public final static int MAX_RECENT_ITEMS = 10;

    public final static String PARAM_SEL_STATION_ID = "SEL_STATION_ID";
    public final static String PARAM_SEL_PORTAL_ID = "SEL_PORTAL_ID";
    public final static String PARAM_PORTAL_DIRECTION = "PORTAL_DIRECTION";
}
