package com.jack.sniffing;



import static com.jack.sniffing.SniffingFilter.DEFAULT_TYPE;

import androidx.annotation.NonNull;



import javax.net.ssl.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

public class Util {


    /**
     *
     * @param url
     * @return [ url,contentType,contentLength]
     */
    public static Object[] getContent(String url) {
        Object[] objects = new Object[3];
        HttpURLConnection urlConnection = null;
        try {
            urlConnection = (HttpURLConnection) new URL(url).openConnection();
            if (url.startsWith("https")) {
                HttpsURLConnection https = (HttpsURLConnection) urlConnection;
                // 方式一，相信所有
                trustAllHosts(https);
                // 方式二，覆盖默认验证方法
                https.getHostnameVerifier();
                // 方式三，不校验
                https.setHostnameVerifier(DO_NOT_VERIFY);
            }
            urlConnection.setRequestMethod("HEAD");
            int responseCode = urlConnection.getResponseCode();
            if (responseCode == 200) {
                objects[0] = urlConnection.getURL();
                objects[1] = urlConnection.getContentType();
                objects[2] = urlConnection.getContentLength();
            }
            LogUtil.e("SniffingUtil", "getContent code = " + responseCode);
        } catch (Exception e) {
            LogUtil.e("SniffingUtil", "getContent error = " + e.toString());
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }
        if (objects[0] == null) objects[0] = -1;
        if (objects[1] == null) objects[1] = "";
        return objects;
    }

    /**
     * 覆盖java默认的证书验证
     */
    private static final TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
            return new java.security.cert.X509Certificate[]{};
        }

        public void checkClientTrusted(X509Certificate[] chain, String authType)
                throws CertificateException {
        }

        public void checkServerTrusted(X509Certificate[] chain, String authType)
                throws CertificateException {
        }
    }};

    /**
     * 设置不验证主机
     */
    private static final HostnameVerifier DO_NOT_VERIFY = new HostnameVerifier() {
        public boolean verify(String hostname, SSLSession session) {
            return true;
        }
    };

    /**
     * 信任所有
     *
     * @param connection
     * @return
     */
    private static SSLSocketFactory trustAllHosts(HttpsURLConnection connection) {
        SSLSocketFactory oldFactory = connection.getSSLSocketFactory();
        try {
            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            SSLSocketFactory newFactory = sc.getSocketFactory();
            connection.setSSLSocketFactory(newFactory);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return oldFactory;
    }

    /**
     * 获取H5 内容字符串
     */
    public static String getHtmlContent(@NonNull String url) {
        HttpURLConnection httpConn = null;
        BufferedReader in = null;
        PrintWriter out = null;
        StringBuffer result = new StringBuffer("");
        try {
            URL realUrl = new URL(url);
            // 打开和URL之间的连接
            httpConn = (HttpURLConnection) realUrl.openConnection();
            httpConn.setRequestMethod("GET");
            httpConn.setConnectTimeout(Integer.valueOf(30000));
            httpConn.setReadTimeout(Integer.valueOf(30000));
            httpConn.setRequestProperty("Accept-Charset", "UTF-8");
            httpConn.setRequestProperty("Connection", "keep-alive");
            httpConn.setRequestProperty("Content-Type","application/x-www-form-urlencoded");
            httpConn.setRequestProperty("Content-Type", "text/javascript");
            httpConn.setRequestProperty("Accept", "*/*");

            // 发送POST必须设置下面两行
            httpConn.setDoInput(true);
            httpConn.setDoOutput(true);
            // 获取HttpURLConnection对象对应的输出流，此处应设置默认字符集
            out = new PrintWriter(new OutputStreamWriter(
                    httpConn.getOutputStream(), "UTF-8"));
            // flush输出流的缓冲
            out.flush();
            // 定义BufferedReader输入流来读取URL的响应
            in = new BufferedReader(new InputStreamReader(
                    httpConn.getInputStream(), "UTF-8"));
            String line = "";
            while ((line = in.readLine()) != null) {
                result.append(line);//可以逐行分析
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
                if (in != null) {
                    in.close();
                }
                if (httpConn != null) {
                    httpConn.disconnect();
                }
            } catch (IOException e) {
                e.printStackTrace();
                e.printStackTrace();
            }
        }
        return result.toString();//也可以整个html分析，参考jsoup

    }

}



