package com.luoying.controller;

import cn.hutool.core.io.FileUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.google.gson.Gson;
import com.luoying.annotation.AuthCheck;
import com.luoying.bizmq.BIMessageProducer;
import com.luoying.common.BaseResponse;
import com.luoying.common.DeleteRequest;
import com.luoying.common.ErrorCode;
import com.luoying.common.ResultUtils;
import com.luoying.constant.UserConstant;
import com.luoying.exception.BusinessException;
import com.luoying.exception.ThrowUtils;
import com.luoying.manager.AIManager;
import com.luoying.manager.RedisLimiterManager;
import com.luoying.model.dto.chart.*;
import com.luoying.model.entity.Chart;
import com.luoying.model.entity.User;
import com.luoying.model.vo.BIResponse;
import com.luoying.service.ChartService;
import com.luoying.service.UserService;
import com.luoying.utils.ExcelUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 帖子接口
 */
@RestController
@RequestMapping("/chart")
@Slf4j
public class ChartController {

    @Resource
    private ChartService chartService;

    @Resource
    private UserService userService;

    @Resource
    private AIManager aiManager;

    @Resource
    private RedisLimiterManager redisLimiterManager;

    @Resource
    private ThreadPoolExecutor threadPoolExecutor;

    @Resource
    private BIMessageProducer biMessageProducer;

    private final static Gson GSON = new Gson();

    // region 增删改查

