package com.sky.controller.admin;

import com.sky.mapper.OrderMapper;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.OrderService;
import com.sky.service.ReportService;
import com.sky.vo.OrderReportVO;
import com.sky.vo.SalesTop10ReportVO;
import com.sky.vo.TurnoverReportVO;
import com.sky.vo.UserReportVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.time.LocalDate;

@RestController
@RequestMapping("/admin/report")
@Slf4j
@Api(tags = "数据统计相关接口")
public class ReportController {
    @Autowired
    private ReportService reportService;

    /**
     * 营业额数据统计
     * @param begin
     * @param end
     * @return
     */
    @GetMapping("/turnoverStatistics")
    @ApiOperation("营业额数据统计")
    public Result<TurnoverReportVO> turnoverStatistics(
            @DateTimeFormat(pattern="yyyy-MM-dd") LocalDate begin,
            @DateTimeFormat(pattern="yyyy-MM-dd") LocalDate end
            ){
        log.info("营业额数据统计, begin:{}, end:{}", begin, end);
        TurnoverReportVO turnoverReportVO = reportService.getTurnoverStatistics(begin, end);

        return Result.success(turnoverReportVO);
    }

    /**
     * 用户数据统计
     * @param begin
     * @param end
     * @return
     */
    @GetMapping("/userStatistics")
    @ApiOperation("用户数据统计")
    public Result<UserReportVO> userStatistics(
            @DateTimeFormat(pattern="yyyy-MM-dd") LocalDate begin,
            @DateTimeFormat(pattern="yyyy-MM-dd") LocalDate end
            ){
        log.info("用户数据统计, begin:{}, end:{}", begin, end);
        UserReportVO userReportVO = reportService.getUserStatistics(begin, end);

        return Result.success(userReportVO);
    }

    /**
     * 订单数据统计
     * @param begin
     * @param end
     * @return
     */
    @GetMapping("/ordersStatistics")
    @ApiOperation("订单数据统计")
    public Result<OrderReportVO> ordersStatistics(
            @DateTimeFormat(pattern="yyyy-MM-dd") LocalDate begin,
            @DateTimeFormat(pattern="yyyy-MM-dd") LocalDate end
    ){
        log.info("订单数据统计, begin:{}, end:{}", begin, end);
        OrderReportVO orderReportVO = reportService.getOrdersStatistics(begin, end);
        return Result.success(orderReportVO);
    }

    /**
     * 查询销量排名top10
     * @return
     */
    @GetMapping("/top10")
    @ApiOperation("查询销量排名top10")
    public Result<SalesTop10ReportVO> getSalesTop10(
            @DateTimeFormat(pattern="yyyy-MM-dd") LocalDate begin,
            @DateTimeFormat(pattern="yyyy-MM-dd") LocalDate end
    ) {
        log.info("查询销量排名top10, begin:{}, end:{}", begin, end);
        SalesTop10ReportVO salesTop10ReportVO = reportService.getSalesTop10(begin, end);
        return Result.success(salesTop10ReportVO);
    }

    /**
     * 导出营业额数据
     * @param response
     */
    @GetMapping("/export")
    @ApiOperation("导出营业额数据")
    public void export(HttpServletResponse response){
        log.info("导出数据");
        reportService.exportBussinessData(response);
    }
}
