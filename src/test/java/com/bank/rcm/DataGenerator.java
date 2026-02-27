package com.bank.rcm;

import com.alibaba.excel.EasyExcel;
import com.bank.rcm.dto.StepBDto;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

// 用于生成测试数据的小工具
public class DataGenerator {

    private static void generateHappyCaseTestData(int totalRows) {
        // 1. 动态获取项目路径，确保生成到 templates 文件夹
        String projectPath = System.getProperty("user.dir");
        String filePath = projectPath + File.separator + "src" + File.separator + "main" +
                File.separator + "resources" + File.separator + "templates" +
                File.separator + "happy_case_" + totalRows + ".xlsx";
        // 确保目录存在
        File file = new File(filePath);
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }

        System.out.println("OB 指令：开始生产 " + totalRows + " 行 Happy Case 数据...");

        // 2. 构造数据
        List<StepBDto> list = new ArrayList<>();
        for (int i = 1; i <= totalRows; i++) {
            StepBDto data = new StepBDto();
            data.setObligationId("OB-ID-" + String.format("%04d", i));
            data.setCesId("CES-ID-" + String.format("%04d", i));
            data.setCesStatement("Standard Requirement Statement for CES " + i);
            // 简单的 Happy Case 拆分格式
            data.setCeamIds("CEAM-" + i + "-1 | CEAM-" + i + "-2");
            list.add(data);
        }

        // 3. 写入文件
        EasyExcel.write(filePath, StepBDto.class)
                .sheet("Sheet1")
                .doWrite(list);

        System.out.println("OB 指令：任务完成！文件已存至: " + filePath);
    }

    // 可生成重复数据
    private static void generateStressTestData(int totalRows, int uniqueObCount) {
        // 1. 动态获取项目路径，确保生成到 templates 文件夹
        String projectPath = System.getProperty("user.dir");
        String filePath = projectPath + File.separator + "src" + File.separator + "main" +
                File.separator + "resources" + File.separator + "templates" +
                File.separator + "happy_case_" + totalRows + "_unique_" + uniqueObCount + ".xlsx";
        // 确保目录存在
        File file = new File(filePath);
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }

        List<StepBDto> list = new ArrayList<>();
        for (int i = 1; i <= totalRows; i++) {
            StepBDto data = new StepBDto();

            // 【核心修改点】
            // 通过 i % uniqueObCount，让 ObligationId 在一定范围内循环
            // 例如 totalRows=4000, uniqueObCount=2000，则每个 ID 会出现两次
            int obSuffix = (i % uniqueObCount) + 1;
            data.setObligationId("OB-ID-" + String.format("%04d", obSuffix));

            // CES ID 必须保持唯一，否则会变成单纯的更新操作，失去测试意义
            data.setCesId("CES-ID-" + String.format("%04d", i));
            data.setCesStatement("Statement for row " + i);
            data.setCeamIds("CEAM-" + i + "-1");

            list.add(data);
        }

        EasyExcel.write(filePath, StepBDto.class).sheet("Sheet1").doWrite(list);
        System.out.println("数据生产完成。总行数：" + totalRows + "，唯一 OB 数：" + uniqueObCount);
    }

    public static void main(String[] args) {
        // generateHappyCaseTestData(300);
        generateStressTestData(1000000, 1000000);
    }

}