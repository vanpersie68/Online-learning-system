package com.xuecheng.base.exception;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.ArrayList;
import java.util.List;

/**
 * 全局异常处理器
 */
@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler
{
   @ResponseBody
   @ExceptionHandler(XueChengPlusException.class)
   @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
   public RestErrorResponse customException(XueChengPlusException e) //自定义的异常
   {
      log.error("【系统异常】{}",e.getErrMessage(),e);
      return new RestErrorResponse(e.getErrMessage());
   }

   @ResponseBody
   @ExceptionHandler(Exception.class)
   @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
   public RestErrorResponse exception(Exception e) //系统的异常
   {
      log.error("【系统异常】{}",e.getMessage(),e);

      if(e.getMessage().equals("不允许访问"))
      {
         return new RestErrorResponse("没有操作此功能的权限");
      }
      return new RestErrorResponse(CommonError.UNKOWN_ERROR.getErrMessage()); //将数据库连接失败、网络问题等错误用 统一的回复方法抛出
   }

   @ResponseBody
   @ExceptionHandler(MethodArgumentNotValidException.class)
   @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
   public RestErrorResponse MethodArgumentNotValidException(MethodArgumentNotValidException e) //系统的异常
   {
      BindingResult bindingResult = e.getBindingResult();
      //存放错误信息
      List<String> errors = new ArrayList<>();
      bindingResult.getFieldErrors().stream().forEach(item->{
         errors.add(item.getDefaultMessage());
      });

      //将list中的错误信息拼接起来
      String errorMessage = StringUtils.join(errors, ",");
      log.error("【系统异常】{}",errorMessage);
      return new RestErrorResponse(errorMessage); //将数据库连接失败、网络问题等错误用 统一的回复方法抛出
   }
}