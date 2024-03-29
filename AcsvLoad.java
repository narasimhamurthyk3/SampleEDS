package com.test;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CsvLoad {

	public static void main(String[] args) throws IOException {
		List<Map<String, String>> list = new ArrayList<Map<String, String>>();		
		BufferedReader reader = new BufferedReader(new FileReader(new File("\\DeleteRecords\\input\\IPMD_RETUKB03_CI033967.csv")));
		reader.readLine();// ignore header
		String headerFromConfig1 = "IPM_RECORD_ID,SORT_CODE,ACCOUNT_NUMBER,FROM_DATE,TO_DATE";
		
		String headerFromConfig2 = "IPM_RECORD_ID,SORT_CODE,BRAND";
				
		String query1 = "SELECT [Id], [DocumentTitle],[DateCreated]  FROM  DOCUMENT WHERE [sortCode] ="
				+ "'" + SORT_CODE + "' AND [accountNumber] = '" + ACCOUNT_NUMBER + "' AND [DateCreated] >="
				+ FROM_DATE + " AND  [DateCreated] <=" + TO_DATE ;
		
		
		String query2 = "SELECT [Id], [DocumentTitle],[DateCreated]  FROM  DOCUMENT WHERE [sortCode] ="
				+ "'" + SORT_CODE + "' AND [brand] = '" + BRAND ;
		
		
		String[] headers = headerFromConfig1.split(",");
		String line;
		while ((line = reader.readLine()) != null) {
			Map<String, String> map = new LinkedHashMap<>();			
			String[] values = line.split(",");
			map.put(headers[0], values[0]);
			String[] keyIdentifyRecords = values[1].split("\\|");
			for (int i = 0; i < headers.length - 1; i++) {
				map.put(headers[i + 1], keyIdentifyRecords[i]);
			}
			list.add(map);
		}
		reader.close();
		for (Map<String, String> map : list) {
			for (String key : map.keySet()) {
				//System.out.print(key + ":" + map.get(key) + "   ");								
			}			
			System.out.println();
			String sqlStmt = "SELECT [Id], [DocumentTitle],[DateCreated]  FROM  [DOCUMENT] WHERE [sortCode] ="
					+ "'" + map.get("SORT_CODE") + "' AND [accountNumber] = '" + map.get("ACCOUNT_NUMBER") + "' AND [DateCreated] >="
					+ map.get("FROM_DATE") + " AND  [DateCreated] <=" + map.get("TO_DATE");			
			System.out.println(sqlStmt);
		}
	}
}
