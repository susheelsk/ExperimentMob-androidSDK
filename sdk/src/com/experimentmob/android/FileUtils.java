package com.experimentmob.android;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.ByteArrayBuffer;
import org.apache.http.util.EntityUtils;

import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.util.Log;

public class FileUtils {

	private Context context;
	private String TARGET_BASE_PATH;
	private String TAG = getClass().getCanonicalName();

	public FileUtils(Context context) {
		this.context = context;
		File file = context.getExternalFilesDir(null);
		if (file != null) {
			TARGET_BASE_PATH = file.getAbsolutePath() + "/";
		}
	}

	public void copyFileOrDir(String path) {
		AssetManager assetManager = context.getAssets();
		if (TARGET_BASE_PATH == null) {
			return;
		}
		String assets[] = null;
		try {
			Log.i(TAG, "copyFileOrDir() " + path);
			assets = assetManager.list(path);
			if (assets.length == 0) {
				copyFile(path);
			} else {
				String fullPath = TARGET_BASE_PATH + path;
				Log.i(TAG, "path=" + fullPath);
				File dir = new File(fullPath);
				if (!dir.exists() && !path.startsWith("images")
						&& !path.startsWith("sounds")
						&& !path.startsWith("webkit")) {
					if (!dir.mkdirs()) {
						Log.i(TAG, "could not create dir " + fullPath);
					}
				}
				for (int i = 0; i < assets.length; ++i) {
					String p;
					if (path.equals(""))
						p = "";
					else
						p = path + "/";

					if (!path.startsWith("images")
							&& !path.startsWith("sounds")
							&& !path.startsWith("webkit"))
						copyFileOrDir(p + assets[i]);
				}
			}
		} catch (IOException ex) {
			Log.e(TAG, "I/O Exception", ex);
		}
	}

	private void copyFile(String filename) {
		AssetManager assetManager = context.getAssets();

		InputStream in = null;
		OutputStream out = null;
		String newFileName = null;
		try {
			Log.i(TAG, "copyFile() " + filename);
			in = assetManager.open(filename);
			boolean isFileExists = context.getSharedPreferences(
					ExperimentMob.ABTEST_SHARED_PREF_NAME, Context.MODE_PRIVATE)
					.getBoolean("openab_file_" + filename, false);
			if (isFileExists) {
				Log.i(TAG, "File Exists already : " + filename);
				return;
			}
			if (filename.endsWith(".jpg")) // extension was added to avoid
											// compression on APK file
				newFileName = TARGET_BASE_PATH
						+ filename.substring(0, filename.length() - 4);
			else
				newFileName = TARGET_BASE_PATH + filename;
			out = new FileOutputStream(newFileName);
			Log.i(TAG, "File path: " + newFileName);

			byte[] buffer = new byte[1024];
			int read;
			while ((read = in.read(buffer)) != -1) {
				out.write(buffer, 0, read);
			}
			in.close();
			in = null;
			out.flush();
			out.close();
			out = null;
			context.getSharedPreferences(ExperimentMob.ABTEST_SHARED_PREF_NAME,
					Context.MODE_PRIVATE).edit()
					.putBoolean("openab_file_" + filename, true).commit();
		} catch (Exception e) {
			Log.e(TAG, "Exception in copyFile() of " + newFileName);
			Log.e(TAG, "Exception in copyFile() " + e.toString());
		}

	}

	public static void downloadFromUrl(String downloadUrl, String fileName) {

		try {
			URL url = new URL(downloadUrl); // you can write here any link
			File file = new File(fileName);

			long startTime = System.currentTimeMillis();
			Log.d("DownloadManager", "download begining");
			Log.d("DownloadManager", "download url:" + url);
			Log.d("DownloadManager", "downloaded file name:" + fileName);

			/* Open a connection to that URL. */
			URLConnection ucon = url.openConnection();

			/*
			 * Define InputStreams to read from the URLConnection.
			 */
			InputStream is = ucon.getInputStream();
			BufferedInputStream bis = new BufferedInputStream(is);

			/*
			 * Read bytes to the Buffer until there is nothing more to read(-1).
			 */
			ByteArrayBuffer baf = new ByteArrayBuffer(5000);
			int current = 0;
			while ((current = bis.read()) != -1) {
				baf.append((byte) current);
			}

			/* Convert the Bytes read to a String. */
			FileOutputStream fos = new FileOutputStream(file);
			fos.write(baf.toByteArray());
			fos.flush();
			fos.close();
			Log.d("DownloadManager",
					"download ready in"
							+ ((System.currentTimeMillis() - startTime) / 1000)
							+ " sec");

		} catch (IOException e) {
			Log.d("DownloadManager", "Error: " + e);
		}

	}
	
	public static String getHttpResponse(String url, List<BasicNameValuePair> params) {
		String responseString;
		try {
			HttpClient httpclient = new DefaultHttpClient();

			if (params != null) {
				List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
				for (BasicNameValuePair pair : params) {
					nameValuePairs.add(pair);
				}

				String paramString = URLEncodedUtils.format(nameValuePairs, "utf-8");
				url += paramString;
			}
			HttpGet httpGet = new HttpGet(url);

			HttpResponse response = httpclient.execute(httpGet);
			responseString = EntityUtils.toString(response.getEntity(), "UTF-8");
			Log.d("HttpClient" + url, "Response : " + responseString);
			return responseString;
		} catch (Exception e) {
			e.printStackTrace();
		}

		return "";
	}
	
	public static int getResourceIdByName(Context context, String className, String name) {
		String packageName = context.getPackageName();
		Resources res = context.getResources();
		int resource = res.getIdentifier(name, className, packageName);
		return resource;
	}

}