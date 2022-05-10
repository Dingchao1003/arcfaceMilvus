package com.vishee.arcface.config;

import com.vishee.arcface.mapper.FaceMapper;
import io.milvus.client.MilvusServiceClient;
import io.milvus.grpc.DataType;
import io.milvus.grpc.GetIndexBuildProgressResponse;
import io.milvus.param.*;
import io.milvus.param.collection.CreateCollectionParam;
import io.milvus.param.collection.FieldType;
import io.milvus.param.collection.HasCollectionParam;
import io.milvus.param.index.CreateIndexParam;
import io.milvus.param.index.GetIndexBuildProgressParam;
import io.milvus.param.partition.CreatePartitionParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
@Slf4j
public class MilvusConfiguration {


    private String host = "139.224.28.25";

    private Integer port = 19530;

    @Bean
    public MilvusServiceClient milvusServiceClient() {
        ConnectParam connectParam = ConnectParam.newBuilder()
                .withHost(host)
                .withPort(port)
                .build();
        MilvusServiceClient client = new MilvusServiceClient(connectParam);
        createCollection(client);
        return client;
    }


    private void createCollection(MilvusServiceClient client) {
        R<Boolean> exist = client.hasCollection(
                HasCollectionParam.newBuilder()
                        .withCollectionName(FaceArchive.COLLECTION_NAME)
                        .build());
        if (exist.getData()) {
            return;
        }


        FieldType archiveId = FieldType.newBuilder()
                .withName(FaceArchive.Field.ARCHIVE_ID)
                .withDescription("主键id")
                .withDataType(DataType.Int64)
                .withPrimaryKey(true)
                .withAutoID(false)
                .build();
        FieldType orgId = FieldType.newBuilder()
                .withName(FaceArchive.Field.ORG_ID)
                .withDescription("组织id")
                .withDataType(DataType.Int32)
                .build();
        FieldType archiveFeature = FieldType.newBuilder()
                .withName(FaceArchive.Field.ARCHIVE_FEATURE)
                .withDescription("档案特征值")
                .withDataType(DataType.FloatVector)
                .withDimension(FaceArchive.FEATURE_DIM)
                .build();
        CreateCollectionParam createCollectionReq = CreateCollectionParam.newBuilder()
                .withCollectionName(FaceArchive.COLLECTION_NAME)
                .withDescription("档案集合")
                .withShardsNum(FaceArchive.SHARDS_NUM)
                .addFieldType(archiveId)
                .addFieldType(orgId)
                .addFieldType(archiveFeature)
                .build();
        R<RpcStatus> response = client.createCollection(createCollectionReq);
        log.info("创建milvus集合结果{}", response);
        createPartition(client);
    }

    /**
     * 创建分区
     */
    private void createPartition(MilvusServiceClient client) {
        for (int i = 0; i < FaceArchive.PARTITION_NUM; i++) {
            R<RpcStatus> response = client.createPartition(CreatePartitionParam.newBuilder()
                    .withCollectionName(FaceArchive.COLLECTION_NAME) //集合名称
                    .withPartitionName(FaceArchive.PARTITION_PREFIX + i) //分区名称
                    .build());
            log.info("创建milvus分区结果{}", response);
        }
        createIndex(client);
    }


    /**
     * 创建索引
     */
    public R<RpcStatus> createIndex(MilvusServiceClient client) {
        R<RpcStatus> response = client.createIndex(CreateIndexParam.newBuilder()
                .withCollectionName(FaceArchive.COLLECTION_NAME)
                .withFieldName(FaceArchive.Field.ARCHIVE_FEATURE)
                .withIndexType(IndexType.IVF_FLAT)
                .withMetricType(MetricType.IP)
                //nlist 建议值为 4 × sqrt(n)，其中 n 指 segment 最多包含的 entity 条数。
                .withExtraParam("{\"nlist\":16384}")
                .withSyncMode(Boolean.FALSE)
                .build());
        log.info("创建milvus索引结果-------------------->{}", response.toString());
        R<GetIndexBuildProgressResponse> idnexResp = client.getIndexBuildProgress(
                GetIndexBuildProgressParam.newBuilder()
                        .withCollectionName(FaceArchive.COLLECTION_NAME)
                        .build());
        log.info("getIndexBuildProgress---------------------------->{}", idnexResp.toString());
        return response;
    }


}