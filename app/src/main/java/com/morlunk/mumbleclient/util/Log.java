/**
 * Copyright (C) 2010-2012 Regis Montoya (aka r3gis - www.r3gis.fr)
 * This file is part of CSipSimple.
 *
 *  CSipSimple is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *  If you own a pjsip commercial license you can also redistribute it
 *  and/or modify it under the terms of the GNU Lesser General Public License
 *  as an android library.
 *
 *  CSipSimple is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with CSipSimple.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.morlunk.mumbleclient.util;


import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class Log {
	private static int logLevel = 5;

	private static Boolean MYLOG_SWITCH=true; // 日志文件总开关
	private static Boolean MYLOG_WRITE_TO_FILE=true;// 日志写入文件开关
	private static char MYLOG_TYPE='v';// 输入日志类型，w代表只输出告警信息等，v代表输出所有信息
	private static String MYLOG_PATH_SDCARD_DIR="/sdcard/voice/";// 日志文件在sdcard中的路径
	private static int SDCARD_LOG_FILE_SAVE_DAYS = 0;// sd卡中日志文件的最多保存天数
	private static String MYLOGFILEName = ".log";// 本类输出的日志文件名称
	private static SimpleDateFormat myLogSdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");// 日志的输出格式
	private static SimpleDateFormat logfile = new SimpleDateFormat("yyyyMMdd");// 日志文件格式
	/**
	 * Change current logging level
	 * @param level new log level 1 <= level <= 6
	 */
	public static void setLogLevel(int level) {
		logLevel = level;
	}

	/**
	 * Get the current log level
	 * @return the log level
	 */
	public static int getLogLevel() {
		return logLevel;
	}

	/**
	 * Log verbose
	 * @param tag Tag for this log
	 * @param msg Msg for this log
	 */
	public static void v(String tag, String msg) {
		if(logLevel >= 5) {
			android.util.Log.v(tag, msg);
		}
	}

	/**
	 * Log verbose
	 * @param tag Tag for this log
	 * @param msg Msg for this log
	 * @param tr Error to serialize in log
	 */
	public static void v(String tag, String msg, Throwable tr) {
		if(logLevel >= 5) {
			android.util.Log.v(tag, msg, tr);
		}
	}

	/**
	 * Log debug
	 * @param tag Tag for this log
	 * @param msg Msg for this log
	 */
	public static void d(String tag, String msg) {
		if(logLevel >= 4) {
			android.util.Log.d(tag, msg);
		}
	}

	/**
	 * Log debug
	 * @param tag Tag for this log
	 * @param msg Msg for this log
	 * @param tr Error to serialize in log
	 */
	public static void d(String tag, String msg, Throwable tr) {
		if(logLevel >= 4) {
			android.util.Log.d(tag, msg, tr);
		}
	}

	/**
	 * Log info
	 * @param tag Tag for this log
	 * @param msg Msg for this log
	 */
	public static void i(String tag, String msg) {
		if(logLevel >= 3) {
			android.util.Log.i(tag, msg);
		}
	}

	/**
	 * Log info
	 * @param tag Tag for this log
	 * @param msg Msg for this log
	 * @param tr Error to serialize in log
	 */
	static void i(String tag, String msg, Throwable tr) {
		if(logLevel >= 3) {
			android.util.Log.i(tag, msg, tr);
		}
	}

	/**
	 * Log warning
	 * @param tag Tag for this log
	 * @param msg Msg for this log
	 */
	public static void w(String tag, String msg) {
		if(logLevel >= 2) {
			android.util.Log.w(tag, msg);
		}
	}

	/**
	 * Log warning
	 * @param tag Tag for this log
	 * @param msg Msg for this log
	 * @param tr Error to serialize in log
	 */
	public static void w(String tag, String msg, Throwable tr) {
		if(logLevel >= 2) {
			android.util.Log.w(tag, msg, tr);
		}
	}

	/**
	 * Log error
	 * @param tag Tag for this log
	 * @param msg Msg for this log
	 */
	public static void e(String tag, String msg) {
		android.util.Log.e(tag, msg);
	}

	/**
	 * Log error
	 * @param tag Tag for this log
	 * @param msg Msg for this log
	 * @param tr Error to serialize in log
	 */
	public static void e(String tag, String msg, Throwable tr) {
		if(logLevel >= 1) {
			android.util.Log.e(tag, msg, tr);
		}
	}
	public static void getClassInfo(){
		String clazzName2 = new Throwable().getStackTrace()[1].getClassName();
		String methodName2 = new Throwable().getStackTrace()[1].getMethodName();
		int lineName2 = new Throwable().getStackTrace()[1].getLineNumber();
		Log.e("ClassInfo", clazzName2+"\t"+methodName2+"\t"+lineName2);
//	    String content = clazzName2+"\t"+methodName2+"\t"+lineName2;
//	    writeFile(content);
	}
	public static void getClassInfo(String param){
		String clazzName2 = new Throwable().getStackTrace()[1].getClassName();
		String methodName2 = new Throwable().getStackTrace()[1].getMethodName();
		int lineName2 = new Throwable().getStackTrace()[1].getLineNumber();
		Log.e("ClassInfo", clazzName2+"\t"+methodName2+"\t"+lineName2+"\t "+param);
//	    String content = clazzName2+"\t"+methodName2+"\t"+lineName2;
//	    writeFile(content);
	}

	public static void writeFile(String content){
		Date nowtime = new Date();
		String needWriteFiel = logfile.format(nowtime);
		String filePath = "";
		File file = new File(MYLOG_PATH_SDCARD_DIR, needWriteFiel + MYLOGFILEName);
		if(!file.exists())
		{
			filePath = MYLOG_PATH_SDCARD_DIR+needWriteFiel + MYLOGFILEName;
			file = FileUtils.createFile(filePath);
		}
		FileUtils.writeToFile(content, filePath);
		try {
			FileWriter filerWriter = new FileWriter(file, true);//后面这个参数代表是不是要接上文件中原来的数据，不进行覆盖
			BufferedWriter bufWriter = new BufferedWriter(filerWriter);
			bufWriter.write(content);
			bufWriter.newLine();
			bufWriter.close();
			filerWriter.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}




}
