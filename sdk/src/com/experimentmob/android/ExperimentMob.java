package com.experimentmob.android;

import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;
import bsh.EvalError;
import bsh.Interpreter;

import com.google.gson.Gson;

public class ExperimentMob {

	public static final String ABTEST_SHARED_PREF_NAME = "AB_TESTING_SHARED_PREF";
	private OnABTesting onAbTesting = null;
	private Editor sharedPrefEditor;
	private SharedPreferences sharedPref;
	private String TAG = "ABTesting";
	private static ExperimentMob instance = null;
	private static Context context;
	private Object classObj = null;
	private final String COHORT = "COHORT";
	private String appABJsonString = null;
	private ExperimentMobPojo openABTestResponse;
	private String userId;
	private String partOfCohortString;
	private LocationPojo locationPojo;

	public interface OnABTesting {
		void onABTestingInitDone(List<String> experimentIds);

		void onABTestingFileDone(String filename);
	}

	public static ExperimentMob getInstance() {
		if (instance == null) {
			return instance = new ExperimentMob();
		}
		return instance;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public static String getABTestingFileBasePath(Context context) {
		File folder = context.getExternalFilesDir(null);
		if (folder == null)
			return null;
		Log.d("ABTesting", "getABTestingFileBasePath() : " + folder.getAbsolutePath() + "/abtesting");
		return folder.getAbsolutePath() + "/abtesting/";
	}

	public void init(Context context, final Object obj, OnABTesting onABTesting) {
		this.onAbTesting = onABTesting;
		this.classObj = obj;
		ExperimentMob.context = context;

		sharedPrefEditor = context.getSharedPreferences(ABTEST_SHARED_PREF_NAME, Context.MODE_PRIVATE).edit();
		sharedPref = context.getSharedPreferences(ABTEST_SHARED_PREF_NAME, Context.MODE_PRIVATE);

		setFieldValues(null, null, classObj);

		new FileUtils(context).copyFileOrDir("abtesting");

		new ABTestingTask().execute();
	}

	public void init(Context context, final Object obj, String userId, OnABTesting onABTesting) {
		this.onAbTesting = onABTesting;
		this.classObj = obj;
		ExperimentMob.context = context;
		this.userId = userId;

		sharedPrefEditor = context.getSharedPreferences(ABTEST_SHARED_PREF_NAME, Context.MODE_PRIVATE).edit();
		sharedPref = context.getSharedPreferences(ABTEST_SHARED_PREF_NAME, Context.MODE_PRIVATE);

		setFieldValues(null, null, classObj);

		new FileUtils(context).copyFileOrDir("abtesting");

		new ABTestingTask().execute();
	}

	class ABTestingTask extends AsyncTask<Void, Void, Void> {
		
		List<String> experimentIds = new ArrayList<String>();

		@Override
		protected Void doInBackground(Void... params) {
			try {
				String basePath = context.getString(FileUtils.getResourceIdByName(context, "string", "experimentmob_basepath"));
				String appId = context.getString(FileUtils.getResourceIdByName(context, "string", "experimentmob_appid"));
				String url = basePath + "apis/" + appId + "/abtesting";
				if (appABJsonString == null || appABJsonString.equals("")) {
					appABJsonString = FileUtils.getHttpResponse(url, null);
					Log.d(TAG, "abTestingString : " + appABJsonString);
				}

				Map<String, Object> fields = new HashMap<String, Object>();
				Map<String, Object> defaultFields = new HashMap<String, Object>();
				openABTestResponse = new Gson().fromJson((appABJsonString), ExperimentMobPojo.class);
				getLocationDetails();

				for (Experiment experiment : openABTestResponse.experiments) {
					if (experiment != null) {
						boolean isEvaluatedAlready = false;
						if (evaluateExpression(experiment.expression, experiment.id)) {
							experimentIds.add(experiment.id);
							Log.d(TAG, "Experiment Expression : " + experiment.expression);
							Log.d(TAG, "Experiment true for id : " + experiment.id);
							if (sharedPref.getBoolean("ExperimentId|" + experiment.id, false)) {
								Log.d(TAG, "Experiment already evaluated");
								isEvaluatedAlready = true;
							}
							for (FieldContainer fieldContainer : experiment.fields) {
								if (fieldContainer.type.equals("FILE")) {
									if (!isEvaluatedAlready) {
										downloadFileAndWrite(fieldContainer);
									}
								} else {
									fields.put(fieldContainer.name, fieldContainer.value);
									Log.d(TAG, "FieldContainerName : " + fieldContainer.name + " ; Value : " + fieldContainer.value);
									try {
										if (fieldContainer.type.equals("NUMBER")) {
											float floatValue = Float.valueOf(String.valueOf((Double) fieldContainer.value));
											sharedPrefEditor.putFloat(fieldContainer.name + "_mobiuswrapper", floatValue).commit();
										} else if (fieldContainer.type.equals("INTEGER")) {
											float floatValue = Float.valueOf(String.valueOf((Double) fieldContainer.value));
											sharedPrefEditor.putFloat(fieldContainer.name + "_mobiuswrapper", floatValue).commit();
										} else if (fieldContainer.type.equals("BOOLEAN")) {
											sharedPrefEditor.putBoolean(fieldContainer.name + "_mobiuswrapper", (Boolean) fieldContainer.value)
													.commit();
										} else if (fieldContainer.type.equals("STRING")) {
											sharedPrefEditor.putString(fieldContainer.name + "_mobiuswrapper", (String) fieldContainer.value)
													.commit();
										}
									} catch (Exception e) {
										e.printStackTrace();
									}
								}
								sharedPrefEditor.putBoolean("ExperimentId|" + experiment.id, true).commit();
							}
						}
					}
				}

				for (Map.Entry<String, Object> entry : fields.entrySet()) {
					Log.d(TAG, "Experiment Fields : " + entry.getKey() + "/" + entry.getValue());
				}
				setFieldValues(fields, defaultFields, classObj);
			} catch (Exception e) {
				e.printStackTrace();
			}

			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			onAbTesting.onABTestingInitDone(experimentIds);
			super.onPostExecute(result);
		}

	}

	@SuppressWarnings("rawtypes")
	private void setFieldValues(Map<String, Object> fields, Map<String, Object> defaultFields, Object obj) {
		try {
			Class cl = obj.getClass();

			// Checking all the fields for annotations
			for (Field field : cl.getDeclaredFields()) {
				try {
					// Processing all the annotations on a single field
					for (Annotation a : field.getAnnotations()) {
						if (a.annotationType() == ExperimentMobVariable.class) {
							field.setAccessible(true);
							// Set values which were present at an earlier stage
							if (sharedPref.contains(field.getName())) {
								if (field.getType().equals(Integer.TYPE)) {
									Log.d(TAG, "Local fields : " + field.getName());
									field.set(obj, sharedPref.getInt(field.getName(), 0));
								} else if (field.getType().equals(Double.TYPE)) {
									Log.d(TAG, "Local fields : " + field.getName());
									field.set(obj, sharedPref.getFloat(field.getName(), 0));
								} else if (field.getType().equals(Float.TYPE)) {
									Log.d(TAG, "Local fields : " + field.getName());
									field.set(obj, sharedPref.getFloat(field.getName(), 0));
								} else if (field.getType().equals(Long.TYPE)) {
									Log.d(TAG, "Local fields : " + field.getName());
									field.set(obj, sharedPref.getLong(field.getName(), 0));
								} else if (field.getType().equals(Boolean.TYPE)) {
									Log.d(TAG, "Local fields : " + field.getName());
									field.set(obj, sharedPref.getBoolean(field.getName(), false));
								} else {
									Log.d(TAG, "Local fields : " + field.getName());
									field.set(obj, sharedPref.getString(field.getName(), ""));
								}
							}

							if (defaultFields == null) {
								break;
							}

							// Set fields with default values, this is a set for
							// all fields. If any change has been made to the
							// defaults then its gets set here
							for (Map.Entry<String, Object> entry : defaultFields.entrySet()) {
								try {
									if (entry.getKey().equals(field.getName())) {
										if (field.getType().equals(Integer.TYPE)) {
											Log.d(TAG, "Default fields : " + field.getName());
											int intValue = (int) Math.floor((Double) entry.getValue());
											sharedPrefEditor.putInt(field.getName(), intValue).commit();
											field.setInt(obj, intValue);
										} else if (field.getType().equals(Double.TYPE)) {
											Log.d(TAG, "Default fields : " + field.getName());
											sharedPrefEditor.putFloat(field.getName(), ((Double) entry.getValue()).floatValue()).commit();
											field.setDouble(obj, (Double) entry.getValue());
										} else if (field.getType().equals(Float.TYPE)) {
											Log.d(TAG, "Default fields : " + field.getName());
											Log.d(TAG, "Float Key " + field.getName());
											float floatValue = ((Double) entry.getValue()).floatValue();
											sharedPrefEditor.putFloat(field.getName(), floatValue).commit();
											field.setFloat(obj, floatValue);
										} else if (field.getType().equals(Long.TYPE)) {
											Log.d(TAG, "Default fields : " + field.getName());
											sharedPrefEditor.putLong(field.getName(), (Long) entry.getValue()).commit();
											field.setLong(obj, (Long) entry.getValue());
										} else if (field.getType().equals(Boolean.TYPE)) {
											Log.d(TAG, "Default fields : " + field.getName());
											sharedPrefEditor.putBoolean(field.getName(), (Boolean) entry.getValue()).commit();
											field.setBoolean(obj, (Boolean) entry.getValue());
										} else {
											Log.d(TAG, "Default fields : " + field.getName());
											sharedPrefEditor.putString(field.getName(), (String) entry.getValue()).commit();
											field.set(obj, entry.getValue());
										}

									}
								} catch (Exception e) {
									e.printStackTrace();
								}
							}

							if (fields == null) {
								break;
							}

							// Previously, fields are set with values
							// which were present locally and with the default
							// ones. If any fields have values determined as a
							// result of any experiments, then their
							// corresponding values as per the experiment is set
							// to the fields.
							for (Map.Entry<String, Object> entry : fields.entrySet()) {
								field.setAccessible(true);
								try {
									if (entry.getKey().equals(field.getName())) {
										if (field.getType().equals(Integer.TYPE)) {
											int intValue = (int) Math.floor((Double) entry.getValue());
											sharedPrefEditor.putInt(field.getName(), intValue).commit();
											field.setInt(obj, intValue);
										} else if (field.getType().equals(Double.TYPE)) {
											Log.d(TAG, "Default fields : " + field.getName());
											sharedPrefEditor.putFloat(field.getName(), ((Double) entry.getValue()).floatValue()).commit();
											field.setDouble(obj, (Double) entry.getValue());
										} else if (field.getType().equals(Float.TYPE)) {
											Log.d(TAG, "Float Key " + field.getName());
											float floatValue = ((Double) entry.getValue()).floatValue();
											sharedPrefEditor.putFloat(field.getName(), floatValue).commit();
											field.setFloat(obj, floatValue);
										} else if (field.getType().equals(Long.TYPE)) {
											sharedPrefEditor.putLong(field.getName(), (Long) entry.getValue()).commit();
											field.setLong(obj, (Long) entry.getValue());
										} else if (field.getType().equals(Boolean.TYPE)) {
											Log.d(TAG, "Default fields : " + field.getName());
											sharedPrefEditor.putBoolean(field.getName(), (Boolean) entry.getValue()).commit();
											field.setBoolean(obj, (Boolean) entry.getValue());
										} else {
											sharedPrefEditor.putString(field.getName(), (String) entry.getValue()).commit();
											field.set(obj, entry.getValue());
										}

									}
								} catch (Exception e) {
									e.printStackTrace();
								}
							}
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private ExperimentMob() {

	}

	private void downloadFileAndWrite(final FieldContainer fileExperiment) {
		if (fileExperiment == null) {
			return;
		}
		new AsyncTask<String, Void, Boolean>() {

			@Override
			protected Boolean doInBackground(String... params) {
				try {
					File folder = new File(getABTestingFileBasePath(context));
					Log.d(TAG, "folderPath : " + folder.getAbsolutePath());
					if (!folder.exists()) {
						Log.d(TAG, "Base Folder doesn't exist");
						if (!folder.mkdir()) {
							return false;
						}
					}
					FileUtils.downloadFromUrl((String) fileExperiment.value, getABTestingFileBasePath(context) + "/" + fileExperiment.name);
					return true;
				} catch (Exception e) {
					e.printStackTrace();
					return false;
				}
			}

			@Override
			protected void onPostExecute(Boolean result) {
				super.onPostExecute(result);
				if (result) {
					onAbTesting.onABTestingFileDone(fileExperiment.name);
				}
			}

		}.execute("");
	}

	private boolean evaluateExpression(String expression, String exprId) {
		Interpreter interpreter = new Interpreter();
		try {
			String appVersion = "1";
			String osVersion = "4.0";
			String city = "U/A";
			String country = "U/A";
			String region = "U/A";
			try {
				city = locationPojo.city;
				country = locationPojo.countryCode;
				region = locationPojo.regionName;
			} catch (Exception e1) {
				e1.printStackTrace();
			}
			int randNumber = 100;
			try {
				appVersion = String.valueOf(context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionCode);
				osVersion = getAndroidVersionName();
				Random r = new Random();
				randNumber = r.nextInt(101 - 1) + 1;
			} catch (NameNotFoundException e) {
				e.printStackTrace();
			}
			randNumber = context.getSharedPreferences(ABTEST_SHARED_PREF_NAME, Context.MODE_PRIVATE).getInt(exprId, randNumber);
			context.getSharedPreferences(ABTEST_SHARED_PREF_NAME, Context.MODE_PRIVATE).edit().putInt(exprId, randNumber).commit();
			expression = "result=" + expression;
			if (expression.contains(COHORT)) {
				String customRules = getCustomRulesString(expression, COHORT, "\"");
				Log.d(TAG, "CustomRuleString : " + customRules);
				String customKeyContents = getStringBetween(customRules, "\"", "\"");
				Log.d(TAG, "COHORT : " + customKeyContents);
				customKeyContents = customKeyContents.replace("\"", "");
				String[] customKeys = customKeyContents.split(",");

				boolean isCustomKeyEvaluated = false;

				for (String customKey : customKeys) {
					Log.d(TAG, "CustomRules : " + customKey);
					isCustomKeyEvaluated = evaluateCohorts(customKey);
					if (!isCustomKeyEvaluated) {
						break;
					}
				}
				expression = expression.replace(customRules, String.valueOf(isCustomKeyEvaluated));
			}
			// "GU_CUSTOMKEY=\"" + "true" + "\";" +
			expression = "APP_VERSION=" + appVersion + ";" + "OS_VERSION=\"" + osVersion + "\";" + "CITY=\"" + city + "\";" + "COUNTRY=\"" + country
					+ "\";" + "REGION=\"" + region + "\";" + "PERC_USERS=" + randNumber + ";" + expression;

			Log.d(TAG, "expression : " + expression);
			interpreter.eval(expression);
			return (Boolean) interpreter.get("result");
		} catch (EvalError e) {
			Log.d(TAG, "Error : " + e.getScriptStackTrace());
			e.printStackTrace();
		}
		return false;
	}

	private void getLocationDetails() {
		try {
			String url = "http://ip-api.com/json";
			String locationString = null;
			if (locationPojo == null) {
				locationString = FileUtils.getHttpResponse(url, null);
				locationPojo = new Gson().fromJson(locationString, LocationPojo.class);
				sharedPrefEditor.putString("locationDetails", locationString).commit();
			}
		} catch (Exception e) {
			e.printStackTrace();
			String locationString = sharedPref.getString("locationDetails", null);
			if (locationString != null) {
				locationPojo = new Gson().fromJson(locationString, LocationPojo.class);
			}
		}
	}

	private boolean evaluateCohorts(String cohortName) {
		String basePath = context.getString(FileUtils.getResourceIdByName(context, "string", "experimentmob_basepath"));
		String appId = context.getString(FileUtils.getResourceIdByName(context, "string", "experimentmob_appid"));
		String url = basePath + "apis/" + appId + "/partOfCohort?userId=" + userId + "&cohortId=" + cohortName;
		Log.d(TAG,"partOfCohortString" + partOfCohortString);
		if (partOfCohortString == null || partOfCohortString.equals("")) {
			partOfCohortString = FileUtils.getHttpResponse(url, null);
		}
		if (partOfCohortString != null && !partOfCohortString.equals("")) {
			try {
				JSONObject jobj = new JSONObject(partOfCohortString);
				boolean isPartOfCohort = jobj.getBoolean("existsInCohort");
				if (isPartOfCohort) {
					return true;
				}
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
		return false;
	}

	private String getAndroidVersionName() {
		String[] mapper = new String[] { "ANDROID BASE", "ANDROID BASE 1.1", "CUPCAKE", "DONUT", "ECLAIR", "ECLAIR_0_1", "ECLAIR_MR1", "FROYO",
				"GINGERBREAD", "GINGERBREAD_MR1", "HONEYCOMB", "HONEYCOMB", "HONEYCOMB", "ICE_CREAM_SANDWICH", "ICECREAMSANDWICH", "JELLYBEAN",
				"JELLYBEAN", "JELLYBEAN", "KITKAT", "KITKAT", "LOLLIPOP" };
		int index = Build.VERSION.SDK_INT - 1;
		String versionName = index < mapper.length ? mapper[index] : "UNKNOWN_VERSION";
		return versionName;
	}

	private String getCustomRulesString(String string, String str1, String str2) {
		Pattern pattern = Pattern.compile(str1 + "(.*?)" + str2 + "(.*?)" + str2);
		Matcher matcher = pattern.matcher(string);
		while (matcher.find()) {
			return matcher.group();
		}
		return null;
	}

	private String getStringBetween(String string, String str1, String str2) {
		Pattern pattern = Pattern.compile(str1 + "(.*?)" + str2);
		Matcher matcher = pattern.matcher(string);
		while (matcher.find()) {
			return matcher.group();
		}
		return null;
	}

}
