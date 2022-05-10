package com.vishee.arcface.controller;

import com.alibaba.fastjson.JSON;
import com.arcsoft.face.*;

import com.arcsoft.face.enums.CompareModel;
import com.arcsoft.face.enums.ExtractType;
import com.arcsoft.face.toolkit.ImageFactory;
import com.arcsoft.face.toolkit.ImageInfo;
import com.github.pagehelper.PageHelper;
import com.google.common.collect.Lists;
import com.vishee.arcface.config.FaceArchive;
import com.vishee.arcface.mapper.FaceMapper;
import com.vishee.arcface.config.ArcFaceEngineRunner;
import com.vishee.arcface.service.RegisterArcThread;
import com.vishee.arcface.util.MilvusUtil;
import io.milvus.client.MilvusServiceClient;
import io.milvus.grpc.DataType;
import io.milvus.grpc.GetCollectionStatisticsResponse;
import io.milvus.grpc.MutationResult;
import io.milvus.grpc.SearchResults;
import io.milvus.param.MetricType;
import io.milvus.param.R;
import io.milvus.param.RpcStatus;
import io.milvus.param.collection.GetCollectionStatisticsParam;
import io.milvus.param.collection.LoadCollectionParam;
import io.milvus.param.collection.ReleaseCollectionParam;
import io.milvus.param.dml.InsertParam;
import io.milvus.param.dml.InsertParam.Field;
import io.milvus.param.dml.SearchParam;
import io.milvus.param.partition.LoadPartitionsParam;
import io.milvus.response.GetCollStatResponseWrapper;
import io.milvus.response.SearchResultsWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author dingchao
 * @date 2022-03-25/15:50
 * @description
 */
@Controller
@Slf4j
public class FaceController {

    @Autowired
    ArcFaceEngineRunner faceEngineService;

    @Autowired
    FaceMapper faceMapper;

    @Autowired
    ExecutorService crawlExecutorPool;

    @Autowired
    FaceEngine defaultEngine;

    AtomicInteger count = new AtomicInteger(0);

    @Autowired
    MilvusServiceClient client;

    @RequestMapping("/index")
    public String loadPage() {
        return "index";
    }

    @RequestMapping("/add")
    public String add() {
        return "add";
    }

//    @ResponseBody
//    @RequestMapping("/initLocalData")
//    public String test() {
//        String prefix = "D:\\pic\\person";
//        File file = new File(prefix);
//        String[] s = file.list();
//        List<String> fileList = new ArrayList<>();
//        log.info("所有图片书");
//        for (int i = 0; i < Objects.requireNonNull(s).length; i++) {
//            fileList.add(prefix + File.separator + s[i]);
//
//            if (fileList.size() == 100) {
//                log.info("创建线程{}", count);
//                count.getAndDecrement();
//                RegisterArcThread thread = new RegisterArcThread(fileList, faceEngineService, faceMapper);
//
//                crawlExecutorPool.execute(thread);
//                fileList = new ArrayList<>();
//            }
//            if (i == Objects.requireNonNull(s).length - 1) {
//                log.info("创建线程2{}", count);
//                count.getAndDecrement();
//                RegisterArcThread thread = new RegisterArcThread(fileList, faceEngineService, faceMapper);
//                crawlExecutorPool.execute(thread);
//                fileList = new ArrayList<>();
//            }
//
//        }
//
//        return "init data success";
//    }


    @ResponseBody
    @RequestMapping("/initMilvusData")
    public String initData() {
        Integer faceCount = faceMapper.count();
        double pageSize = Math.ceil((double) faceCount / 1000);
        System.out.println("分页大小" + pageSize);
        for (int i = 0; i <= pageSize; i++) {
            PageHelper.startPage(i, 1000);
            List<Map<String, Object>> mapList = faceMapper.list();
            RegisterArcThread thread = new RegisterArcThread(mapList, client);
            crawlExecutorPool.execute(thread);

        }

        return "init data success";
    }


    @ResponseBody
    @RequestMapping("/postpic")
    public String loadPage(@RequestParam(value = "file") MultipartFile file) throws IOException {

        Instant inst1 = Instant.now();
        int errorCode;

        List<FaceInfo> faceInfoList = new ArrayList<FaceInfo>();
        ImageInfo imageInfo = ImageFactory.getRGBData(file.getBytes());
        errorCode = defaultEngine.detectFaces(imageInfo, faceInfoList);
        System.out.println("人脸检测errorCode:" + errorCode);
        System.out.println("检测到人脸数:" + faceInfoList.size());
        Instant inst3 = Instant.now();
        System.err.println("查询人脸数据时间3" + Duration.between(inst1, inst3));
        if (!faceInfoList.isEmpty()) {
            FaceInfo faceInfo = faceInfoList.get(0);
            SearchResult searchResult = new SearchResult();
            FaceFeature faceFeature = new FaceFeature();
            errorCode = defaultEngine.extractFaceFeature(imageInfo, faceInfo, ExtractType.REGISTER, 0, faceFeature);
            Instant inst4 = Instant.now();
            System.err.println("查询人脸数据时间4" + Duration.between(inst1, inst4));
            String result = search(client, faceFeature.getFeatureData());
            Instant inst2 = Instant.now();
            System.err.println("查询人脸数据时间" + Duration.between(inst1, inst2));
            return result;
            // errorCode = defaultEngine.searchFaceFeature(faceFeature, CompareModel.LIFE_PHOTO, searchResult);

        }
        return "未找到相似人脸数据";
    }


