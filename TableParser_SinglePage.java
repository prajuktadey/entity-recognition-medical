package com.amazonaws.getInfo;

//import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.io.*;
import javax.imageio.ImageIO;
import javax.swing.*;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.amazonaws.services.textract.AmazonTextract;
import com.amazonaws.services.textract.AmazonTextractClientBuilder;
import com.amazonaws.services.textract.model.AnalyzeDocumentRequest;
import com.amazonaws.services.textract.model.AnalyzeDocumentResult;
import com.amazonaws.services.textract.model.Block;
//import com.amazonaws.services.textract.model.BoundingBox;
import com.amazonaws.services.textract.model.Document;
import com.amazonaws.services.textract.model.S3Object;
//import com.amazonaws.services.textract.model.Point;
import com.amazonaws.services.textract.model.Relationship;
import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration;

public class TableParser_SinglePage extends JPanel
{
	private static final long serialVersionUID = 1L;
	
	//BufferedImage image;
	AnalyzeDocumentResult output;

	public TableParser_SinglePage(AnalyzeDocumentResult documentResult) throws Exception 
	{
		super();

	    output = documentResult; // Results of text detection.
	    //image = bufImage; // The image containing the document.

	}
	
	public String get_table_csv_results(AnalyzeDocumentResult result)
	{
		List<Block> blocks = result.getBlocks();
		HashMap<String,Block> block_map = new HashMap<String, Block>();
		List<Block> table_blocks = new ArrayList<Block>();
		
		for(Block block: blocks)
		{
			block_map.put(block.getId(), block);
			if(block.getBlockType().equals("TABLE"))
			{
				//System.out.println("In TABLE");
				table_blocks.add(block);
			}
		}
		
		if(table_blocks.size()<=0)
			return "\nNo Tables Found\n";
		
		String csv = "";
		int index = 0;
		for(Block table: table_blocks)
		{
			csv += generate_table_csv(table, block_map, index +1);
			csv += "\n\n";
			index++;
			System.out.println(index);
		}
		
		return csv;
	}
	
	
	public List<List<HashMap<String, String>>> get_table_map_results(AnalyzeDocumentResult result)
	{
		List<Block> blocks = result.getBlocks();
		HashMap<String,Block> block_map = new HashMap<String, Block>();
		List<Block> table_blocks = new ArrayList<Block>();
		
		for(Block block: blocks)
		{
			block_map.put(block.getId(), block);
			if(block.getBlockType().equals("TABLE"))
			{
				//System.out.println("In TABLE");
				table_blocks.add(block);
			}
		}
		
		if(table_blocks.size()<=0)
			return null;
		
		List<List<HashMap<String, String>>> allTables = new ArrayList<>();
		
		for(Block table: table_blocks)
		{
			List<HashMap<String,String>> dataTable = new ArrayList<>();
			HashMap<Integer, HashMap<Integer, String>> rows = get_rows_columns_map(table, block_map);
			List<String> keys = new ArrayList<>();
			for(Map.Entry<Integer, String> entry: rows.get(1).entrySet())
			{
				keys.add(entry.getValue());
			}
			for(int i=2; i<=rows.size(); i++)
			{
				HashMap<String, String> row_data = new HashMap<>();
				HashMap<Integer, String> row = rows.get(i);
				for(Map.Entry<Integer, String> entry: row.entrySet())
				{
					row_data.put(keys.get(entry.getKey()-1), entry.getValue());
				}
				dataTable.add(row_data);
			}
			allTables.add(dataTable);
			
		}
		return allTables;
		
	}
	
	
	public String generate_table_csv(Block table_result, HashMap<String,Block> block_map, int table_index)
	{
		HashMap<Integer, HashMap<Integer, String>> rows = get_rows_columns_map(table_result, block_map);
		
		System.out.println(rows.toString());
		
		String table_id = "Table_" + (new Integer(table_index)).toString();
		String csv = "Table : " + table_id + "\n\n";
		
		for(Map.Entry<Integer, HashMap<Integer, String>> row : rows.entrySet())
		{
			HashMap<Integer, String> cols = row.getValue();
			for(Map.Entry<Integer, String> col : cols.entrySet())
			{
				csv += col.getValue() + ",";
			}
			csv += "\n";
		}
		csv += "\n\n\n";
		return csv;
	}
	
