package com.zzh.luntan.actuator;

import com.zzh.luntan.util.CommunityUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
//我们直接在地址栏输入...//actuator/database即可访问，database就是id
@Component
@Endpoint(id = "database")
public class DataBaseEndPoint {
    // 自定义监控点，用于获取数据库链接对象是否正常
    private static final Logger logger = LoggerFactory.getLogger(DataBaseEndPoint.class);
    @Autowired
    private DataSource dataSource;

    // 使用此注解标明该方法被get请求调用
    @ReadOperation
    public String checkConnection() {
        // 向前端返回一个Json字符串
        try ( // 因为在application.properties配置了datasource为mysql，所以获取连接对象如果存在就是mysql的连接对象
              //括号中的对象在方法结束会自动释放资源
                  Connection connection = dataSource.getConnection();) {
                return CommunityUtil.getJSONString(0, "获取连接对象成功!");
        } catch (SQLException e) {
            logger.error("获取连接失败:" + e.getMessage());
            return CommunityUtil.getJSONString(1, "获取连接失败!");
        }
    }
}
