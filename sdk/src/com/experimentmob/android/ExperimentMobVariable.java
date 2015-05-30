package com.experimentmob.android;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * OpenABTestVariable is an annotation which is used for variables to be tested
 * using Open A/B Testing.
 * 
 * @author Susheel Kumar
 * 
 */
@Documented
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ExperimentMobVariable {
}