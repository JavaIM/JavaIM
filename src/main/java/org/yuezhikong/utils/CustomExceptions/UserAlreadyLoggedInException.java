package org.yuezhikong.utils.CustomExceptions;

public class UserAlreadyLoggedInException extends Exception{
    //构造函数
    public UserAlreadyLoggedInException(String message){
        super(message);
    }
}
