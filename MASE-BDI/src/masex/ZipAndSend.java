package masex;

// File Name SendFileEmail.java

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.mail.*;
import javax.mail.internet.*;
import javax.activation.*;

import com.dropbox.core.DbxAppInfo;
import com.dropbox.core.DbxAuthFinish;
import com.dropbox.core.DbxClient;
import com.dropbox.core.DbxEntry;
import com.dropbox.core.DbxException;
import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.DbxWebAuthNoRedirect;
import com.dropbox.core.DbxWriteMode;

public class ZipAndSend {
	public static String zip(String zipFile, String[] fileList) {
		byte[] buffer = new byte[1024];
		String name = "";
		try {
			int count = 0;
			File f = new File(zipFile + "_" + count + ".zip");
			while (f.exists()) {
				count++;
				f = new File(zipFile + "_" + count + ".zip");
			}

			Starter.print("Output to Zip : " + zipFile + count + ".zip");

			FileOutputStream fos = new FileOutputStream(zipFile + count + ".zip");
			ZipOutputStream zos = new ZipOutputStream(fos);
			for (String file : fileList) {

				Starter.print("File Added : " + file);
				ZipEntry ze = new ZipEntry(file);
				zos.putNextEntry(ze);

				FileInputStream in = new FileInputStream(file);

				int len;
				while ((len = in.read(buffer)) > 0) {
					zos.write(buffer, 0, len);
				}

				in.close();

			}

			zos.closeEntry();
			// remember close it
			zos.close();
			name = zipFile + count + ".zip";

			Starter.print("Done");
		} catch (IOException ex) {
			ex.printStackTrace();
		}
		return name;
	}

	public static void send(String filename) {

		// Recipient's email ID needs to be mentioned.
		String to = "cassiocouto88@gmail.com";

		// Sender's email ID needs to be mentioned
		String from = "cassiocouto88@gmail.com";

		// Assuming you are sending email from localhost
		String host = "localhost";

		// Get system properties
		Properties properties = System.getProperties();

		// Setup mail server
		properties.setProperty("mail.smtp.host", host);

		// Get the default Session object.
		Session session = Session.getDefaultInstance(properties);

		try {
			// Create a default MimeMessage object.
			MimeMessage message = new MimeMessage(session);

			// Set From: header field of the header.
			message.setFrom(new InternetAddress(from));

			// Set To: header field of the header.
			message.addRecipient(Message.RecipientType.TO, new InternetAddress(to));

			// Set Subject: header field
			message.setSubject("Resultado simulacao");

			// Create the message part
			BodyPart messageBodyPart = new MimeBodyPart();

			// Fill the message
			messageBodyPart.setText("vide anexo");

			// Create a multipar message
			Multipart multipart = new MimeMultipart();

			// Set text message part
			multipart.addBodyPart(messageBodyPart);

			// Part two is attachment
			messageBodyPart = new MimeBodyPart();

			DataSource source = new FileDataSource(filename);
			messageBodyPart.setDataHandler(new DataHandler(source));
			messageBodyPart.setFileName(filename);
			multipart.addBodyPart(messageBodyPart);

			// Send the complete message parts
			message.setContent(multipart);

			// Send message
			Transport.send(message);
			Starter.print("Sent message successfully....");
		} catch (MessagingException mex) {
			// mex.printStackTrace();
			Starter.print("Message was not sent!");
		}

	}

	public static void main(String[] args) {
		upload("jcc.settings.xml");
	}

	public static void auth() {
		/* Carol's
		 * final String APP_KEY = "7zx42ains023jyu"; final String APP_SECRET =
		 * "rrzea2a9lpws67x";
		 */final String APP_KEY = "2nbjbpkt922982c";
		final String APP_SECRET = "2nbjbpkt922982c";
		DbxAppInfo appInfo = new DbxAppInfo(APP_KEY, APP_SECRET);

		DbxRequestConfig config = new DbxRequestConfig("JavaTutorial/1.0", Locale.getDefault().toString());
		DbxWebAuthNoRedirect webAuth = new DbxWebAuthNoRedirect(config, appInfo);
		String authorizeUrl = webAuth.start();
		// Have the user sign in and authorize your app.
		Starter.print("1. Go to: " + authorizeUrl);
		Starter.print("2. Click \"Allow\" (you might have to log in first)");
		Starter.print("3. Copy the authorization code.");
		try {
			String code = new BufferedReader(new InputStreamReader(System.in)).readLine().trim();
			DbxAuthFinish authFinish = webAuth.finish(code);
			String accessToken = authFinish.accessToken;
			DbxClient client = new DbxClient(config, accessToken);
			Starter.print("Linked account: " + client.getAccountInfo().displayName);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (DbxException e) {
			e.printStackTrace();
		}
	}

	public static void upload(String filename) {
		DbxRequestConfig config = new DbxRequestConfig("JavaTutorial/1.0", Locale.getDefault().toString());
		//Carol's
		//DbxClient client = new DbxClient(config, "ZR8DcQDH-_IAAAAAAAK_8kTNcEK0hVxAFrTq8Bo2al2rHTkJRZExLzcZYaSVAUzH");
		DbxClient client = new DbxClient(config, "AGHswENOpB4AAAAAAAAuQDgGZ2mwOi3s85LsU64Z4Je_K25I-ro-q499uMOPqH3F");
		File inputFile = new File(filename);
		FileInputStream inputStream = null;
		try {
			inputStream = new FileInputStream(inputFile);
			DbxEntry.File uploadedFile = client.uploadFile("/" + filename, DbxWriteMode.add(), inputFile.length(),
					inputStream);
			Starter.print("Uploaded: " + uploadedFile.toString());

		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (DbxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			try {
				inputStream.close();
			} catch (IOException e) {

				e.printStackTrace();
			}
		}
	}
}