	public HashMap<Integer, HashMap<Integer, String>> get_rows_columns_map(Block table_result, HashMap<String,Block> block_map)
	{
		HashMap<Integer, HashMap<Integer, String>> rows = new HashMap<Integer, HashMap<Integer, String>>();
	
		
		for(Relationship relationship: table_result.getRelationships())
		{
			if(relationship.getType().equals("CHILD"))
			{
				for(String child_id: relationship.getIds())
				{
					Block cell = block_map.get(child_id);
					if(cell.getBlockType().equals("CELL"))
					{
						int row_index = cell.getRowIndex();
						int col_index = cell.getColumnIndex();
						if(!(rows.containsKey(row_index)))
							rows.put(row_index, new HashMap<Integer, String>());
						
						HashMap<Integer, String> columns = rows.get(row_index);
						columns.put(col_index, get_text(cell, block_map));
						rows.replace(row_index, columns);
					}
				}
			}
		}
			
		return rows;
	}
	
	public String get_text(Block result, HashMap<String,Block> block_map)
	{
		String text = "";
		
		if(result.getRelationships()!=null)
		{
			for(Relationship relationship: result.getRelationships())
			{
				if(relationship.getType().equals("CHILD"))
				{
					for(String child_id:relationship.getIds())
					{
						Block word = block_map.get(child_id);
						if(word.getBlockType().equals("WORD"))
						{
							text += word.getText() + " "; 
						}
					}
				}
			}
		}
		
		return text;
	}
	
	public void map_to_csv(String table_csv, String fileName)
	{
		String eol = System.getProperty("line.separator");
		//Writer writer = new FileWriter(new File("table_output.csv"));
		List<String> rows = new ArrayList<String>(Arrays.asList(table_csv.split("\n")));
		//System.out.println(rows.toString());
		try { 
			Writer writer = new FileWriter(fileName);
			writer.write(table_csv);
			writer.close();
			} catch (IOException ex) {
			  ex.printStackTrace(System.err);
			}
		
	}
	
	public static void main(String arg[]) throws Exception {

        // The S3 bucket and document
        String document = "0001.jpg";
        String bucket = "digitalreports";

        AmazonS3 s3client = AmazonS3ClientBuilder.standard()
                .withEndpointConfiguration( 
                        new EndpointConfiguration("https://s3.amazonaws.com","ap-south-1"))
                .build();
        
               
        // Get the document from S3
        com.amazonaws.services.s3.model.S3Object s3object = s3client.getObject(bucket, document);
        S3ObjectInputStream inputStream = s3object.getObjectContent();
        BufferedImage image = ImageIO.read(inputStream);

        // Call AnalyzeDocument 
        EndpointConfiguration endpoint = new EndpointConfiguration(
                "https://textract.ap-south-1.amazonaws.com", "ap-south-1");
        AmazonTextract client = AmazonTextractClientBuilder.standard()
                .withEndpointConfiguration(endpoint).build();

                
        AnalyzeDocumentRequest request = new AnalyzeDocumentRequest()
                .withFeatureTypes("TABLES","FORMS")
                 .withDocument(new Document().//withS3Object(s3object);
                        withS3Object(new S3Object().withName(document).withBucket(bucket)));


        AnalyzeDocumentResult result = client.analyzeDocument(request);
        System.out.println(result.toString());
        
        TableParser_SinglePage obj = new TableParser_SinglePage(result);
        String table_csv = obj.get_table_csv_results(result);
        List<List<HashMap<String, String>>> table_map = obj.get_table_map_results(result);
        System.out.println(table_map.toString());
        //obj.map_to_csv(table_csv, "table_output.csv");
	}
}
