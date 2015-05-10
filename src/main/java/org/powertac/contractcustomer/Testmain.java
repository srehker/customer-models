package org.powertac.contractcustomer;

import java.io.File;

import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.powertac.contractcustomer.timeseries.DayComparisonLoadForecast;
import org.powertac.contractcustomer.timeseries.LoadForecast;
import org.powertac.contractcustomer.timeseries.LoadTimeSeries;
import org.powertac.contractcustomer.timeseries.TimeSeriesGenerator;

public class Testmain {
	
	public static void main(String[] args){
		DateTimeFormatter df=DateTimeFormat.forPattern("dd.MM.yyyy");
		TimeSeriesGenerator gen = new TimeSeriesGenerator();
		LoadTimeSeries t=gen.generateLoadTimeSeries( df.parseDateTime("01.01.2012"), df.parseDateTime("31.12.2012"), 3);
		String filePath = new File("").getAbsolutePath();
        System.out.println (filePath);
		t.outputFile(filePath+"\\histLoad.csv");
		LoadForecast lf =new DayComparisonLoadForecast();
		LoadTimeSeries pred =lf.calculateLoadForecast(t, df.parseDateTime("01.01.2013"), df.parseDateTime("31.12.2013"));
		pred.outputFile(filePath+"\\predLoad.csv");
		LoadTimeSeries real=gen.generateLoadTimeSeries( df.parseDateTime("01.01.2013"), df.parseDateTime("31.12.2013"), 3);
		real.outputFile(filePath+"\\realLoad.csv");
		System.out.println("histTotal:"+t.getTotalLoad());
		System.out.println("predTotal:"+pred.getTotalLoad());
		System.out.println("realTotal:"+real.getTotalLoad());
	}

}
