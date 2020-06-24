/*
Copyright (c) 2011, Marcos Diez --  marcos AT unitron.com.br
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright
      notice, this list of conditions and the following disclaimer.
 * Neither the name of  Marcos Diez nor the
      names of its contributors may be used to endorse or promote products
      derived from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY
DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

/**
 * Title: A simple Webserver Tutorial NO warranty, NO guarantee, MAY DO damage
 * to FILES, SOFTWARE, HARDWARE!! Description: This is a simple tutorial on
 * making a webserver posted on http://turtlemeat.com . Go there to read the
 * tutorial! This program and sourcecode is free for all, and you can copy and
 * modify it as you like, but you should give credit and maybe a link to
 * turtlemeat.com, you know R-E-S-P-E-C-T. You gotta respect the work that has
 * been put down.
 * <p>
 * Copyright: Copyright (c) 2002 Company: TurtleMeat
 *
 * @author: Jon Berg <jon.berg[on_server]turtlemeat.com
 * @version 1.0
 */

package com.sibyl.httpfiledominator;

import android.util.Log;

import com.hjq.permissions.BuildConfig;
import com.sibyl.httpfiledominator.activities.BaseActivity;
import com.sibyl.httpfiledominator.utils.FileZipper;
import com.sibyl.httpfiledominator.utils.Util;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class HttpServerConnection implements Runnable {

    private BaseActivity launcherActivity;
    private UriInterpretation theUriInterpretation;
    private Socket connectionSocket;
    private ArrayList<UriInterpretation> fileUriZ;
    private String ipAddress = "";
    public HttpServerConnection(ArrayList<UriInterpretation> fileUris, Socket connectionSocket, BaseActivity launcherActivity) {
        this.fileUriZ = fileUris;
        this.connectionSocket = connectionSocket;
        this.launcherActivity = launcherActivity;
    }

    private static String httpReturnCodeToString(int return_code) {
        switch (return_code) {
            case 200:
                return "200 OK";
            case 302:
                return "302 Moved Temporarily";
            case 400:
                return "400 Bad Request";
            case 403:
                return "403 Forbidden";
            case 404:
                return "404 Not Found";
            case 500:
                return "500 Internal Server Error";
            case 501:
            default:
                return "501 Not Implemented";
        }
    }

    public void run() {
        ipAddress = getClientIpAddress();

        //SasukeTodo 先注释掉，后面把Activity整好了记得打开
//        launcherActivity.sendConnectionStartMessage(ipAddress);

        try {
            InputStream theInputStream;
            try {
                theInputStream = connectionSocket.getInputStream();
            } catch (IOException e1) {
                s("Error getting the InputString from connection socket.");
                e1.printStackTrace();
                return;
            }

            OutputStream theOutputStream;
            try {
                theOutputStream = connectionSocket.getOutputStream();
            } catch (IOException e1) {
                s("Error getting the OutputStream from connection socket.");
                e1.printStackTrace();
                return;
            }

            BufferedReader input = new BufferedReader(new InputStreamReader(
                    theInputStream));

            DataOutputStream output = new DataOutputStream(theOutputStream);
            http_handler(input, output);
            try {
                output.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        } finally {
            s("Closing connection.");
            //SasukeTodo 先注释掉，后面把Activity整好了记得打开
//            launcherActivity.sendConnectionEndMessage(ipAddress);
        }
    }

    private String getClientIpAddress() {
        InetAddress client = connectionSocket.getInetAddress();
        return client.getHostAddress() + "/" + client.getHostName();
    }

    // our implementation of the hypertext transfer protocol
    // its very basic and stripped down
    private void http_handler(BufferedReader input, DataOutputStream output) {
        String header;
        try {
            header = input.readLine();
        } catch (IOException e1) {
            e1.printStackTrace();
            return;
        }
        String upperCaseHeader = header.toUpperCase();
        Boolean sendOnlyHeader = false;

        if (upperCaseHeader.startsWith("HEAD")) {
            sendOnlyHeader = true;
        } else {
            if (!upperCaseHeader.startsWith("GET")) {
                dealWithUnsupportedMethod(output);
                return;
            }
        }

        String path = getRequestedFilePath(header);

        if (path == null || path == "") {
            s("path is null!!!");
            return;
        }
        if (fileUriZ == null) {
            s("fileUri is null");
            return;
        }

        String fileUriStr = fileUriZ.size() == 1 ? fileUriZ.get(0).getUri().toString()
                : fileUriZ.toString();

        s("Client requested: [" + path + "][" + fileUriStr + "]");

        if (path.equals("/favicon.ico")) { // we have no favicon
            shareFavIcon(output);
            return;
        }

        if (fileUriStr.startsWith("http://")
                || fileUriStr.startsWith("https://")
                || fileUriStr.startsWith("ftp://")
                || fileUriStr.startsWith("maito:")
                || fileUriStr.startsWith("callto:")
                || fileUriStr.startsWith("skype:")) {
            // we will work as a simple URL redirector
            redirectToFinalPath(output, fileUriStr);
            return;
        }

        try {
            theUriInterpretation = fileUriZ.get(0);
        } catch (java.lang.SecurityException e) {
            e.printStackTrace();
            s("Share Via HTTP has no permition to read such file.");
            return;
        }
        if (path.equals("/")) {
            shareRootUrl(output);
            return;
        }
        shareOneFile(output, sendOnlyHeader, fileUriStr);
    }

    private void shareOneFile(DataOutputStream output, Boolean sendOnlyHeader, String fileUriStr) {

        InputStream requestedfile = null;

        if (!theUriInterpretation.isDirectory()) {
            try {
                requestedfile = theUriInterpretation.getInputStream();
            } catch (FileNotFoundException e) {
                try {
                    s("I couldn't locate file. I am sending the input as text/plain");
                    // instead of sending a 404, we will send the contact as text/plain
                    output.writeBytes(construct_http_header(200, "text/plain"));
                    output.writeBytes(fileUriStr);


                    // if you could not open the file send a 404
                    //s("Sending HTTP ERROR 404:" + e.getMessage());
                    //output.writeBytes(construct_http_header(404, null));
                    return;
                } catch (IOException e2) {
                    s("errorX:" + e2.getMessage());
                    return;
                }
            } // print error to gui
        }
        // happy day scenario

        String outputString = construct_http_header(200,
                theUriInterpretation.getMime());

        try {
            output.writeBytes(outputString);

            // if it was a HEAD request, we don't print any BODY
            if (!sendOnlyHeader) {

                if (theUriInterpretation.isDirectory() || fileUriZ.size() > 1) {
                    FileZipper zz = new FileZipper(output, fileUriZ, MyApp.getInstance().getContentResolver());
                    zz.run();
                } else {
                    byte[] buffer = new byte[8 * 1024 * 1024];//8M
                    for (int n; (n = requestedfile.read(buffer)) != -1; ) {
                        output.write(buffer, 0, n);
                    }
                }

            }
            requestedfile.close();
        } catch (IOException e) {
        }
    }

    private void redirectToFinalPath(DataOutputStream output, String thePath) {

        String redirectOutput = construct_http_header(302, null, thePath);
        try {
            // if you could not open the file send a 404
            output.writeBytes(redirectOutput);
            // close the stream
        } catch (IOException e2) {
        }
    }

    private void shareRootUrl(DataOutputStream output) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd=HH-mm-ss");

        if (theUriInterpretation.isDirectory()) {
            redirectToFinalPath(output, "HTTP_" + format.format(new Date()) + ".ZIP"/*theUriInterpretation.getName() + ".ZIP"*/);
            return;
        }

        if (fileUriZ.size() == 1) {
            redirectToFinalPath(output, theUriInterpretation.getName());
        } else {
            redirectToFinalPath(output,
                    "HTTP_" + format.format(new Date()) + ".ZIP");
        }
    }

    private void shareFavIcon(DataOutputStream output) {
        try {
            // if you could not open the file send a 404
            output.writeBytes(construct_http_header(404, null));
            // close the stream
        } catch (IOException e2) {
        }
    }

    private String getRequestedFilePath(String inputHeader) {
        String path;
        String tmp2 = new String(inputHeader);

        // tmp contains "GET /index.html HTTP/1.0 ......."
        // find first space
        // find next space
        // copy whats between minus slash, then you get "index.html"
        // it's a bit of dirty code, but bear with me...
        int start = 0;
        int end = 0;
        for (int a = 0; a < tmp2.length(); a++) {
            if (tmp2.charAt(a) == ' ' && start != 0) {
                end = a;
                break;
            }
            if (tmp2.charAt(a) == ' ' && start == 0) {
                start = a;
            }
        }
        path = tmp2.substring(start + 1, end); // fill in the path
        return path;
    }

    private void dealWithUnsupportedMethod(DataOutputStream output) {
        try {
            output.writeBytes(construct_http_header(501, null));
        } catch (Exception e3) { // if some error happened catch it
            s("_error:" + e3.getMessage());
        } // and display error
    }

    private void s(String s2) { // an alias to avoid typing so much!
        Log.d(Util.myLogName, "[" + ipAddress + "] " + s2);
    }

    private String construct_http_header(int return_code, String mime) {
        return construct_http_header(return_code, mime, null);
    }

    // it is not always possible to get the file size :(
    private String getFileSizeHeader() {
        if (theUriInterpretation == null) {
            return "";
        }
        if (fileUriZ.size() == 1 && theUriInterpretation.getSize() > 0) {
            return "Content-Length: "
                    + Long.toString(theUriInterpretation.getSize()) + "\r\n";
        }
        return "";
    }

    private String generateRandomFileNameForTextPlainContent() {
        return "StringContent-" + Math.round((Math.random() * 100000000)) + ".txt";
    }

    // this method makes the HTTP header for the response
    // the headers job is to tell the browser the result of the request
    // among if it was successful or not.
    private String construct_http_header(int return_code, String mime,
                                         String location) {

        StringBuilder output = new StringBuilder();
        output.append("HTTP/1.0 ");
        output.append(httpReturnCodeToString(return_code) + "\r\n");
        output.append(getFileSizeHeader());
        //解决：请求头Date不能含中文，否则会报 ERR_INVALID_HTTP_RESPONSE 无效返回。所以这里Locale必须要填 Locale.ENGLISH
        SimpleDateFormat format = new SimpleDateFormat(
                "EEE, dd MMM yyyy HH:mm:ss zzz", Locale.ENGLISH);


        output.append("Date: " + format.format(new Date()) + "\r\n");

        output.append("Connection: close\r\n"); // we can't handle persistent
        // connections
        output.append("Server: ").append(Util.myLogName).append(" ").append(BuildConfig.VERSION_NAME).append("\r\n");

        if (location == null && return_code == 302) {
            location = generateRandomFileNameForTextPlainContent();
        }
        if (location != null) {
            try {
                //解决：下载时中文文件名乱码的问题
                location = URLEncoder.encode(location, "UTF-8").replaceAll("\\+","%20");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            // we don't want cache for the root URL
//            try {
//                int pos = location.indexOf("://");
//                if (pos > 0 && pos < 10) {
//                    // so russians can download their files as well :)
//                    // but if a protocol like http://, than we may as well redirect
//                    location = URLEncoder.encode(location, "UTF-8");
//                    s("after urlencode location:" + location);
//                }
//            } catch (UnsupportedEncodingException e) {
//                s(Log.getStackTraceString(e));
//            }

            output.append("Location: ").append(location).append("\r\n"); // server name

//            output.append("Connection: keep-alive\r\n");
            output.append("Expires: Tue, 03 Jul 2001 06:00:00 GMT\r\n");
            output.append("Cache-Control: no-store, no-cache, must-revalidate, max-age=0\r\n");
            output.append("Cache-Control: post-check=0, pre-check=0\r\n");
            output.append("Pragma: no-cache\r\n");
        }
        if (mime != null) {
            if (fileUriZ.size() > 1) {
                mime = "multipart/x-zip";
            }
            output.append("Content-Type: ").append(mime).append("\r\n");
            if (UriInterpretation.isMimeVideoType(mime)){//如果是多媒体文件
//                Accept-Ranges:     bytes
//                Content-Length:    2097152
//                Content-Range:     bytes 0-2097151/38696534
                //解决：以下响应头设置使支持视频文件的进度条拖动，不设置的话会无法拖动进度条
                //参考资料：https://blog.csdn.net/hudaijun/article/details/87456583
                output.append("Accept-Ranges:  bytes\r\n");
                output.append("Content-Range:  bytes 0-10485760/" + theUriInterpretation.getSize() + "\r\n");
            }
        }
        output.append("\r\n");
        return output.toString();
    }


    /**
     * <pre>
     * 浏览器下载文件时需要在服务端给出下载的文件名，当文件名是ASCII字符时没有问题
     * 当文件名有非ASCII字符时就有可能出现乱码
     *
     * 这里的实现方式参考这篇文章
     * http://blog.robotshell.org/2012/deal-with-http-header-encoding-for-file-download/
     *
     * 最终设置的response header是这样:
     *
     * Content-Disposition: attachment;
     *                      filename="encoded_text";
     *                      filename*=utf-8''encoded_text
     *
     * 其中encoded_text是经过RFC 3986的“百分号URL编码”规则处理过的文件名
     * </pre>
     * @param response
     * @param filename
     * @return
     */
//    public static void setFileDownloadHeader(HttpServletResponse response, String filename) {
//        String headerValue = "attachment;";
//        headerValue += " filename=\"" + encodeURIComponent(filename) +"\";";
//        headerValue += " filename*=utf-8''" + encodeURIComponent(filename);
//        response.setHeader("Content-Disposition", headerValue);
//    }

    /**
     * <pre>
     * 符合 RFC 3986 标准的“百分号URL编码”
     * 在这个方法里，空格会被编码成%20，而不是+
     * 和浏览器的encodeURIComponent行为一致
     * </pre>
     * @param value
     * @return
     */
    public static String encodeURIComponent(String value) {
        try {
            return URLEncoder.encode(value, "UTF-8").replaceAll("\\+", "%20");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return null;
        }
    }

}