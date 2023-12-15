package org.yuezhikong.utils;

public class checks {
    private checks() {
    }

    /**
     * 参数检查
     * @param expression 表达式
     * @param errorMessage 表达式为true时抛出的异常信息
     * @throws IllegalArgumentException 表达式为true时抛出的异常
     */
    public static void checkArgument(boolean expression,String errorMessage) {
        if (expression) {
            throw new IllegalArgumentException(errorMessage);
        }
    }

    /**
     * 状态检查
     * @param expression 表达式
     * @param errorMessage 当表达式为true时抛出的异常信息
     * @throws IllegalStateException 表达式为true时抛出的异常
     */
    public static void checkState(boolean expression,String errorMessage) {
        if (expression) {
            throw new IllegalStateException(errorMessage);
        }
    }

}
