package com.sibyl.HttpFileDominator.utils;



import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * 自己造JSON数据
 *
 * Created by Sasuke on 2036/3/31.
 *
 * 用来直接调用SD卡里对应fileName.txt的文本文件，
 * 在事先知道接口返回JSON格式的前提下，可以先捏造一个JSON的文本放到SD卡里，起到模拟接口返回，伪造数据的作用
 *
 * 食用方法：
 * 调用网络请求的地方，先调这个loadFromSD()，如果返回null，则说明SD卡里没有提前放txt文件，那就执行原有的网络请求；
 * 如果返回了对应实体，那就说明成功从SD卡里提取了伪造JSON，就通过这个SD卡读取的JSON来进行后续操作，而跳过原有网络请求，
 * 这样就能通过直接控制SD卡上的txt，来实现动态模拟接口返回。
 *
 * common_example：
 *
 * //===========test==========
 BaseResponse res = JsonDominator.loadFromSD(BaseResponse.class);
 if(res != null){
 //......do_anything
 return;
 }
 //===========test==========
 */

public class JsonDominator {
//    public static <T> T loadFromSD(Class<T> clazz){
//        String fileName = clazz.getSimpleName();
//        return loadFromSD(fileName,clazz);
//    }

    /**还要单独写一个type版的，用于 new TypeToken<List<XXX>>() { }.getType()  */
//    public static <T> T loadFromSD(String fileName, Type typeOfT){
//        String sdPath = Environment.getExternalStorageDirectory().getAbsolutePath();
//        File cardFile = new File(sdPath + File.separator + "0カード"+File.separator + "JsonDominator");
//        if(! cardFile.exists()){
//            return null;
//        }
//        File file = new File(cardFile + File.separator + fileName + ".txt");
//        if(! file.exists()){
//            return null;
//        }
//
//        String result = readTextFromFile(file,null);
//        Log.i("JsonDominator","JsonDominator响应报文:"+result);
//        if(TextUtils.isEmpty(result)){
//            return null;
//        }
//        return new Gson().fromJson(result,typeOfT);
//    }

//    public static <T> T loadFromSD(String fileName,Class<T> clazz){
//        String sdPath = Environment.getExternalStorageDirectory().getAbsolutePath();
//        File cardFile = new File(sdPath + File.separator + "0カード"+File.separator + "JsonDominator");
//        if(! cardFile.exists()){
//            return null;
////            cardFile.mkdirs();
//        }
//        File file = new File(cardFile + File.separator + fileName + ".txt");
//        if(! file.exists()){
//            return null;
//        }
//
//        String result = readTextFromFile(file,null);
//        Log.i("JsonDominator","JsonDominator响应报文:"+result);
//        if(TextUtils.isEmpty(result)){
//            return null;
//        }
//        return new Gson().fromJson(result,clazz);
////        try {
////            InputStreamReader is = new InputStreamReader(new FileInputStream(file), "UTF-8");// 考虑到编码格式
////            BufferedReader reader = new BufferedReader(is);
////            StringBuffer sb = new StringBuffer("");
////            String temp;
////            while ((temp = reader.readLine()) != null){
////                sb.append(temp);
////            }
////            reader.close();
////            is.close();
////            Log.i("JsonDominator","JsonDominator响应报文:"+sb.toString());
////            return new Gson().fromJson(sb.toString(),clazz);
////        }catch (FileNotFoundException e){
////            e.printStackTrace();
////            return null;
////        }catch (IOException e){
////            e.printStackTrace();
////            return null;
////        }
//    }

    /**
     * 根据路径读取文件
     * @param file 需要读取的文件file
     * @param clazz 需要转换成的对应实体
     * @param <T>
     * @String suffix需要读取的文件的后缀
     * @return
     */
//    public static <T> T loadFromDir(File file,Class<T> clazz,boolean doUnGzip) {
//        String result = doUnGzip? GZIPUtils.unzip(readTextFromFile(file,null)) : readTextFromFile(file,null);
//        if(TextUtils.isEmpty(result)){
//            return null;
//        }
//        return new Gson().fromJson(result,clazz);
//    }


        /**
         * 单独提出来的用来读出file里的文本的工具（如果是目录，就只提取最新一个文本）
         * @param file
         * @return
         */
    public static String readTextFromFile(File file,FileFilter filter){
        if(! file.exists()){
            return "";
        }
        if(file.isDirectory()){
            List<File> files = Arrays.asList(file.listFiles(filter));
            if(files.size() == 0){
                return "";
            }
            Collections.sort(files, new Comparator<File>() {
                @Override
                public int compare(File lhs, File rhs) {
                    return rhs.getName().compareTo(lhs.getName());
                }
            });
            file = files.get(0);
        }

        try {
            InputStreamReader is = new InputStreamReader(new FileInputStream(file), "UTF-8");// 考虑到编码格式
            BufferedReader reader = new BufferedReader(is);
            StringBuffer sb = new StringBuffer("");
            String temp;
            while ((temp = reader.readLine()) != null){
                temp += '\n';
                sb.append(temp);
            }
            reader.close();
            is.close();
            return sb.toString();
        }catch (FileNotFoundException e){
            e.printStackTrace();
            return "";
        }catch (IOException e){
            e.printStackTrace();
            return "";
        }
    }


    /**
     * 烧入到指定File
     * @param content 写入的内容
     * @param file 目标文件
     * @param doGzip 是否要用GZIP压缩
     * @return
     */
    public static boolean fire2Dir(String content,File file){
        try {
            if(! file.exists()){
                file.createNewFile();
            }
            BufferedWriter writer = new BufferedWriter(new FileWriter(file,false));
            writer.write(content);
            writer.flush();
            writer.close();
            return true;
        }catch (FileNotFoundException e){
            e.printStackTrace();
            return false;
        }catch (IOException e){
            e.printStackTrace();
            return false;
        }
    }

//    public static boolean fire2SD(String content,String fileName){
//        String sdPath = Environment.getExternalStorageDirectory().getAbsolutePath();
//        File cardFile = new File(sdPath + File.separator + "0カード");
//        if(! cardFile.exists()){
//            cardFile.mkdirs();
//        }
//        File file = new File(cardFile + File.separator + fileName + ".txt");
//
//    }
}
