package com.sv.speakerverify.utils;

import android.content.Context;
import android.content.res.AssetManager;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * @author phx
 * @create 2020-10-20-20:07
 */
public class JsonUtils {

  /**
   * 得到json文件中的内容
   *
   * @param context
   * @param fileName json 文件名 需要在assets资源中
   * @return
   */
  public static String getJson(Context context, String fileName) {
    StringBuilder stringBuilder = new StringBuilder();
    //获得assets资源管理器
    AssetManager assetManager = context.getAssets();
    //使用IO流读取json文件内容
    try {
      BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(
          assetManager.open(fileName), "UTF-8"));
      String line;
      while ((line = bufferedReader.readLine()) != null) {
        stringBuilder.append(line.trim());
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
    return stringBuilder.toString();
  }

}
