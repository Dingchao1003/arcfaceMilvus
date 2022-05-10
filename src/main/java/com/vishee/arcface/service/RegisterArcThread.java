package com.vishee.arcface.service;

import com.arcsoft.face.FaceEngine;
import com.arcsoft.face.FaceFeature;
import com.arcsoft.face.FaceInfo;
import com.arcsoft.face.enums.ExtractType;
import com.arcsoft.face.toolkit.ImageFactory;
import com.arcsoft.face.toolkit.ImageInfo;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.google.common.collect.Lists;
import com.vishee.arcface.config.ArcFaceEngineRunner;
import com.vishee.arcface.config.FaceArchive;
import com.vishee.arcface.mapper.FaceMapper;
import com.vishee.arcface.util.MilvusUtil;
import io.milvus.client.MilvusServiceClient;
import io.milvus.grpc.DataType;
import io.milvus.grpc.MutationResult;
import io.milvus.param.R;
import io.milvus.param.dml.InsertParam;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author dingchao
 * @date 2022-03-26/16:12
 * @description
 */
@Slf4j
public class RegisterArcThread implements Runnable {

    private List<Map<String, Object>> ListMap;

    private MilvusServiceClient service;


    public RegisterArcThread(List<Map<String, Object>> fileNameList, MilvusServiceClient service) {
        this.ListMap = fileNameList;
        this.service = service;
    }

    @Override
    public void run() {
        try {
            List<InsertParam.Field> fields = new ArrayList<>();
            List<Long> archiveIds = Lists.newArrayList();
            List<Integer> ids = Lists.newArrayList();
            List<List<Float>> floatVectors = Lists.newArrayList();

            ListMap.forEach(x -> {
                archiveIds.add(Long.valueOf(x.get("id").toString()));
                ids.add(((Integer) x.get("id")));
                floatVectors.add(MilvusUtil.arcsoftToFloat((byte[]) x.get("data")));

            });
            //档案ID
            fields.add(new InsertParam.Field(FaceArchive.Field.ARCHIVE_ID, DataType.Int64, archiveIds));
            //小区id
            fields.add(new InsertParam.Field(FaceArchive.Field.ORG_ID, DataType.Int32, ids));
            //特征值
            fields.add(new InsertParam.Field(FaceArchive.Field.ARCHIVE_FEATURE, DataType.FloatVector, floatVectors));
            InsertParam insertParam = InsertParam.newBuilder()
                    .withCollectionName(FaceArchive.COLLECTION_NAME)
                    .withPartitionName(FaceArchive.PARTITION_PREFIX + 1)
                    .withFields(fields)
                    .build();
            R<MutationResult> insert = service.insert(insertParam);


            log.info("| server| insert to milvus success {}", insert);
        } catch (Exception e) {
            log.error("解析人脸出现问题", e);
            e.printStackTrace();
        } finally {

        }


    }


}
