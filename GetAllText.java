package com.amazonaws.getInfo;

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration;
import com.amazonaws.services.textract.AmazonTextract;
import com.amazonaws.services.textract.AmazonTextractClientBuilder;
import com.amazonaws.services.textract.model.AnalyzeDocumentRequest;
import com.amazonaws.services.textract.model.AnalyzeDocumentResult;
import com.amazonaws.services.textract.model.Block;
import com.amazonaws.services.textract.model.BoundingBox;
import com.amazonaws.services.textract.model.DetectDocumentTextRequest;
import com.amazonaws.services.textract.model.DetectDocumentTextResult;
import com.amazonaws.services.textract.model.Document;
import com.amazonaws.services.textract.model.S3Object;

public class GetAllText {
	
	AnalyzeDocumentResult output;

	public GetAllText(AnalyzeDocumentResult documentResult) throws Exception 
	{
		super();
	    output = documentResult; // Results of text detection
	}

	
	public List<String> extractText(/*DetectDocumentTextResult result*/AnalyzeDocumentResult result)
	{
		List<String> lines = new ArrayList<String>();
        List<Block> blocks = result.getBlocks();
        BoundingBox boundingBox = null;
        for (Block block : blocks) {
            if ((block.getBlockType()).equals("LINE")) {
                boundingBox = block.getGeometry().getBoundingBox();
                lines.add(block.getText());
            }
        }
        return lines;
	}
	
	public void getTextFile(List<String> lines, String filepath) throws IOException
	{
		String eol = System.getProperty("line.separator");
	     FileWriter myWriter = new FileWriter(filepath);
	     for(String line : lines)
	     {
	    	myWriter.append(line)
	    			.append(eol);
	     }
	     myWriter.close();
	     System.out.println("Successfully wrote to the file.");
	}
	
	public static void main(String args[]) throws Exception
	{
		String documentName = "1.0.pdf";
        String bucketName = "digitalreports";
		
		 EndpointConfiguration endpoint = new EndpointConfiguration(
	                "https://textract.ap-south-1.amazonaws.com", "ap-south-1");
	     AmazonTextract client = AmazonTextractClientBuilder.standard()
	                .withEndpointConfiguration(endpoint).build();
	     
	     /*
	     DetectDocumentTextRequest request = new DetectDocumentTextRequest()
	                .withDocument(new Document()
	                        .withS3Object(new S3Object()
	                                .withName(documentName)
	                                .withBucket(bucketName)));
	     */
	     
	     AnalyzeDocumentRequest request = new AnalyzeDocumentRequest()
	    		 .withFeatureTypes("TABLES","FORMS")
	                .withDocument(new Document()
	                        .withS3Object(new S3Object()
	                                .withName(documentName)
	                                .withBucket(bucketName)));
	     
	     //DetectDocumentTextResult result = client.detectDocumentText(request);
	     AnalyzeDocumentResult result = client.analyzeDocument(request);
	     System.out.println(result.toString());
	     
	     GetAllText obj = new GetAllText(result);
	     List<String> lines = obj.extractText(result);
	     //System.out.println(lines.toString());
	     
	     
	}

}
