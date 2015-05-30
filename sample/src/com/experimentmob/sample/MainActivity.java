package com.experimentmob.sample;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.TableLayout.LayoutParams;
import android.widget.TableRow;
import android.widget.TextView;

import com.example.experimentmob.R;
import com.experimentmob.android.ExperimentMob;
import com.experimentmob.android.ExperimentMob.OnABTesting;
import com.experimentmob.android.ExperimentMobVariable;

public class MainActivity extends Activity implements OnClickListener {

	String TAG = "ABTestingActivity";
	private Button testButton;

	@ExperimentMobVariable
	private int id1 = 1;
	@ExperimentMobVariable
	private float id2 = 4.5f;
	@ExperimentMobVariable
	private double id3 = 4.5;
	@ExperimentMobVariable
	private boolean id4 = false;
	@ExperimentMobVariable
	private double id5 = 56.3;
	@ExperimentMobVariable
	private String id6 = "#ff0000";

	private Map<String, String> variablesMap = new HashMap<String, String>();

	public void onCreate(Bundle savedInstanceState) {
		Log.d(TAG, "onCreate...");

		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		testButton = (Button) findViewById(R.id.abTestButton);

		MainActivity.this.getWindow().getDecorView().setBackgroundColor(Color.parseColor(id6));

		testButton.setOnClickListener(this);

		ExperimentMob.getInstance().setUserId("susheel");
		
		ExperimentMob.getInstance().init(this,this,"susheel", new OnABTesting() {

			@Override
			public void onABTestingInitDone(List<String> experimentIds) {
				if(experimentIds!=null) {
					for(String experimentId : experimentIds) {
						Log.d(TAG,"Part of experiment : "+experimentId);
					}
				}
				MainActivity.this.getWindow().getDecorView().setBackgroundColor(Color.parseColor(id6));
				populateVariablesIntoMap();
				drawTableRows();
			}

			@Override
			public void onABTestingFileDone(String filename) {
				Log.d(TAG, "onABTestingFileDone : " + filename);
				loadImageView();
			}
		});
		MainActivity.this.getWindow().getDecorView().setBackgroundColor(Color.parseColor(id6));

		loadImageView();

		populateVariablesIntoMap();

		drawTableRows();
	}

	@Override
	public void onBackPressed() {
		super.onBackPressed();
		finish();
	}

	private void loadImageView() {
		ImageView imageView = (ImageView) findViewById(R.id.sampleImageView);
		File imgFile = new File(ExperimentMob.getABTestingFileBasePath(this) + "pink");
		Log.d(TAG, "imgFile : " + imgFile.getAbsolutePath());
		if (imgFile.exists()) {
			Log.d(TAG, "Image Exists");
			Bitmap myBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
			imageView.setImageBitmap(myBitmap);
		} else {
			Log.d(TAG, "Image not Exists");
		}
	}

	private void populateVariablesIntoMap() {
		variablesMap.clear();
		variablesMap.put("id1", String.valueOf(id1));
		variablesMap.put("id2", String.valueOf(id2));
		variablesMap.put("id3", String.valueOf(id3));
		variablesMap.put("id4", String.valueOf(id4));
		variablesMap.put("id5", String.valueOf(id5));
		variablesMap.put("id6", String.valueOf(id6));
	}

	@Override
	public void onClick(View view) {
		int id = view.getId();
		switch (id) {
		case R.id.abTestButton:
			Log.d(TAG, "id1 : " + id1);
			Log.d(TAG, "id2 : " + id2);
			Log.d(TAG, "id3 : " + id3);
			Log.d(TAG, "id4 : " + id4);
			Log.d(TAG, "id5 : " + id5);
			Log.d(TAG, "id6 : " + id6);
			break;
		}
	}

	@SuppressWarnings("deprecation")
	private void drawTableRows() {

		TableLayout tbl = (TableLayout) findViewById(R.id.variableTable);
		tbl.removeAllViews();
		int rowNum = -1;
		for (String key : variablesMap.keySet()) {

			String value = variablesMap.get(key);
			rowNum++;
			TableRow tr = new TableRow(this);

			tr.setLayoutParams(new TableLayout.LayoutParams(TableLayout.LayoutParams.FILL_PARENT, TableLayout.LayoutParams.FILL_PARENT, 1.0f));

			TextView tvc1 = new TextView(this);
			TextView tvc2 = new TextView(this);

			tvc1.setBackgroundResource(R.drawable.cell_shape);
			tvc1.append(key);
			tvc1.setGravity(Gravity.CENTER);
			tvc1.setLayoutParams(new TableRow.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT, 1f));
			tvc2.setBackgroundResource(R.drawable.cell_shape);
			tvc2.append(value);
			tvc2.setGravity(Gravity.CENTER);
			tvc2.setLayoutParams(new TableRow.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT, 1f));

			tr.addView(tvc1);
			tr.addView(tvc2);

			tbl.addView(tr, rowNum);

		}
	}

}
