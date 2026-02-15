package com.bank.rcm;

import com.alibaba.excel.EasyExcel;
import com.bank.rcm.dto.StepBDto;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

// 用于生成测试数据的小工具
public class DataGenerator {

    private static void generateHappyCaseTestData() {
        // 0. 设置数据量
        int generateDataAmount = 100;

        // 1. 动态获取项目路径，确保生成到 templates 文件夹
        String projectPath = System.getProperty("user.dir");
        String filePath = projectPath + File.separator + "src" + File.separator + "main" +
                File.separator + "resources" + File.separator + "templates" +
                File.separator + "happy_case_" + generateDataAmount + ".xlsx";
        // 确保目录存在
        File file = new File(filePath);
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }

        System.out.println("OB 指令：开始生产 " + generateDataAmount + " 行 Happy Case 数据...");

        // 2. 构造数据
        List<StepBDto> list = new ArrayList<>();
        for (int i = 1; i <= generateDataAmount; i++) {
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

    public static void main(String[] args) {
        generateHappyCaseTestData();
    }
    
}