package com.sibyl.HttpFileDominator.utils;

import android.content.ContentResolver;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import com.sibyl.HttpFileDominator.UriInterpretation;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.zip.Adler32;
import java.util.zip.CheckedOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class FileZipper implements Runnable {
    private void s(String s2) { // an alias to avoid typing so much!
        Log.d(Util.myLogName, s2);
    }

    private OutputStream dest;
    private ArrayList<UriInterpretation> inputUriInterpretations;
    private Boolean atLeastOneDirectory = false;
    private ContentResolver contentResolver;
    private HashSet<String> fileNamesAlreadyUsed = new HashSet<String>();
    private String trashPath = "";//垃圾前缀地址，（其实压缩的时候不需要这么深的目录，把前面的目录给去掉


    public FileZipper(OutputStream dest, ArrayList<UriInterpretation> inputUriInterpretations, ContentResolver contentResolver) {
        /*
         * // get a list of files from current directory File f = new File(".");
         * String inputFiles[] = f.list();
         */
        this.dest = dest;
        this.inputUriInterpretations = inputUriInterpretations;
        this.contentResolver = contentResolver;

    }

    @Override
    public void run() {
        int BUFFER = 4096;

        try {
            // FileOutputStream dest = new
            // FileOutputStream("c:\\zip\\myfigs.zip");
            s("Initializing ZIP");

            CheckedOutputStream checksum = new CheckedOutputStream(dest,
                    new Adler32());

            ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(
                    checksum));

            // out.setMethod(method);
            // out.setLevel(1) ;
            byte data[] = new byte[BUFFER];
            for (UriInterpretation thisUriInterpretation : inputUriInterpretations) {
                addFileOrDirectory(BUFFER, out, data, thisUriInterpretation);
            }

            out.close();
            s("Zip Done. Checksum: " + checksum.getChecksum().getValue());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void addFileOrDirectory(int BUFFER, ZipOutputStream out, byte[] data,
                            UriInterpretation uriFile) throws FileNotFoundException,
            IOException {
        if (uriFile.isDirectory()) {
            addDirectory(BUFFER, out, data, uriFile);
        } else {
            addFile(BUFFER, out, data, uriFile);
        }
    }

    void addDirectory(int BUFFER, ZipOutputStream out, byte[] data,
                      UriInterpretation uriFile) throws FileNotFoundException,
            IOException {
        atLeastOneDirectory = true;
        String directoryPath = uriFile.getUri().getPath();
        if (directoryPath.charAt(directoryPath.length() - 1) != File.separatorChar) {
            directoryPath += File.separatorChar;
        }
        //割出多余的上级目录。。。（/storage/mulated/0）
        if (TextUtils.isEmpty(trashPath)){
            trashPath = splitOutTrashPath(directoryPath);
        }

		ZipEntry entry = new ZipEntry(directoryPath.replace(trashPath,""));
        out.putNextEntry(entry);

        s("Adding Directory: " + directoryPath);
        File f = new File(directoryPath);
        String[] theFiles = f.list();
        if (theFiles != null) {
            for (String aFilePath : theFiles) {
                if (!aFilePath.equals(".") && !aFilePath.equals("..")) {
                    String fixedFileName = "file://" + directoryPath
                            + aFilePath;
                    Uri aFileUri = Uri.parse(fixedFileName);
                    UriInterpretation uriFile2 = new UriInterpretation(aFileUri, contentResolver);
                    addFileOrDirectory(BUFFER, out, data, uriFile2);
                }
            }
        }

    }

    void addFile(int BUFFER, ZipOutputStream out, byte[] data,
                 UriInterpretation uriFile) throws FileNotFoundException,
            IOException {
        BufferedInputStream origin;
        s("Adding File: " + uriFile.getUri().getPath() + " -- " + uriFile.getName());
        origin = new BufferedInputStream(uriFile.getInputStream(), BUFFER);

        ZipEntry entry = new ZipEntry(getFileName(uriFile));

        out.putNextEntry(entry);
        int count;
        while ((count = origin.read(data, 0, BUFFER)) != -1) {
            out.write(data, 0, count);
        }
        origin.close();
    }

    String getFileName(UriInterpretation uriFile) {
        /*  The Android Galary sends us two files with the same name, we must make them unique or we get a
         *  "java.util.zip.ZipException: duplicate entry: x.gif" exception
         */
        String fileName = getFileNameHelper(uriFile);
        while (!fileNamesAlreadyUsed.add(fileName)) {
            // fileNamesAlreadyUsed.add returns TRUE if the file was added to the HashSet
            // false it it was already there.
            // in this case we must change the filename, to keep it unique
            fileName = "_" + fileName;
        }
        return fileName;
    }

    private String getFileNameHelper(UriInterpretation uriFile) {
        /*	Galery Sends uri.getPath() with values like /external/images/media/16458
         *  while urlFile.name returns IMG_20120427_120038.jpg
         *
         *  since such name has no directory info, that would break real directories
         */
        if (atLeastOneDirectory && !TextUtils.isEmpty(trashPath)) {
//            return uriFile.getUri().getPath().substring(1);
            return uriFile.getUri().getPath().replace(trashPath,"");//干掉前面的多余路径名
        }
        return uriFile.getName();
    }

    /***
     * 割出冗余的路径字符串
     */
    private String splitOutTrashPath(String dirPath) {
        if (dirPath.endsWith("/")) {
            dirPath = dirPath.substring(0, dirPath.length() - 1);
        }
        return dirPath.substring(0, dirPath.lastIndexOf("/") + 1);//因为后面要传到ZipEntry里的路径前面不需要带“/”，所以+1把后面的斜杠也干掉
    }
}
