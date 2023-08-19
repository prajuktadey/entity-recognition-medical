package com.amazonaws.getInfo;

//import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.*;
import java.io.*;
import java.nio.ByteBuffer;
import javax.imageio.ImageIO;
import javax.swing.*;
import com.amazonaws.util.IOUtils;
//import com.amazon.textract.pdf.ImageType;
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

public class KeyValueParser_SinglePage extends JPanel {
	private static final long serialVersionUID = 1L;

	//BufferedImage image;

	AnalyzeDocumentResult output;

	public KeyValueParser_SinglePage(AnalyzeDocumentResult documentResult) throws Exception 
	{
		super();

	    output = documentResult; // Results of text detection.
	    //image = bufImage; // The image containing the document.

	}
	
	public List<HashMap<String, Block>> get_kv_map(AnalyzeDocumentResult result)
	{
		List<Block> blocks = result.getBlocks();
		
		HashMap<String,Block> key_map, value_map, block_map;
		key_map = new HashMap<String, Block>();
		value_map = new HashMap<String, Block>();
		block_map = new HashMap<String, Block>();
		
		for (Block block : blocks)
		{
			String block_id = block.getId();
			block_map.put(block_id, block);
			//System.out.println(block.getBlockType());
			if(block.getBlockType().equals("KEY_VALUE_SET"))
			{
				if(block.getEntityTypes().contains("KEY"))
				{
					key_map.put(block_id, block);
				}
				else
				{
					value_map.put(block_id, block);
				}
			}
		}
		
		//System.out.println(key_map.toString());
		
		List<HashMap<String, Block>> maps = new ArrayList<HashMap<String, Block>>();
		maps.add(block_map);
		maps.add(key_map);
		maps.add(value_map);
		return maps;
		
	}
	
	public HashMap<String,String> get_kv_relationship(List<HashMap<String, Block>> maps)
	{
		HashMap<String,Block> block_map = maps.get(0);
		HashMap<String,Block> key_map = maps.get(1);
		HashMap<String,Block> value_map = maps.get(2);
		
		HashMap<String, String> kvs = new HashMap<String,String>();
		
		for(Map.Entry<String, Block> ele: key_map.entrySet())
		{
			Block value_block = find_value_block(ele.getValue(), value_map);
			String key = get_text(ele.getValue(), block_map);
			String val = get_text(value_block, block_map);
			kvs.put(key, val);
		}
		
		return kvs;
	}
	
	public Block find_value_block(Block key_block, HashMap<String,Block> value_map)
	{
		Block value_block=null; // check later
		for(Relationship relationship: key_block.getRelationships())
		{
			if(relationship.getType().equals("VALUE"))
			{
				for(String value_id:relationship.getIds())
				{
					value_block = value_map.get(value_id);
				}
			}
		}
		return value_block;
	}
	
	public String get_text(Block result, HashMap<String,Block> block_map)
	{
		String text = "";
		
		if(result.equals(null))
			return text;
		
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
	
	public void print_kvs(HashMap<String, String> kvs)
	{
		//System.out.println(kvs.toString());
		for(Map.Entry<String, String> ele: kvs.entrySet())
		{
			System.out.println(ele.getKey()+ " : " + ele.getValue());
		}
	}
	
	public void map_to_csv(HashMap<String, String> kvs, String fileName)
	{
		String eol = System.getProperty("line.separator");
		try (Writer writer = new FileWriter(fileName)) {
			  for (Map.Entry<String, String> ele : kvs.entrySet()) {
			    writer.append(ele.getKey())
			          .append(',')
			          .append(ele.getValue())
			          .append(eol);
			  }
			} catch (IOException ex) {
			  ex.printStackTrace(System.err);
			}
	}
		
	public static void main(String arg[]) throws Exception {

		
        // The S3 bucket and document
        String document = "D://Stellablue/1.0 img files/1.0_1.png";
        //String bucket = "digitalreports";

        /*
        AmazonS3 s3client = AmazonS3ClientBuilder.standard()
                .withEndpointConfiguration( 
                        new EndpointConfiguration("https://s3.amazonaws.com","ap-south-1"))
                .build();
        
               
        //Get the document from S3
        com.amazonaws.services.s3.model.S3Object s3object = s3client.getObject(bucket, document);
        S3ObjectInputStream inputStream = s3object.getObjectContent();
        System.out.println("Content-Type: "  + s3object.getObjectMetadata().getContentType());
        BufferedImage image = ImageIO.read(inputStream);
        */
        
        /*
        ImageType imageType = ImageType.JPEG;
        if(document.toLowerCase().endsWith(".png"))
            imageType = ImageType.PNG;
         */

        //Get image bytes
        ByteBuffer imageBytes = null;
        try(InputStream in = new FileInputStream(document)) {
            imageBytes = ByteBuffer.wrap(IOUtils.toByteArray(in));
        }

        

        // Call AnalyzeDocument 
        EndpointConfiguration endpoint = new EndpointConfiguration(
                "https://textract.ap-south-1.amazonaws.com", "ap-south-1");
        AmazonTextract client = AmazonTextractClientBuilder.standard()
                .withEndpointConfiguration(endpoint).build();

        /*        
        AnalyzeDocumentRequest request = new AnalyzeDocumentRequest()
                .withFeatureTypes("TABLES","FORMS")
                 .withDocument(new Document().
                        withS3Object(new S3Object().withName(document).withBucket(bucket)));
		*/
        
        AnalyzeDocumentRequest request = new AnalyzeDocumentRequest()
        		.withFeatureTypes("TABLES","FORMS")
                .withDocument(new Document()
                        .withBytes(imageBytes));
        AnalyzeDocumentResult result = client.analyzeDocument(request);
        
        //System.out.println(result.toString());
        KeyValueParser_SinglePage obj = new KeyValueParser_SinglePage(result);
        List<HashMap<String, Block>> maps = obj.get_kv_map(result);
        HashMap<String,String> kvs = obj.get_kv_relationship(maps);
        obj.print_kvs(kvs);
        obj.map_to_csv(kvs, "key_pair_output.csv");
        
        /*
        AWSCredentials credentials = null;
        try {
            credentials = new ProfileCredentialsProvider("default").getCredentials();
        } catch (Exception e) {
            throw new AmazonClientException(
                    "Cannot load the credentials from the credential profiles file. " +
                    "Please make sure that your credentials file is at the correct " +
                    "location (C:\\Users\\KIIT\\.aws\\credentials), and is in valid format.",
                    e);
        }

        AmazonS3 s3 = AmazonS3ClientBuilder.standard()
            .withCredentials(new AWSStaticCredentialsProvider(credentials))
            .withRegion("ap-south-1")
            .build();
        
        System.out.println("Uploading results to S3");
        s3.putObject(new PutObjectRequest(bucket, document+"results", "key_pair_output.csv"));
         
         */
	}

}
