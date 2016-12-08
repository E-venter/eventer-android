package com.example.gabriel.testgooglemaps;

import android.util.Base64;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class HttpPost {

    private final static String CRLF = "\r\n";

    private String boundary;

    private URL url;
    private ByteArrayOutputStream buffer;
    String token = null, client = null, uid = null;

    Map<String, List<String>> responseHeader = null;
    String response = null;
    int responseCode = -1;


    public HttpPost(URL url, String token, String client, String uid) {
        // Generate random boundary
        // Boundary length: max. 70 characters (not counting the two leading hyphens)
        byte[] random = new byte[40];
        new Random().nextBytes(random);
        boundary = Base64.encodeToString(random, Base64.DEFAULT);

        // Init buffer
        buffer = new ByteArrayOutputStream();

        this.url    = url;
        this.token  = token;
        this.client = client;
        this.uid    = uid;
    }

    public void setTarget(URL url) {
        this.url = url;
    }

    public void add(NameValuePair nvp) throws IOException {
        //addToBuffer("--" + boundary + CRLF);
        //addToBuffer("Content-Disposition: form-data; name=\"" + key + "\"" + CRLF);
        //addToBuffer("Content-Type: text/plain; charset=UTF-8" + CRLF + CRLF);
        //addToBuffer(key + "=" + value + CRLF);

        values.add(nvp);
    }

    public void add(String key, String value) throws IOException {
        values.add(new NameValuePair(key, value));
    }

    public int send() throws IOException, URISyntaxException {
        return otherPost(url.toURI().toString(), token, client, uid);
/*
        // Add boundary end
        addToBuffer("--" + boundary + "--" + CRLF);

        // Open url connection
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setDoOutput(true);
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Connection", "Keep-Alive");
        connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
        connection.setRequestProperty("User-Agent", "Google App Engine");

        // Open data output stream
        DataOutputStream request = new DataOutputStream(connection.getOutputStream());
        request.write(buffer.toByteArray());
        request.flush();
        request.close();

        //connection.getInputStream().re

        // Close connection
        connection.disconnect();

        return "";*/
    }

    private void addToBuffer(String string) throws IOException {
        buffer.write(string.getBytes());
    }

    private void addToBuffer(byte[] bytes) throws IOException {
        buffer.write(bytes);
    }

    public String workingPost(String address) throws IOException {
        byte[] postData       = buffer.toByteArray();
        int    postDataLength = postData.length;
        String request        = address;
        URL    url            = new URL( request );

        HttpURLConnection conn= (HttpURLConnection) url.openConnection();

        conn.setDoOutput( true );
        conn.setInstanceFollowRedirects( false );

        conn.setRequestMethod( "POST" );
        conn.setRequestProperty( "Content-Type", "application/x-www-form-urlencoded");
        conn.setRequestProperty( "charset", "utf-8");
        conn.setRequestProperty( "Content-Length", Integer.toString( postDataLength ));

        conn.setUseCaches( false );

        try( DataOutputStream wr = new DataOutputStream( conn.getOutputStream())) {
            wr.write( postData );
        }

        try(DataInputStream rd = new DataInputStream( conn.getInputStream() )){

            byte[] buff = new byte[1000];
            int numBytes;
            String completeLine = "";
            while((numBytes = rd.read(buff)) != -1){
                System.out.println("READ " + numBytes);
                for(int i = 0; i < numBytes; i++){
                    completeLine += (char) buff[i];
                }
            }

            //System.out.println(completeFile);
            System.out.println(completeLine);
            return completeLine;
        }
    }

    ArrayList<NameValuePair> values = new ArrayList<>();
    byte[] image;
    String imageName;

    public int otherPost(String address, String token, String client, String uid) throws IOException {
        //return null;

        byte[] postData       = buffer.toByteArray();
        int    postDataLength = postData.length;
        String request        = address;
        URL    url            = new URL( request );

        String boundary = Long.toHexString(System.currentTimeMillis()); // Just generate some unique random value.
        String CRLF = "\r\n"; // Line separator required by multipart/form-data.
        String charset = "UTF-8";

        HttpURLConnection conn= (HttpURLConnection) url.openConnection();
        conn.setDoOutput( true );
        conn.setRequestProperty( "Content-Type", "multipart/form-data; boundary=" + boundary);
        conn.setRequestProperty( "access-Token", token);
        conn.setRequestProperty( "client", client);
        conn.setRequestProperty( "uid", uid);

        try (
                OutputStream output = conn.getOutputStream();
                PrintWriter writer = new PrintWriter(new OutputStreamWriter(output, charset), true)
        ) {
            for(NameValuePair nameValuePair : values){
                System.out.println(nameValuePair.name + " : " + nameValuePair.value);
                // Send normal param.
                writer.append("--" + boundary).append(CRLF);
                writer.append("Content-Disposition: form-data; name=\"" + nameValuePair.name + "\"").append(CRLF);
                writer.append("Content-Type: text/plain; charset=" + charset).append(CRLF);
                writer.append(CRLF).append(nameValuePair.value).append(CRLF).flush();
            }

            if(image != null) {
                // Send binary file.
                writer.append("--" + boundary).append(CRLF);
                writer.append("Content-Disposition: form-data; name=\"binary_file\"; filename=\"" + imageName + "\"").append(CRLF);
                writer.append("Content-Type: " + URLConnection.guessContentTypeFromName(imageName)).append(CRLF);
                writer.append("Content-Transfer-Encoding: binary").append(CRLF);
                writer.append(CRLF).flush();

                output.write(image);

                output.flush(); // Important before continuing with writer!
                writer.append(CRLF).flush(); // CRLF is important! It indicates end of boundary.
            }

            // End of multipart/form-data.
            writer.append("--" + boundary + "--").append(CRLF).flush();
        }

        responseHeader = conn.getHeaderFields();

        try(DataInputStream rd = new DataInputStream( conn.getInputStream() )){

            byte[] buff = new byte[1000];
            int numBytes;
            String completeLine = "";
            while((numBytes = rd.read(buff)) != -1){
                System.out.println("READ " + numBytes);
                for(int i = 0; i < numBytes; i++){
                    completeLine += (char) buff[i];
                }
            }

            //System.out.println(completeFile);
            System.out.println(completeLine);
            response = completeLine;
        }catch (Exception e){

        }

        responseCode = conn.getResponseCode();
        return responseCode;
    }
}
