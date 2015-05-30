package com.experimentmob.android;

import java.util.List;

public class ExperimentMobPojo {
	List<Experiment> experiments;
}

class Experiment {
	String id;
	String name;
	String desc;
	List<FieldContainer> fields;
	String createDate;
	String finishedDate;
	String expression;
}

class FieldContainer {
	String name;
	String type;
	Object value;
}
