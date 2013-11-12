package com.nextgis.metro4all.GoodGuy.utils.db;

import android.content.Context;
import android.database.Cursor;

/**
 * Так как класс DBHelper генерируется автоматически из скрипта в нем как
 * правило нехватает нужных функций, таких, как например поиск строки в таблице
 * по полю. Данный класс как раз предназначен для того, чтобы во-первых добавить
 * нужные функции, а во вторых - связать объекты с хранилищем.
 * 
 * @author valetin
 * 
 */
public class DBWrapper extends DBHelper {

	public DBWrapper(Context context) {
		super(context);
	}

	public Cursor getLineByLineId(int id_line) {
		Cursor res = mDb.query(LINES_TABLE, new String[] { ROW_ID,
				LINES_ID_LINE_COLUMN, LINES_NAME_COLUMN, LINES_NAME_EN_COLUMN,
				LINES_COLOR_COLUMN }, LINES_ID_LINE_COLUMN + " = " + id_line,
				null, null, null, null);
		if (res != null) {
			res.moveToFirst();
		}
		return res;
	}

	public Cursor getStationsyLineId(int id_line) {
		Cursor res = mDb.query(STATIONS_TABLE, new String[] { ROW_ID,
				STATIONS_ID_STATION_COLUMN, STATIONS_ID_LINE_COLUMN,
				STATIONS_NAME_COLUMN, STATIONS_NAME_EN_COLUMN,
				STATIONS_LAT_COLUMN, STATIONS_LON_COLUMN },
				STATIONS_ID_LINE_COLUMN + " = " + id_line, null, null, null,
				null);
		if (res != null) {
			res.moveToFirst();
		}
		return res;
	}

	public Cursor getCellDataByLocation(int cid, int lac) {
		Cursor res = mDb.query(CELLDATA_TABLE, new String [] {
				ROW_ID,
                CELLDATA_DATE_COLUMN,
                CELLDATA_ID_STATION_COLUMN,
                CELLDATA_ID_LINE_COLUMN,
                CELLDATA_CELL_CID_COLUMN,
                CELLDATA_CELL_LAC_COLUMN,
                CELLDATA_CELL_HASH_COLUMN,
                CELLDATA_CELL_NAME_COLUMN,
                CELLDATA_CELL_SIGNAL_COLUMN,
                CELLDATA_CELL_NEIGHBORS_COUNT_COLUMN,
                CELLDATA_CELL_NEIGHBORS_DATA_COLUMN,
                CELLDATA_CELL_GEO_LOCATION_COLUMN
		}, CELLDATA_CELL_CID_COLUMN + "=" + cid + " AND " + DBHelper.CELLDATA_CELL_LAC_COLUMN + "=" + lac, null, null, null, null);
		if(res != null) {
			res.moveToFirst();
		}
		return res;
	}

}
