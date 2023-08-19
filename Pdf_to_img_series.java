package com.amazonaws.getInfo;

import java.awt.image.BufferedImage;
import java.io.*;
import java.util.List;

import javax.imageio.ImageIO;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;

@SuppressWarnings("unchecked")
public class Pdf_to_img_series {
	
	public static void main(String[] args) {
		AWSCredentials credentials = null;
        try {
        	
        String bucketName = "digitalreports";
        String key = "1.0.pdf";
        credentials = new ProfileCredentialsProvider("default").getCredentials();
        AmazonS3 s3 = AmazonS3ClientBuilder.standard()
                .withCredentials(new AWSStaticCredentialsProvider(credentials))
                .withRegion("ap-south-1")
                .build();
        
        System.out.println("Downloading an object");
        /*
        File download_dest = new File("D://Stellablue/S3Downloads/1.pdf");
        if (!download_dest.exists()) {
        	download_dest.mkdir();
            System.out.println("Folder Created -> "+ download_dest.getAbsolutePath());
        }
        ObjectMetadata object = s3.getObject(new GetObjectRequest(bucketName, key), download_dest);
        System.out.println("Content-Type: "  + object.getContentType());
        */
        
        String down_destn = "D://Stellablue/S3Downloads/1.0.pdf";
        S3Object object = s3.getObject(new GetObjectRequest(bucketName, key));
        System.out.println("Content-Type: "  + object.getObjectMetadata().getContentType());
        InputStream reader = new BufferedInputStream(
        		   object.getObjectContent());
        		File file = new File(down_destn);      
        		OutputStream writer = new BufferedOutputStream(new FileOutputStream(file));

        		int read = -1;

        		while ( ( read = reader.read() ) != -1 ) {
        		    writer.write(read);
        		}

        		writer.flush();
        		writer.close();
        		reader.close();
        
        String sourceDir = "D://Stellablue/S3Downloads/1.0.pdf"; // Pdf files are read from this folder
        String destinationDir = "D://Stellablue/1.0 img files/"; // converted images from pdf document are saved here

        File sourceFile = new File(sourceDir);
        File destinationFile = new File(destinationDir);
        if (!destinationFile.exists()) {
            destinationFile.mkdir();
            System.out.println("Folder Created -> "+ destinationFile.getAbsolutePath());
        }
        if (sourceFile.exists()) {
            System.out.println("Images copied to Folder: "+ destinationFile.getName());             
            PDDocument document = PDDocument.load(sourceDir);
            List<PDPage> list = document.getDocumentCatalog().getAllPages();
            System.out.println("Total files to be converted -> "+ list.size());

            String fileName = sourceFile.getName().replace(".pdf", "");             
            int pageNumber = 1;
            for (PDPage page : list) {
                BufferedImage image = page.convertToImage();
                File outputfile = new File(destinationDir + fileName +"_"+ pageNumber +".png");
                System.out.println("Image Created -> "+ outputfile.getName());
                ImageIO.write(image, "png", outputfile);
                pageNumber++;
            }
            document.close();
            System.out.println("Converted Images are saved at -> "+ destinationFile.getAbsolutePath());
        } else {
            System.err.println(sourceFile.getName() +" File not exists");
        }

    } catch (Exception e) {
        e.printStackTrace();
    }
}

}
