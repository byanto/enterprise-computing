package com.aws.s3;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;

import org.apache.log4j.Logger;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.CreateBucketRequest;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;

public class S3Exercise {
	
	static Logger log = Logger.getLogger(S3Exercise.class.getName());

	/**
	 * Amazon S3 exercise: Fill the gaps.
	 * 
	 * @author markus klems
	 */
	public static void main(String[] args) {
		/*
		 * The ProfileCredentialsProvider will return your [default] credential
		 * profile by reading from the credentials file located at
		 * (/path-to-your-user-home/.aws/credentials).
		 */
		AWSCredentials credentials = null;
		try {
			credentials = new ProfileCredentialsProvider("default")
					.getCredentials();
		} catch (Exception e) {
			throw new AmazonClientException(
					"Cannot load the credentials from the credential profiles file. "
							+ "Please make sure that your credentials file is at the correct "
							+ "location (/path-to-your-user-home/.aws/credentials), and is in valid format.",
					e);
		}

		AmazonS3 s3 = new AmazonS3Client(credentials);

		try {
			// TODO create a bucket with name "ise-tu-berlin-exercise2-",
			// followed by your nickname (e.g., silversurfer)
			log.info("Creating a bucket (if it does not exist, yet)");
			String bucketName = "ise-tu-berlin-exercise2-batman";
			if(!(s3.doesBucketExist(bucketName))){
				s3.createBucket(new CreateBucketRequest(bucketName));
			}

			// TODO Upload a text File object to your S3 bucket
			// use the createSampleFile method to create the File object
			log.info("Uploading an object");
			File file = createSampleFile("batman");
			String key = "batman-sample-file";
			s3.putObject(new PutObjectRequest(bucketName, key, file));

			// TODO Download the file from S3 and print it out using the
			// displayTextInputStream method.
			log.info("Downloading an object");
			S3Object s3Object = s3.getObject(new GetObjectRequest(bucketName, key));
			displayTextInputStream(s3Object.getObjectContent());
			
			s3.deleteObject(bucketName, key);
			s3.deleteBucket(bucketName);

		} catch (AmazonServiceException ase) {
			log.error("Caught an AmazonServiceException, which means your request made it "
							+ "to Amazon S3, but was rejected with an error response for some reason.");
			log.error("Error Message:    " + ase.getMessage());
			log.error("HTTP Status Code: " + ase.getStatusCode());
			log.error("AWS Error Code:   " + ase.getErrorCode());
			log.error("Error Type:       " + ase.getErrorType());
			log.error("Request ID:       " + ase.getRequestId());
		} catch (AmazonClientException ace) {
			log.error("Caught an AmazonClientException, which means the client encountered "
							+ "a serious internal problem while trying to communicate with S3, "
							+ "such as not being able to access the network.");
			log.error("Error Message: " + ace.getMessage());
		} catch(IOException ioe){
			log.error("Caught on IOException");
			log.error("Error Message: " + ioe.getMessage());
		}

	}

	private static File createSampleFile(String nickname) throws IOException {
		File file = File.createTempFile(nickname, ".txt");
		file.deleteOnExit();

		Writer writer = new OutputStreamWriter(new FileOutputStream(file));
		writer.write("Hello World!\n");
		writer.write("I am " + nickname + "\n");
		writer.write("Give me my Portfoliopunkte!!!\n");
		writer.close();

		return file;
	}

	private static void displayTextInputStream(InputStream input)
			throws IOException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(input));
		while (true) {
			String line = reader.readLine();
			if (line == null)
				break;

			log.info("    " + line);
		}
	}

}
