package org.yuezhikong.utils.CustomExceptions;

public class UserNotFoundException extends Exception{
    //构造函数
    public UserNotFoundException(String message){
        super(message);
    }
}
