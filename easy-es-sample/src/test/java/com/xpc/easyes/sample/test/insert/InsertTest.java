package com.xpc.easyes.sample.test.insert;

import com.xpc.easyes.sample.entity.Document;
import com.xpc.easyes.sample.mapper.DocumentMapper;
import com.xpc.easyes.sample.test.TestEasyEsApplication;
import org.elasticsearch.geometry.Point;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * 插入测试
 * <p>
 * Copyright © 2021 xpc1024 All Rights Reserved
 **/
@RunWith(SpringRunner.class)
@SpringBootTest(classes = TestEasyEsApplication.class)
public class InsertTest {
    @Resource
    private DocumentMapper documentMapper;

    @Test
    public void testInsert() {
        // 测试插入数据
        Document document = new Document();
        document.setTitle("老汉");
        document.setContent("推*技术过硬");
        document.setLocation("40.171975,116.587105");
        Point point = new Point(13.400544, 52.530286);
        document.setGeoLocation(point.toString());
        int successCount = documentMapper.insert(document);
        Assert.assertEquals(successCount, 1);
    }

    @Test
    public void testInsertBatch() {
        List<Document> documentList = new ArrayList<>();
        Document document = new Document();
        document.setTitle("老王");
        document.setContent("推*技术过硬");
        document.setCreator("老王");
        document.setLocation("40.17836693398477,116.64002551005981");

        Document document1 = new Document();
        document1.setTitle("老李");
        document1.setContent("推*技术过硬");
        document1.setCreator("老汉");
        document1.setLocation("40.19103839805197,116.5624013764374");


        Document document2 = new Document();
        document2.setTitle("老汉");
        document2.setContent("推*技术过硬");
        document2.setCreator("老汉");
        document2.setLocation("40.13933715136454,116.63441990026217");

        documentList.add(document);
        documentList.add(document1);
        documentList.add(document2);

        int successCount = documentMapper.insertBatch(documentList);
        System.out.println(successCount);
        // id可以直接从被插入的数据中取出
        documentList.forEach(System.out::println);
    }
}