    @ResponseBody
    @RequestMapping("/postpicadd")
    public String postadd(@RequestParam(value = "file") MultipartFile file) throws IOException {

        Instant inst1 = Instant.now();
        int errorCode;

        List<FaceInfo> faceInfoList = new ArrayList<FaceInfo>();
        ImageInfo imageInfo = ImageFactory.getRGBData(file.getBytes());
        errorCode = defaultEngine.detectFaces(imageInfo, faceInfoList);
        System.out.println("人脸检测errorCode:" + errorCode);
        System.out.println("检测到人脸数:" + faceInfoList.size());
        Instant inst3 = Instant.now();
        System.err.println("查询人脸数据时间3" + Duration.between(inst1, inst3));
        if (!faceInfoList.isEmpty()) {
            FaceInfo faceInfo = faceInfoList.get(0);
            SearchResult searchResult = new SearchResult();
            FaceFeature faceFeature = new FaceFeature();
            errorCode = defaultEngine.extractFaceFeature(imageInfo, faceInfo, ExtractType.REGISTER, 0, faceFeature);
            List<InsertParam.Field> fields = new ArrayList<>();
            List<Long> archiveIds = Lists.newArrayList();
            List<Integer> ids = Lists.newArrayList();
            List<List<Float>> floatVectors = Lists.newArrayList();
            archiveIds.add(110101000111L);
            ids.add(1008686);
            floatVectors.add(MilvusUtil.arcsoftToFloat(faceFeature.getFeatureData()));
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
            R<MutationResult> insert = client.insert(insertParam);


            log.info("| server| insert to milvus success {}", insert);

        } return "未找到相似人脸数据";
    }


    private String search(MilvusServiceClient client, byte[] da) {
        R<RpcStatus> response = client.loadCollection(LoadCollectionParam.newBuilder()
            //集合名称
            .withCollectionName(FaceArchive.COLLECTION_NAME).build());
        log.info("loadCollection------------->{}", response);

        List<Float> arcsoftToFloat = MilvusUtil.arcsoftToFloat(da);
        List<List<Float>> list = new ArrayList<>();
        list.add(arcsoftToFloat);
        SearchParam.Builder builder = SearchParam.newBuilder()
            //集合名称
            .withCollectionName(FaceArchive.COLLECTION_NAME)
            //计算方式
            // 欧氏距离 (L2)
            // 内积 (IP)
            .withMetricType(MetricType.IP)
            //返回多少条结果
            .withTopK(1)
            //搜索的向量值
            .withVectors(list)
            //搜索的Field
            .withVectorFieldName(FaceArchive.Field.ARCHIVE_FEATURE).withOutFields(Lists.newArrayList(FaceArchive.Field.ARCHIVE_ID, FaceArchive.Field.ORG_ID))
            //https://milvus.io/cn/docs/v2.0.0/performance_faq.md
            .withParams("{\"nprobe\":512}");

        builder

            .withPartitionNames(Lists.newArrayList(FaceArchive.PARTITION_PREFIX + 1));

        R<SearchResults> search = client.search(builder.build());
        System.out.println(1111);
        if (search.getData() == null) {

            log.info("什么都没搜到");

        }
        SearchResultsWrapper wrapper = new SearchResultsWrapper(search.getData().getResults());
        log.info("搜到结果{}", search.toString());
        for (int i = 0; i < list.size(); ++i) {

            List<SearchResultsWrapper.IDScore> scores = wrapper.getIDScore(i);
            if (scores.size() > 0) {

                SearchResultsWrapper.IDScore idScore = scores.get(0);
                log.info("搜到如下结果{},{}", idScore.getLongID(), idScore.toString());

            }
        }

//        R<RpcStatus> response3 = client.releaseCollection(ReleaseCollectionParam.newBuilder().withCollectionName(FaceArchive.COLLECTION_NAME).build());
//        log.info("releaseCollection------------->{}", response3);
        return search.toString();

//        R<RpcStatus> response2 = client.loadPartitions(
//                LoadPartitionsParam
//                        .newBuilder()
//                        //集合名称
//                        .withCollectionName(FaceArchive.COLLECTION_NAME)
//                        //需要加载的分区名称
//                        .withPartitionNames(Lists.newArrayList(FaceArchive.PARTITION_PREFIX + 1))
//                        .build()
//        );
//        log.info("loadCollection2------------->{}", response2);

    }

    @ResponseBody
    @RequestMapping("collection")
    public Object getCollection() {
        R<GetCollectionStatisticsResponse> respCollectionStatistics = client.getCollectionStatistics(   // Return the statistics information of the collection.
            GetCollectionStatisticsParam.newBuilder().withCollectionName(FaceArchive.COLLECTION_NAME).build());
        GetCollStatResponseWrapper wrapperCollectionStatistics = new GetCollStatResponseWrapper(respCollectionStatistics.getData());
        return wrapperCollectionStatistics.getRowCount();
    }


}
