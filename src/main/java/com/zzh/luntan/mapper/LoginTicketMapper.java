package com.zzh.luntan.mapper;

import com.zzh.luntan.entity.LoginTicket;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
@Deprecated
@Mapper
public interface LoginTicketMapper {
    int insertTicket(LoginTicket loginTicket);

    int updateTicketStatus(@Param("ticket") String ticket, @Param("status") int status);

    LoginTicket selectByTicket(@Param("ticket") String ticket);
}
