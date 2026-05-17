package com.zsj.tactimind.opendata.model;

/**
 * 本地公开数据转换请求。
 * 第一版只处理已经下载到 raw 目录的 StatsBomb 事件 JSON，不在后端里联网抓取数据。
 */
public record OpenDataConvertRequest(
        String matchCode,
        String rawEventFile,
        String outputFile
) {
}