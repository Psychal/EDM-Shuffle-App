package com.mwr.edmshuffle;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;

class WebRequest {
    static String response = null;
    final static int GETRequest = 1;
    private final static int POSTRequest = 2;

    //Constructor with no parameter
    WebRequest() {
    }

//     Making web service call
//     @url - url to make web request
//     @requestmethod - http request method
//     @params - http request params
     String makeWebServiceCall(String urladdress, int GET, HashMap<String, String> params) {
        URL urlWeb;
        String response = "";
        try {
            urlWeb = new URL(urladdress);
            HttpURLConnection conn = (HttpURLConnection) urlWeb.openConnection();
            conn.setReadTimeout(15001);
            conn.setConnectTimeout(15001);
            conn.addRequestProperty("Referer", "http://edm-shuffle.com");
            conn.setDoInput(true);
            conn.setDoOutput(true);
            if (GET == POSTRequest) {
                conn.setRequestMethod("POST");
            } else if (GET == GETRequest) {
                conn.setRequestMethod("GET");
            }
            if (params != null) {
                OutputStream ostream = conn.getOutputStream();
                BufferedWriter writer = new BufferedWriter(
                        new OutputStreamWriter(ostream, "UTF-8"));
                StringBuilder requestresult = new StringBuilder();
                boolean first = true;
                for (Map.Entry<String, String> entry : params.entrySet()) {
                    if (first)
                        first = false;
                    else
                        requestresult.append("&");
                    requestresult.append(URLEncoder.encode(entry.getKey(), "UTF-8"));
                    requestresult.append("=");
                    requestresult.append(URLEncoder.encode(entry.getValue(), "UTF-8"));
                }
                writer.write(requestresult.toString());
                writer.flush();
                writer.close();
                ostream.close();
            }
            int reqresponseCode = conn.getResponseCode();
            if (reqresponseCode == HttpsURLConnection.HTTP_OK) {
                String line;
                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                while ((line = br.readLine()) != null) {
                    response += line;
                    System.out.println(line +"line");
                }
            } else {
                response = "";
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return response;
    }
}

