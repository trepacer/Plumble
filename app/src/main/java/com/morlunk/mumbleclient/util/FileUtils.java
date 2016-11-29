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


import java.io.File;
import java.io.FileWriter;

import android.text.TextUtils;


public class FileUtils {
	/**
	 * 删除文件或目录
	 *
	 * @param path 文件或目录。
	 * @return true 表示删除成功，否则为失败
	 */
	synchronized public static boolean delete(File path) {
		if (null == path) {
			return true;
		}

		if (path.isDirectory()) {
			File[] files = path.listFiles();
			if (null != files) {
				for (File file : files) {
					if (!delete(file)) {
						return false;
					}
				}
			}
		}
		return !path.exists() || path.delete();
	}

	/**
	 * 创建文件， 如果不存在则创建，否则返回原文件的File对象
	 *
	 * @param path 文件路径
	 * @return 创建好的文件对象, 返回为空表示失败
	 */
	synchronized public static File createFile(String path) {
		if (TextUtils.isEmpty(path)) {
			return null;
		}

		File file = new File(path);
		if (file.isFile()) {
			return file;
		}

		File parentFile = file.getParentFile();
		if (parentFile != null && (parentFile.isDirectory() || parentFile.mkdirs())) {
			try {
				if (file.createNewFile()) {
					return file;
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		return null;
	}

	public static void writeToFile(String content, String filePath) {
		FileWriter fileWriter = null;
		try {
			fileWriter = new FileWriter(filePath, true);
			fileWriter.write(content);
			fileWriter.flush();
		} catch (Throwable t) {
			t.printStackTrace();
		} finally {
			if (fileWriter != null) {
				try {
					fileWriter.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}
}