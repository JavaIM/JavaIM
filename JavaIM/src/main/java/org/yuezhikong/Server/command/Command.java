package org.yuezhikong.Server.command;

import org.jline.reader.Candidate;
import org.jline.reader.Completer;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;
import org.yuezhikong.Server.UserData.user;

import java.util.List;

public interface Command extends Completer {

    /**
     * 当发生 Tab 补全时调用
     *
     * @param reader     line Reader
     * @param line       命令行
     * @param candidates The {@link List} of candidates to populate
     */
    @Override
    void complete(LineReader reader, ParsedLine line, List<Candidate> candidates);

    /**
     * 执行指令
     *
     * @param command 指令
     * @param args    参数
     * @param User    用户
     * @return 是否执行成功
     */
    boolean execute(String command, String[] args, user User);

    /**
     * 获取指令注释
     *
     * @return 注释
     */
    String getDescription();

    /**
     * 获取指令语法
     *
     * @return 语法
     */
    String getUsage();

    /**
     * 是否允许此指令执行被提示到管理员以及控制台
     *
     * @return 是/否允许
     */
    boolean isAllowBroadcastCommandRunning();
}
