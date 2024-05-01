package com.sky.service.impl;

import com.sky.dto.GoodsSalesDTO;
import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.UserMapper;
import com.sky.result.Result;
import com.sky.service.ReportService;
import com.sky.service.WorkspaceService;
import com.sky.vo.*;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.lang.StringUtils;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ReportServiceImpl implements ReportService {
    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private WorkspaceService workspaceService;


    /**
     * 营业额数据统计
     *
     * @param begin
     * @param end
     * @return
     */
    @Override
    public TurnoverReportVO getTurnoverStatistics(LocalDate begin, LocalDate end) {
        //构建dataList
        List<LocalDate> dateList = new ArrayList<>();
        dateList.add(begin);
        while (!begin.equals(end)) {
            begin = begin.plusDays(1);
            dateList.add(begin);
        }


        //构建turnoverList
        List<Double> turnoverList = new ArrayList<>();
        for (LocalDate date : dateList) {
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN); //00:00:00
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX); //23:59:59
            Map map = new HashMap<>();
            map.put("begin", beginTime);
            map.put("end", endTime);
            map.put("status", Orders.COMPLETED);
            Double turnover = orderMapper.sumByMap(map);
            turnover = turnover == null ? 0.0 : turnover;
            turnoverList.add(turnover);
        }


        return TurnoverReportVO
                .builder()
                .dateList(StringUtils.join(dateList, ","))
                .turnoverList(StringUtils.join(turnoverList, ","))
                .build();
    }

    /**
     * 用户数据统计
     * @param begin
     * @param end
     * @return
     */
    @Override
    public UserReportVO getUserStatistics(LocalDate begin, LocalDate end) {
        //构建dataList
        List<LocalDate> dateList = new ArrayList<>();
        dateList.add(begin);
        while (!begin.equals(end)) {
            begin = begin.plusDays(1);
            dateList.add(begin);
        }


        //构建totalUserList和newUserList
        List<Integer> totalUserList = new ArrayList<>();
        List<Integer> newUserList = new ArrayList<>();
        for (LocalDate date : dateList) {
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN); //00:00:00
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX); //23:59:59
            Map map = new HashMap<>();

            //构建totalUserList
            map.put("end", endTime);
            Integer totalUser = userMapper.countByMap(map);
            totalUser = totalUser == null ? 0 : totalUser;
            totalUserList.add(totalUser);

            //构建newUserList
            map.put("begin", beginTime);
            Integer newUser = userMapper.countByMap(map);
            newUser = newUser == null ? 0 : newUser;
            newUserList.add(newUser);
        }


        return UserReportVO
                .builder()
                .dateList(StringUtils.join(dateList, ","))
                .totalUserList(StringUtils.join(totalUserList, ","))
                .newUserList(StringUtils.join(newUserList, ","))
                .build();
    }

    /**
     * 订单数据统计
     * @param begin
     * @param end
     * @return
     */
    @Override
    public OrderReportVO getOrdersStatistics(LocalDate begin, LocalDate end) {

        //构建dataList
        List<LocalDate> dateList = new ArrayList<>();
        dateList.add(begin);
        while (!begin.equals(end)) {
            begin = begin.plusDays(1);
            dateList.add(begin);
        }

        //构建orderCountList和validOrderCountList
        List<Integer> orderCountList = new ArrayList<>();
        List<Integer> validOrderCountList = new ArrayList<>();
        for (LocalDate date : dateList) {
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN); //00:00:00
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX); //23:59:59
            Map map = new HashMap<>();
            map.put("begin", beginTime);
            map.put("end", endTime);
            Integer orderCount = orderMapper.countByMap(map);
            orderCount = orderCount == null ? 0 : orderCount;
            orderCountList.add(orderCount);

            map.put("status", Orders.COMPLETED);
            Integer validOrderCount = orderMapper.countByMap(map);
            validOrderCount = validOrderCount == null ? 0 : validOrderCount;
            validOrderCountList.add(validOrderCount);
        }

        //计算30天内totalOrderCount
        Integer totalOrderCount = orderCountList.stream().reduce(Integer::sum).get();

        //计算30天内validOrderCount
        Integer validOrderCount = validOrderCountList.stream().reduce(Integer::sum).get();

        //计算orderCompletionRate
        Double orderCompletionRate = 0.0;
        if (totalOrderCount != 0){
            orderCompletionRate = (double) validOrderCount / totalOrderCount;
        }

        return OrderReportVO
                .builder()
                .dateList(StringUtils.join(dateList, ","))
                .orderCountList(StringUtils.join(orderCountList, ","))
                .validOrderCountList(StringUtils.join(validOrderCountList, ","))
                .totalOrderCount(totalOrderCount)
                .validOrderCount(validOrderCount)
                .orderCompletionRate(orderCompletionRate)
                .build();
    }

    /**
     * 查询销量排名top10
     * @param begin
     * @param end
     * @return
     */
    @Override
    public SalesTop10ReportVO getSalesTop10(LocalDate begin, LocalDate end) {
        LocalDateTime beginTime = LocalDateTime.of(begin, LocalTime.MIN); //00:00:00
        LocalDateTime endTime = LocalDateTime.of(end, LocalTime.MAX); //23:59:59
        List<GoodsSalesDTO> getSalesTop10List = orderMapper.getSalesTop10(beginTime, endTime);

        List<String> nameList = getSalesTop10List.stream().map(GoodsSalesDTO::getName).collect(Collectors.toList());
        List<Integer> numberList = getSalesTop10List.stream().map(GoodsSalesDTO::getNumber).collect(Collectors.toList());

        return SalesTop10ReportVO
                .builder()
                .numberList(StringUtils.join(numberList, ","))
                .nameList(StringUtils.join(nameList, ","))
                .build();
    }

    /**
     * 导出营业额数据
     * @param response
     */
    @Override
    public void exportBussinessData(HttpServletResponse response) {
        //1.查询营业数据--查询最近30天的营业额数据
        //查询时间段
        LocalDate begin = LocalDate.now().minusDays(30);
        LocalDateTime beginTime = LocalDateTime.of(begin, LocalTime.MIN); //00:00:00
        LocalDate end = LocalDate.now().minusDays(1);
        LocalDateTime endTime = LocalDateTime.of(end, LocalTime.MAX); //23:59:59
        //查询概览数据
        BusinessDataVO businessDataVO = workspaceService.getBusinessData(beginTime, endTime);

        //2.使用POI将营业数据写入excel文件
        InputStream in = this.getClass().getClassLoader().getResourceAsStream("template/运营数据报表模板.xlsx");
        try {
            //基于模板文件创建一个新的excel文件
            XSSFWorkbook excel = new XSSFWorkbook(in);
            //获取excel文件的sheet页
            XSSFSheet sheet = excel.getSheet("Sheet1");
            //填充数据-时间段
            sheet.getRow(1).getCell(1).setCellValue("时间：" + begin.toString() + "至" + end.toString());
            //获得第四行，并填充数据
            XSSFRow row = sheet.getRow(3);
            row.getCell(2).setCellValue(businessDataVO.getTurnover());
            row.getCell(4).setCellValue(businessDataVO.getOrderCompletionRate());
            row.getCell(6).setCellValue(businessDataVO.getNewUsers());
            //获得第五行，并填充数据
            row = sheet.getRow(4);
            row.getCell(2).setCellValue(businessDataVO.getValidOrderCount());
            row.getCell(4).setCellValue(businessDataVO.getUnitPrice());

            //填充明细数据
            for (int i = 0; i < 30; i++) {
                LocalDate date = begin.plusDays(i);
                //查询某一天的营业数据
                BusinessDataVO data = workspaceService.getBusinessData(LocalDateTime.of(date, LocalTime.MIN), LocalDateTime.of(date, LocalTime.MAX));
                //获取第8+i行
                row = sheet.getRow(7 + i);
                //填充数据
                row.getCell(1).setCellValue(date.toString());
                row.getCell(2).setCellValue(data.getTurnover());
                row.getCell(3).setCellValue(data.getValidOrderCount());
                row.getCell(4).setCellValue(data.getOrderCompletionRate());
                row.getCell(5).setCellValue(data.getUnitPrice());
                row.getCell(6).setCellValue(data.getNewUsers());
            }

            //3.利用输出流将excel文件下载到客户端浏览器
            ServletOutputStream out = response.getOutputStream();
            excel.write(out);

            //关闭资源
            out.close();
            excel.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }



}
