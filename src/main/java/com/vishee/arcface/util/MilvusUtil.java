package com.vishee.arcface.util;

import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class MilvusUtil {


 
    /**
     * 虹软byte[]特征值转List<Float>
     * @param bytes
     * @return
     */
    public static List<Float> arcsoftToFloat(byte[] bytes) {
        List<Float> list = Lists.newArrayList();
        for (int i = 8 + 1024; i < bytes.length; i += 4) {
            byte[] bytes1 = {bytes[i], bytes[i + 1], bytes[i + 2], bytes[i + 3]};
            list.add(byte2float(bytes1, 0));
        }
        return list;
    }
 
    /**
     * 字节数组转Float
     * @param b
     * @param index
     * @return
     */
    public static float byte2float(byte[] b, int index) {
        int l;
        l = b[index];
        l &= 0xff;
        l |= ((long) b[index + 1] << 8);
        l &= 0xffff;
        l |= ((long) b[index + 2] << 16);
        l &= 0xffffff;
        l |= ((long) b[index + 3] << 24);
        return Float.intBitsToFloat(l);
    }
 
}