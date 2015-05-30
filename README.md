##ExperimentMob-androidSDK##
> This repository contains code for the android-SDK for [ExperimentMob](http://github.com/callmesusheel/ExperimentMob). You can use *ExperimentMob* to implement A/B Testing in your app/game. 
 
 * Integrate the *ExperimentMob android-SDK* by adding it as a reference project to your android project. 
 
 * Add the following lines in the *strings.xml* file of your android project.
		 <string name="experimentmob_basepath">http://hostname:port/openab/</string>
		 <string name="experimentmob_appid">your appId</string>
 * Add the following lines in the onCreate function of your activity class. 

			ExperimentMob.getInstance().init(this,this,"<userId of the user>", new OnABTesting() {
			
			@Override
			public void onABTestingInitDone(List<String>experimentIds) {
				// Add code here to perform action after variables are synced. You also get
				// the experimentIds which a user is part of. You can use this data to see  
				// if an experiment had a positive impact or not.
			}

			@Override
			public void onABTestingFileDone(String filename) {
				//add code here to perform action after file are synced.
			}
		});
 *  Use ***@ExperimentMobVariable*** annotation on class fields to include them as fields to perform A/B Testing.  *ExperimentMob* supports *int, float, double, boolean and String* types of class fields. You can declare the class member like this For example :
 
  ![Example](https://raw.githubusercontent.com/callmesusheel/ExperimentMob/master/screenshots/variables_dec.png)
	
    Always remember to include these variables in an activity class. Whenever the variables are included with the  *@ExperimentMobVariable* annotation please ensure you insert the code snippet from the above step in the onCreate(). Also, when such variables are declared, create the same in the fields tab of the ExpermimentMob Dashboard.
 *  To support **file synchronisation** and to perform A/B Testing over the same, please include the required files in a folder called *"abtesting"*  in the assets folder. Again, create the same entry in the fields tab of the ExperimentMob Dashboard. Here you may place all your files. While reading the files, do not read the files from this location. You can get the base path to read the file in this manner : 
`ExperimentMob.getABTestingFileBasePath(context)` 
An example of such an instance would be like this

 `File imgFile = new File(ExperimentMob.getABTestingFileBasePath(context)+"/pink");`
 
 ![Example](https://raw.githubusercontent.com/callmesusheel/ExperimentMob/master/screenshots/eclipse-assets.png)
 
----------
> For further help or feedback, contact [Susheel](mailto:susheel.s2k@gmail.com) . Also, you can post the bugs on github. 
