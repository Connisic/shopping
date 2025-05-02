package com.owner.shopping_common.util;

import java.util.Random;

public class RandomUtil {
    public static String buildCheckCode(){
        String str="0123456789";
        StringBuilder sb=new StringBuilder();
        Random random = new Random();
        for (int i=0;i<4;i++){
            char num = str.charAt(random.nextInt(str.length()));
            sb.append(num);
        }
        return sb.toString();
    }
    public static double getRandom(double min,double max){
        Random random = new Random();
        double s = random.nextDouble();
        double result = min + s * (max - min);
        return Double.parseDouble(String.format("%.1f",result));
    }
    public static int getRandom(int min,int max){
        Random random = new Random();
        return random.nextInt(max)%(max-min+1) + min;
    }
}
