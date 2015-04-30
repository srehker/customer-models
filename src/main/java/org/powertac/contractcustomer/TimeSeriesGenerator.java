package org.powertac.contractcustomer;

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

	public TimeSeries generateTimeSeries(DateTime start, DateTime end) {
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

				for (int i = 1; i < 25; i++) {
					hourvalues.add(nf.parse(split[i]).doubleValue()); // TODO
																		// randomize
																		// here?
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
		return new TimeSeries(finalDays, start, end);
	}
}
