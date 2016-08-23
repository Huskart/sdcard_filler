package com.zexuan.filler.utils;

import java.io.File;
import java.io.IOException;

/**
 * Created by zexuan on 2016/8/23.
 */
public class FileUtil {

    public static boolean deleteFolder(String sPath) {
        File file = new File(sPath);
        if (!file.exists()) {
            return false;
        } else {
            // 判断是否为文件
            if (file.isFile()) {
                return deleteDestFile(sPath);
            } else {
                return deleteDirectory(sPath);
            }
        }
    }

    public static boolean deleteDirectory(String path) {
        boolean flag;
        //如果path不以文件分隔符结尾，自动添加文件分隔符
        if (!path.endsWith(File.separator)) {
            path = path + File.separator;
        }
        File dirFile = new File(path);
        //如果dir对应的文件不存在，或者不是一个目录，则退出
        if (!dirFile.exists() || !dirFile.isDirectory()) {
            return false;
        }
        flag = true;
        //删除文件夹下的所有文件(包括子目录)
        File[] files = dirFile.listFiles();
        for (File file : files) {
            //删除子文件
            if (file.isFile()) {
                flag = deleteDestFile(file.getAbsolutePath());
                if (!flag) break;
            } //删除子目录
            else {
                flag = deleteDirectory(file.getAbsolutePath());
                if (!flag) break;
            }
        }
        if (!flag) return false;
        //删除当前目录
        return dirFile.delete();
    }

    public static boolean deleteDestFile(String sPath) {
        boolean flag = false;
        File file = new File(sPath);
        if (file.isFile() && file.exists()) {
            if (file.delete()) {
                flag = true;
            } else {
                flag = true;
            }
        }
        return flag;
    }

    public static boolean copyFile(String src, String dest) {
        File test = new File(dest);
        if (!test.exists()) {
            test.mkdirs();
        }
        Process p;
        try {
            p = Runtime.getRuntime().exec("cp " + src + " " + dest);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        try {
            p.waitFor();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return true;
    }

    public static boolean createDir(String path) {
        File file = new File(path);
        if(!file.exists()){
            return file.mkdirs();
        }
        return false;
    }

}
