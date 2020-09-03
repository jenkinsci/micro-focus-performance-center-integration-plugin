package com.microfocus.performancecenter.integration.common.helpers.utils;

import java.nio.file.Path;
import java.nio.file.Paths;

public class Helper {

    public static Path getParent(Path path)
    {
        if(path.getParent() != null)
            return path.getParent();
        return Paths.get(getParent(path.toString()));
    }

    private static String getParent(String strPath)
    {
        char chrSeparatorBackward = '\\';
        char chrSeparatorForward = '/';
        String strPathToHandle = strPath;
        if(strPathToHandle == null || strPathToHandle.isEmpty() || !(strPathToHandle.indexOf(chrSeparatorBackward) != -1 || strPathToHandle.indexOf(chrSeparatorForward)!= -1))
            return "";
        if(strPathToHandle.indexOf(chrSeparatorForward) != -1 )
            strPathToHandle = strPathToHandle.replace(chrSeparatorForward, chrSeparatorBackward);
        if(strPathToHandle.endsWith(String.valueOf(chrSeparatorBackward)))
            strPathToHandle = strPathToHandle.replaceAll("\\$", "");

        int index = strPathToHandle.lastIndexOf(chrSeparatorBackward);
        strPathToHandle = strPathToHandle.substring(0, index);
        return strPathToHandle;
    }
}
