package org.yuezhikong.transformers;

import org.apache.maven.plugins.shade.relocation.Relocator;
import org.apache.maven.plugins.shade.resource.ResourceTransformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;
import java.util.jar.JarOutputStream;

public class LibraryTransformers implements ResourceTransformer {
    private static final Logger log = LoggerFactory.getLogger(LibraryTransformers.class);
    private final static boolean DebugMode = false;
    @Override
    public boolean canTransformResource(String str) {
        String s = str.toLowerCase(Locale.ROOT);
        if (s.contains("license") || s.contains("info_") || s.endsWith(".html"))
            return true;
        if (s.equals("authors") || s.contains("changelog") || s.equals("release-timestamp.txt") || s.contains("lombok")
                || s.equals("meta-inf/services/javax.annotation.processing.processor") || s.contains("readme") || s.startsWith("org/iq80/snappy/")
                || s.contains("about") || s.contains("notice") || s.endsWith(".jar"))
            return true;
        if (s.endsWith(".dll") || s.endsWith(".so"))
        {
            if (!s.contains("libjlinenative.so") &&
                    !s.contains("jlinenative.dll") &&
                    !s.contains("libjansi.so") &&
                    !s.contains("jansi.dll") &&
                    !s.contains("jnidispatch.so") &&
                    !s.contains("libnetty_transport_native_epoll_x86_64.so") &&
                    !s.contains("libnetty_transport_native_epoll_aarch_64.so") &&
                    !s.contains("libnetty_transport_native_epoll_riscv64.so") &&
                    !s.contains("sqlitejdbc.dll") &&
                    !s.contains("libsqlitejdbc.so"))
            {
                if (DebugMode)
                    log.info("Delete Library:{}",s);
                return true;
            } else {
                if (DebugMode)
                    log.info("Allow Library:{}",s);
            }
        }
        return false;
    }

    @Override
    public void processResource(String s, InputStream inputStream, List<Relocator> list) throws IOException {

    }

    @Override
    public boolean hasTransformedResource() {
        return true;
    }

    @Override
    public void modifyOutputStream(JarOutputStream jarOutputStream) {

    }
}
