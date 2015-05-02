package org.powertac.contractcustomer.timeseries;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Locale;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

public class TimeSeriesGenerator {

	public LoadTimeSeries generateLoadTimeSeries(DateTime start, DateTime end) {
		BufferedReader br = null;
		String line = "";
		String cvsSplitBy = ";";
		String filePath = new File("").getAbsolutePath();
		System.out.println(filePath);
		ArrayList<TimeSeriesDay> days = new ArrayList<TimeSeriesDay>();
		DateTimeFormatter df = DateTimeFormat.forPattern("dd.MM.yyyy");
		NumberFormat nf = NumberFormat.getInstance(Locale.GERMAN);
		try {

			br = new BufferedReader(new FileReader(filePath
					+ "\\src\\main\\resources\\historical-load-profiles.csv"));
			br.readLine(); // skip 1st line
			while ((line = br.readLine()) != null) {

				String[] split = line.split(cvsSplitBy);
				DateTime date = df.parseDateTime(split[0]);
				ArrayList<Double> hourvalues = new ArrayList<Double>();

				hourvalues.add(nf.parse(split[24]).doubleValue());// 24:00 -->
																	// 0:00
				for (int i = 1; i < 24; i++) {
					double value = nf.parse(split[i]).doubleValue();

					double rand = Math.random() * 100 +1;
					if (rand >= 99) {
						value *=1.1; //+10%
					} else if (rand <= 2) {
						value *=0.9; //-10%
					} else if (rand <= 5) {
						value *=1.01; //+1%
					} else if (rand >= 95) {
						value *=0.99; //-1%
					}

					hourvalues.add(value);
				}

				TimeSeriesDay d = new TimeSeriesDay(
						Daytype.getDaytypeFromDate(date), date, hourvalues);
				days.add(d);

			}

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		ArrayList<TimeSeriesDay> finalDays = new ArrayList<TimeSeriesDay>();
		for (TimeSeriesDay d : days) {
			if (d.getDate().equals(start)
					|| d.getDate().equals(end)
					|| (d.getDate().isAfter(start) && d.getDate().isBefore(end))) {
				finalDays.add(d);
			}
		}
		return new LoadTimeSeries(finalDays, start, end);
	}
}