    /**
     * 创建
     *
     * @param chartAddRequest
     * @param request
     * @return
     */
    @PostMapping("/add")
    public BaseResponse<Long> addChart(@RequestBody ChartAddRequest chartAddRequest, HttpServletRequest request) {
        if (chartAddRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Chart chart = new Chart();
        BeanUtils.copyProperties(chartAddRequest, chart);
        User loginUser = userService.getLoginUser(request);
        chart.setUserId(loginUser.getId());
        boolean result = chartService.save(chart);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        long newChartId = chart.getId();
        return ResultUtils.success(newChartId);
    }

    /**
     * 删除
     *
     * @param deleteRequest
     * @param request
     * @return
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteChart(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = userService.getLoginUser(request);
        long id = deleteRequest.getId();
        // 判断是否存在
        Chart oldChart = chartService.getById(id);
        ThrowUtils.throwIf(oldChart == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人或管理员可删除
        if (!oldChart.getUserId().equals(user.getId()) && !userService.isAdmin(request)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        boolean b = chartService.removeById(id);
        return ResultUtils.success(b);
    }

    /**
     * 更新（仅管理员）
     *
     * @param chartUpdateRequest
     * @return
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateChart(@RequestBody ChartUpdateRequest chartUpdateRequest) {
        if (chartUpdateRequest == null || chartUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Chart chart = new Chart();
        BeanUtils.copyProperties(chartUpdateRequest, chart);
        long id = chartUpdateRequest.getId();
        // 判断是否存在
        Chart oldChart = chartService.getById(id);
        ThrowUtils.throwIf(oldChart == null, ErrorCode.NOT_FOUND_ERROR);
        boolean result = chartService.updateById(chart);
        return ResultUtils.success(result);
    }

    /**
     * 根据 id 获取
     *
     * @param id
     * @return
     */
    @GetMapping("/get")
    public BaseResponse<Chart> getChartVOById(long id, HttpServletRequest request) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Chart chart = chartService.getById(id);
        if (chart == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        return ResultUtils.success(chart);
    }

    /**
     * 分页获取列表（封装类）
     *
     * @param chartQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/list/page")
    public BaseResponse<Page<Chart>> listChartVOByPage(@RequestBody ChartQueryRequest chartQueryRequest,
                                                       HttpServletRequest request) {
        long current = chartQueryRequest.getCurrent();
        long size = chartQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        Page<Chart> chartPage = chartService.page(new Page<>(current, size),
                chartService.getQueryWrapper(chartQueryRequest));
        return ResultUtils.success(chartPage);
    }

    /**
     * 分页获取当前用户创建的资源列表
     *
     * @param chartQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/my/list/page")
    public BaseResponse<Page<Chart>> listMyChartVOByPage(@RequestBody ChartQueryRequest chartQueryRequest,
                                                         HttpServletRequest request) {
        if (chartQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        chartQueryRequest.setUserId(loginUser.getId());
        long current = chartQueryRequest.getCurrent();
        long size = chartQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        Page<Chart> chartPage = chartService.page(new Page<>(current, size),
                chartService.getQueryWrapper(chartQueryRequest));
        return ResultUtils.success(chartPage);
    }

    // endregion

    /**
     * 编辑（用户）
     *
     * @param chartEditRequest
     * @param request
     * @return
     */
    @PostMapping("/edit")
    public BaseResponse<Boolean> editChart(@RequestBody ChartEditRequest chartEditRequest, HttpServletRequest request) {
        if (chartEditRequest == null || chartEditRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Chart chart = new Chart();
        BeanUtils.copyProperties(chartEditRequest, chart);
        User loginUser = userService.getLoginUser(request);
        long id = chartEditRequest.getId();
        // 判断是否存在
        Chart oldChart = chartService.getById(id);
        ThrowUtils.throwIf(oldChart == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人或管理员可编辑
        if (!oldChart.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        boolean result = chartService.updateById(chart);
        return ResultUtils.success(result);
    }
    /**
     * 智能分析（同步）
     *
     * @param multipartFile
     * @param genChartRequest
     * @param request
     * @return
     */
    /*@PostMapping("/gen")
    public BaseResponse<BIResponse> genChartByAI(@RequestPart("file") MultipartFile multipartFile,
                                                 GenChartRequest genChartRequest, HttpServletRequest request) {
        String goal = genChartRequest.getGoal();
        String chartType = genChartRequest.getChartType();
        String chartName = genChartRequest.getChartName();
        // 拼接分析目标
        if (StringUtils.isNotBlank(chartType)) {
            goal = goal + "，请使用" + chartType;
        }
        // 校验参数
        ThrowUtils.throwIf(StringUtils.isBlank(goal), ErrorCode.PARAMS_ERROR, "分析目标为空");
        ThrowUtils.throwIf(StringUtils.isNotBlank(chartName) && chartName.length() > 128, ErrorCode.PARAMS_ERROR, "图表名称过长");
        // 校验文件
        long size = multipartFile.getSize();
        String originalFilename = multipartFile.getOriginalFilename();
        // 校验文件大小
        final long ONE_MB = 1024 * 1024L;
        ThrowUtils.throwIf(size > ONE_MB, ErrorCode.PARAMS_ERROR, "文件大小超过 1M");
        // 校验文件后缀
        String suffix = FileUtil.getSuffix(originalFilename);
        final List<String> validFileSuffix = Arrays.asList("xlsx");
        ThrowUtils.throwIf(!validFileSuffix.contains(suffix), ErrorCode.PARAMS_ERROR, "文件后缀不符合要求");
        // 获取登录用户
        User loginUser = userService.getLoginUser(request);
        // 限流,每个用户一个限流器
        redisLimiterManager.doRateLimit("genChartByAI_"+loginUser.getId().toString());
        // 压缩原始数据
        String data = ExcelUtils.excelToCsv(multipartFile);
        // 构造用户请求（分析目标，图表名称，图表类型，csv数据）
        StringBuilder userInput = new StringBuilder();
        userInput.append("分析需求：").append("\n");
        userInput.append(goal).append("\n");
        userInput.append("原始数据：").append("\n");
        userInput.append(data).append("\n");
        // 调用鱼聪明SDK，得到响应
        long biModelId = 1719916921023344642L;
        String answer = aiManager.doChat(biModelId, userInput.toString());
        // 从AI响应结果中，取出需要的数据
        String[] splits = answer.split("【【【【【");
        if (splits.length < 3) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "AI生成错误");
        }
        String genChart = splits[1].trim();
        String genSummary = splits[2].trim();
        // 插入数据库
        Chart chart = new Chart();
        chart.setGoal(goal);
        chart.setRawData(data);
        chart.setChartType(chartType);
        chart.setChartName(chartName);
        chart.setGenChart(genChart);
        chart.setGenSummary(genSummary);
        chart.setUserId(loginUser.getId());
        boolean save = chartService.save(chart);
        ThrowUtils.throwIf(!save, ErrorCode.SYSTEM_ERROR, "图表保存失败");
        // 返回给前端
        BIResponse biResponse = new BIResponse();
        biResponse.setGenChart(genChart);
        biResponse.setGenSummary(genSummary);
        biResponse.setChartId(chart.getId());
        return ResultUtils.success(biResponse);
    }*/


    /**
     * 智能分析（异步）
     *
     * @param multipartFile
     * @param genChartRequest
     * @param request
     * @return
     */
    /*@PostMapping("/gen")
    public BaseResponse<BIResponse> genChartByAI(@RequestPart("file") MultipartFile multipartFile,
                                                 GenChartRequest genChartRequest, HttpServletRequest request) {
        String goal = genChartRequest.getGoal();
        String chartType = genChartRequest.getChartType();
        String chartName = genChartRequest.getChartName();
        // 拼接分析目标
        if (StringUtils.isNotBlank(chartType)) {
            goal = goal + "，请使用" + chartType;
        }
        // 校验参数
        ThrowUtils.throwIf(StringUtils.isBlank(goal), ErrorCode.PARAMS_ERROR, "分析目标为空");
        ThrowUtils.throwIf(StringUtils.isNotBlank(chartName) && chartName.length() > 128, ErrorCode.PARAMS_ERROR, "图表名称过长");
        // 校验文件
        long size = multipartFile.getSize();
        String originalFilename = multipartFile.getOriginalFilename();
        // 校验文件大小
        final long ONE_MB = 1024 * 1024L;
        ThrowUtils.throwIf(size > ONE_MB, ErrorCode.PARAMS_ERROR, "文件大小超过 1M");
        // 校验文件后缀
        String suffix = FileUtil.getSuffix(originalFilename);
        final List<String> validFileSuffix = Arrays.asList("xlsx");
        ThrowUtils.throwIf(!validFileSuffix.contains(suffix), ErrorCode.PARAMS_ERROR, "文件后缀不符合要求");
        // 获取登录用户
        User loginUser = userService.getLoginUser(request);
        // 限流,每个用户一个限流器
        redisLimiterManager.doRateLimit("genChartByAI_" + loginUser.getId().toString());
        // 压缩原始数据
        String data = ExcelUtils.excelToCsv(multipartFile);
        // 构造用户请求（分析目标，图表名称，图表类型，csv数据）
        StringBuilder userInput = new StringBuilder();
        userInput.append("分析需求：").append("\n");
        userInput.append(goal).append("\n");
        userInput.append("原始数据：").append("\n");
        userInput.append(data).append("\n");
        // 插入数据库
        Chart chart = new Chart();
        chart.setGoal(goal);
        chart.setRawData(data);
        chart.setChartType(chartType);
        chart.setChartName(chartName);
        chart.setUserId(loginUser.getId());
        chart.setStatus("wait");
        boolean save = chartService.save(chart);
        ThrowUtils.throwIf(!save, ErrorCode.SYSTEM_ERROR, "图表保存失败");

        //todo 处理任务队列满了后，抛异常的情况
        CompletableFuture.runAsync(() -> {
            //先修改图表的任务状态为“执行中”；等执行成功后，修改为“已完成”，保存执行结果；执行失败后，修改为失败，记录失败信息
            //修改任务状态为执行中，减少重复执行的风险、同时让用户知晓执行状态
            Chart updateChart = new Chart();
            updateChart.setId(chart.getId());
            updateChart.setStatus("running");
            boolean result = chartService.updateById(updateChart);
            if (!result) {
                handleChartUpdateError(chart.getId(),"更新图表执行中状态失败");
                return;
            }
            // 调用鱼聪明SDK，得到响应
            long biModelId = 1719916921023344642L;
            String answer = aiManager.doChat(biModelId, userInput.toString());
            // 从AI响应结果中，取出需要的数据
            String[] splits = answer.split("【【【【【");
            if (splits.length < 3) {
                handleChartUpdateError(chart.getId(),"AI生成错误");
                return;
            }
            String genChart = splits[1].trim();
            String genSummary = splits[2].trim();
            //执行成功
            Chart updateChartResult = new Chart();
            updateChartResult.setId(chart.getId());
            //todo 建议定义状态为枚举值
            updateChartResult.setStatus("succeeded");
            updateChartResult.setGenChart(genChart);
            updateChartResult.setGenSummary(genSummary);
            result = chartService.updateById(updateChartResult);
            if (!result) {
                handleChartUpdateError(chart.getId(),"更新图表已完成状态失败");
                return;
            }
        },threadPoolExecutor);
        // 返回给前端
        BIResponse biResponse = new BIResponse();
        biResponse.setChartId(chart.getId());
        return ResultUtils.success(biResponse);
    }*/

    /**
     * 智能分析（异步&消息队列）
     *
     * @param multipartFile
     * @param genChartRequest
     * @param request
     * @return
     */
    @PostMapping("/gen")
    public BaseResponse<BIResponse> genChartByAI(@RequestPart("file") MultipartFile multipartFile,
                                                 GenChartRequest genChartRequest, HttpServletRequest request) {
        String goal = genChartRequest.getGoal();
        String chartType = genChartRequest.getChartType();
        String chartName = genChartRequest.getChartName();
        // 拼接分析目标
        if (StringUtils.isNotBlank(chartType)) {
            goal = goal + "，请使用" + chartType;
        }
        // 校验参数
        ThrowUtils.throwIf(StringUtils.isBlank(goal), ErrorCode.PARAMS_ERROR, "分析目标为空");
        ThrowUtils.throwIf(StringUtils.isNotBlank(chartName) && chartName.length() > 128, ErrorCode.PARAMS_ERROR, "图表名称过长");
        // 校验文件
        long size = multipartFile.getSize();
        String originalFilename = multipartFile.getOriginalFilename();
        // 校验文件大小
        final long ONE_MB = 1024 * 1024L;
        ThrowUtils.throwIf(size > ONE_MB, ErrorCode.PARAMS_ERROR, "文件大小超过 1M");
        // 校验文件后缀
        String suffix = FileUtil.getSuffix(originalFilename);
        final List<String> validFileSuffix = Arrays.asList("xlsx");
        ThrowUtils.throwIf(!validFileSuffix.contains(suffix), ErrorCode.PARAMS_ERROR, "文件后缀不符合要求");
        // 获取登录用户
        User loginUser = userService.getLoginUser(request);
        // 限流,每个用户一个限流器
        redisLimiterManager.doRateLimit("genChartByAI_" + loginUser.getId().toString());
        // 压缩原始数据
        String data = ExcelUtils.excelToCsv(multipartFile);
        // 插入数据库
        Chart chart = new Chart();
        chart.setGoal(goal);
        chart.setRawData(data);
        chart.setChartType(chartType);
        chart.setChartName(chartName);
        chart.setUserId(loginUser.getId());
        chart.setStatus("wait");
        boolean save = chartService.save(chart);
        ThrowUtils.throwIf(!save, ErrorCode.SYSTEM_ERROR, "图表保存失败");

        // todo 处理任务队列满了后，抛异常的情况
        // 发送消息
        biMessageProducer.sendMessage(String.valueOf(chart.getId()));
        // 返回给前端
        BIResponse biResponse = new BIResponse();
        biResponse.setChartId(chart.getId());
        return ResultUtils.success(biResponse);
    }

    private void handleChartUpdateError(long chartId, String execMessage) {
        Chart updateChart = new Chart();
        updateChart.setId(chartId);
        updateChart.setStatus("failed");
        updateChart.setExecMessage(execMessage);
        boolean result = chartService.updateById(updateChart);
        if (!result) {
            log.error("更新图表失败状态失败，" + chartId + "，" + execMessage);
        }
    }
}
