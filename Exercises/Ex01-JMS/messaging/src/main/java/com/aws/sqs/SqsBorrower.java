package com.aws.sqs;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;
import java.util.StringTokenizer;
import java.util.UUID;

import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.DeleteMessageRequest;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.MessageAttributeValue;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.services.sqs.model.SendMessageRequest;

/**
 * 
 * Modified version of code example in Chapter 4 of "Java Message Service", 2nd
 * Ed, O'Reilly which replaces JMS with SQS.
 * 
 * @author markus klems
 * 
 */
public class SqsBorrower {

	private AmazonSQS sqs = null;
	private String responseQ = null;
	private String requestQ = null;
	private UUID uuid = null;

	public SqsBorrower(String requestQueue, String responseQueue) {
		this.requestQ = requestQueue;
		this.responseQ = responseQueue;
		this.uuid = UUID.randomUUID();

		/*
		 * The ProfileCredentialsProvider will return your [default] credential
		 * profile by reading from the credentials file located at
		 * (/path-to-you-user-directory/.aws/credentials).
		 */
		AWSCredentials credentials = null;
		try {
			credentials = new ProfileCredentialsProvider("default")
					.getCredentials();
		} catch (Exception e) {
			throw new AmazonClientException(
					"Cannot load the credentials from the credential profiles file. "
							+ "Please make sure that your credentials file is at the correct "
							+ "location (/path-to-you-user-directory/.aws/credentials), and is in valid format.",
					e);
		}

		sqs = new AmazonSQSClient(credentials);
		Region useast1 = Region.getRegion(Regions.US_EAST_1);
		sqs.setRegion(useast1);
	}

	private void sendLoanRequest(double salary, double loanAmt) {

		// Create and send the loan request message request
		SendMessageRequest loanRequestMessageRequest = new SendMessageRequest();
		MessageAttributeValue value = new MessageAttributeValue()
				.withDataType("String");
		value.setStringValue(uuid.toString());
		loanRequestMessageRequest.addMessageAttributesEntry("uuid", value);
		loanRequestMessageRequest.setQueueUrl(requestQ);
		loanRequestMessageRequest.setMessageBody(salary + "," + loanAmt);
		sqs.sendMessage(loanRequestMessageRequest);

		// TODO check response queue for matching responses
//		ReceiveMessageRequest receiveMessageRequest = new ReceiveMessageRequest().withMessageAttributeNames("uuid");
		ReceiveMessageRequest receiveMessageRequest = new ReceiveMessageRequest();
		receiveMessageRequest.setQueueUrl(responseQ);
		receiveMessageRequest.setMessageAttributeNames(Arrays.asList("uuid"));
		boolean response = false;
		System.out.println("Waiting for responses...");
		while (!response) {
			List<Message> messages = sqs.receiveMessage(receiveMessageRequest).getMessages();
			for (Message lenderResponseMessage : messages) {
				for (Entry<String, MessageAttributeValue> entry : lenderResponseMessage
						.getMessageAttributes().entrySet()) {
					
					if (entry.getKey().equals("uuid")
							&& entry.getValue().getStringValue()
									.equals(uuid.toString())) {
						String messageRecieptHandle = lenderResponseMessage
								.getReceiptHandle();
						// Print out the response
			 			System.out.println(lenderResponseMessage.getBody());
						// delete the message from the queue
			 			sqs.deleteMessage(new DeleteMessageRequest(responseQ, messageRecieptHandle));
						response = true;
					}
				}
			}
			if (!response) {
				try {
					Thread.sleep(5000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}

	public static void main(String argv[]) {
		String requestq = "https://sqs.us-east-1.amazonaws.com/336380577901/LoanRequestQ";
		String responseq = "https://sqs.us-east-1.amazonaws.com/336380577901/LoanResponseQ";
		SqsBorrower borrower = new SqsBorrower(requestq, responseq);

		try {
			// Read all standard input and send it as a message
			BufferedReader stdin = new BufferedReader(new InputStreamReader(
					System.in));
			System.out.println("QBorrower Application Started");
			System.out.println("Press enter to quit application");
			System.out.println("Enter: Salary, Loan_Amount");
			System.out.println("\ne.g. 50000, 120000");

			while (true) {
				System.out.print("> ");

				String loanRequest = stdin.readLine();
				if (loanRequest == null || loanRequest.trim().length() <= 0) {
					System.exit(0);
				}

				// Parse the deal description
				StringTokenizer st = new StringTokenizer(loanRequest, ",");
				double salary = Double.valueOf(st.nextToken().trim())
						.doubleValue();
				double loanAmt = Double.valueOf(st.nextToken().trim())
						.doubleValue();

				borrower.sendLoanRequest(salary, loanAmt);
			}
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
	}
}
