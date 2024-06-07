package org.yuezhikong.Server.request;

import lombok.extern.slf4j.Slf4j;
import org.jline.reader.Candidate;
import org.jline.reader.Completer;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
final class JavaIMCompleter implements Completer {

    final List<ChatRequest.CommandInformation> informations = new CopyOnWriteArrayList<>();

    @Override
    public void complete(LineReader reader, ParsedLine line, List<Candidate> candidates) {
        Objects.requireNonNull(line);
        Objects.requireNonNull(candidates);
        String command;
        if (!line.words().get(0).startsWith("/"))
            command = line.words().get(0);
        else
            command = line.words().get(0).substring(1);
        informations.forEach(info -> {
            if (!info.command().contains(command))
                return;
            try {
                info.commandInstance().complete(reader, line, candidates);
            } catch (Throwable t) {
                log.error("在处理指令自动补全时出现错误!",t);
            }
        });
    }
}
