package com.sibyl.httpfiledominator.utils;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD;

/**
 * @author HUANGSHI-PC on 2020-08-03 0003.
 * 剪切板共享
 */
public class ClipServer extends NanoHTTPD {
    public static String html;
    public static String text;
    public static String textRaw;
    public RefreshCallback callback;

    public interface RefreshCallback{
        void run(String newString);
    }

    public ClipServer(int port, String _html,RefreshCallback callback) {
        super(port);
        text = "";
        html = _html;
        this.callback = callback;
    }

//    public static void main(String[] args) {
//        ServerRunner0.run(ClipServer.class);
//    }

    @Override
    public Response serve(IHTTPSession session) {
        super.serve(session);
        if (session.getMethod() == Method.GET) {
            return newFixedLengthResponse(html);
        }
//        Map<String, String> map = session.getParms();
        Map<String, List<String>> map = session.getParameters();

        if (map.get("isUpdate").get(0) == null || !((String) map.get("isUpdate").get(0)).equals("YES")) {
            textRaw = (String) map.get("value").get(0);
            String decodeText = new String(Base64.getDecoder().decode(textRaw), StandardCharsets.UTF_8);
            if (textRaw != null) {
                text = decodeText;
                if (callback != null){
                    callback.run(decodeText);
                }
            }
            return newFixedLengthResponse("OK");
        }
        if (text == null) {
            text = "";
        }
        return newFixedLengthResponse(textRaw);
    }

    public void  startIfNotInUse(){
        try {
//            this.start(2000,true);
            this.start(2000);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String getIpAddress(){
        try {
            for (Enumeration<NetworkInterface> enNetI = NetworkInterface.getNetworkInterfaces(); enNetI
                    .hasMoreElements();) {
                NetworkInterface netI = enNetI.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = netI.getInetAddresses(); enumIpAddr.hasMoreElements();) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (inetAddress instanceof Inet4Address &&!inetAddress.isLoopbackAddress()) {
                        return inetAddress.getHostAddress();
                    }
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
        return "";
    }

}