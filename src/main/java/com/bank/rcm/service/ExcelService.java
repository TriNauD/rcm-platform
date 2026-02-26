package com.bank.rcm.service;

import java.io.InputStream;
import java.util.function.Consumer;

import org.springframework.stereotype.Service;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.bank.rcm.constant.AppConstants;

import lombok.extern.slf4j.Slf4j;

/**
 * Excel文件处理服务
 * <p>负责流式读取Excel文件，支持大文件低内存处理</p>
 */
@Service
@Slf4j
public class ExcelService {

    /**
     * 流式读取Excel文件
     * <p>采用EasyExcel的流式读取方式，逐行处理数据，降低内存占用。支持泛型，可处理任意DTO类型，
     * 提高代码可扩展性和复用性。</p>
     *
     * @param <T> 泛型参数，表示Excel行数据对应的DTO类型
     * @param inputStream 待读取的Excel文件输入流
     * @param clazz 数据映射的DTO类对象，用于将Excel行数据映射为对应的Java对象
     * @param rowProcessor 行数据处理器，每读取一行数据时调用该处理函数进行业务处理
     */
    public <T> void readExcelInStream(InputStream inputStream, Class<T> clazz, Consumer<T> rowProcessor) {
        EasyExcel.read(inputStream, clazz, new AnalysisEventListener<T>() {
            @Override
            public void invoke(T data, AnalysisContext context) {
                // 调用传入的处理逻辑
                rowProcessor.accept(data);
            }

            @Override
            public void doAfterAllAnalysed(AnalysisContext context) {
                log.info("{} Excel 文件读取完成", AppConstants.LOG_EXCEL_PREFIX);
            }
        }).sheet().doRead();
    }

}
