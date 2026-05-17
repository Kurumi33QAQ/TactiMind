package com.zsj.tactimind.opendata.web;

import com.zsj.tactimind.catalog.model.SourceType;
import com.zsj.tactimind.opendata.model.OpenDataConvertRequest;
import com.zsj.tactimind.opendata.model.OpenDataConvertResult;
import com.zsj.tactimind.opendata.model.OpenDataImportGuide;
import com.zsj.tactimind.opendata.service.StatsBombLocalEventConverter;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;

@CrossOrigin
@RestController
@RequestMapping("/api/open-data")
public class OpenDataImportController {
    private final StatsBombLocalEventConverter statsBombLocalEventConverter;

    public OpenDataImportController(StatsBombLocalEventConverter statsBombLocalEventConverter) {
        this.statsBombLocalEventConverter = statsBombLocalEventConverter;
    }

    @GetMapping("/import-guide")
    public OpenDataImportGuide importGuide(
            @RequestParam(defaultValue = "STATSBOMB_OPEN_DATA") SourceType sourceType
    ) {
        if (sourceType != SourceType.STATSBOMB_OPEN_DATA) {
            return unsupportedGuide(sourceType);
        }
        return statsBombGuide();
    }

    /**
     * 将本地 StatsBomb Open Data 事件文件转换成 TactiMind 统一事件流。
     * 注意：接口不会联网下载数据，真实数据需要先人工放入 data/open-data/statsbomb/raw。
     */
    @PostMapping("/statsbomb/convert-local")
    public OpenDataConvertResult convertLocalStatsBombEvents(
            @RequestBody OpenDataConvertRequest request
    ) throws IOException {
        return statsBombLocalEventConverter.convert(request);
    }

    private OpenDataImportGuide statsBombGuide() {
        return new OpenDataImportGuide(
                SourceType.STATSBOMB_OPEN_DATA.name(),
                "StatsBomb Open Data",
                true,
                true,
                "data/open-data/statsbomb/raw",
                "data/open-data/statsbomb/converted",
                List.of(
                        "人工下载免费的 StatsBomb Open Data 原始 JSON，放入 raw 目录，避免运行时依赖外网。",
                        "调用 POST /api/open-data/statsbomb/convert-local，把本地事件 JSON 转换为 TactiMind MatchEvent。",
                        "转换器会优先保留射门、射正、进球、关键传球、危险推进、防守动作、丢失球权等 Agent 高价值事件。",
                        "转换后的事件流写入 converted 目录，再把比赛目录 eventFilePath 指向转换结果。",
                        "页面和报告必须标注数据来源为公开事件数据，不能和 AI 模拟数据混淆。"
                ),
                List.of(
                        "minute：比赛分钟",
                        "team：球队名称，后续通过目录映射为中文展示",
                        "type：统一事件类型，例如 SHOT、SHOT_ON_TARGET、GOAL、KEY_PASS、TACKLE、TURNOVER",
                        "player：事件主要球员",
                        "description：中文事件描述",
                        "data.sourceType / data.originalType：数据来源和原始事件类型",
                        "data.zone / data.direction / data.phase / data.result：Agent 可解释分析字段"
                ),
                List.of(
                        "当前转换器只处理本地文件，不会联网下载真实数据。",
                        "真实公开数据接入后，仍然要经过 VerifyAgent 证据校验。",
                        "如果原始数据缺少阵容、球员状态或能力标签，Agent 必须降低置信度或提示数据不足。"
                )
        );
    }

    private OpenDataImportGuide unsupportedGuide(SourceType sourceType) {
        return new OpenDataImportGuide(
                sourceType.name(),
                sourceType.name(),
                true,
                false,
                "data/open-data/unsupported/raw",
                "data/open-data/unsupported/converted",
                List.of("该数据源暂未实现导入流程，后续可按 MatchEventDataSource 接口扩展。"),
                List.of("统一转换目标仍为 TactiMind MatchEvent。"),
                List.of("暂不支持该数据源的自动导入。")
        );
    }
}