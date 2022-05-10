package com.vishee.arcface.config;

public class FaceArchive {
    /**
     * 集合名称(库名)
     */
    public static final String COLLECTION_NAME = "face_archive";
    /**
     * 分片数量
     */
    public static final Integer SHARDS_NUM = 8;
    /**
     * 分区数量
     */
    public static final Integer PARTITION_NUM = 4;

    /**
     * 分区前缀
     */
    public static final String PARTITION_PREFIX = "shards_";
    /**
     * 特征值长度
     */
    public static final Integer FEATURE_DIM = 256;

    /**
     * 字段
     */
    public static class Field {
        /**
         * 档案id
         */
        public static final String ARCHIVE_ID = "face_id";
        /**
         * 小区id
         */
        public static final String ORG_ID = "user_id";
        /**
         * 档案特征值
         */
        public static final String ARCHIVE_FEATURE = "archive_feature";
    }


    public static String getPartitionName(Long faceId) {
        return PARTITION_PREFIX + (faceId % PARTITION_NUM);
    }
}
