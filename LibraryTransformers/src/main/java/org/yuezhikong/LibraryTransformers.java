package org.yuezhikong;

import org.apache.maven.plugins.shade.relocation.Relocator;
import org.apache.maven.plugins.shade.resource.ResourceTransformer;

import java.io.InputStream;
import java.util.List;
import java.util.Locale;
import java.util.jar.JarOutputStream;

public class LibraryTransformers implements ResourceTransformer {
    private final static boolean DebugMode = false;
    @Override
    public boolean canTransformResource(String str) {
        String s = str.toLowerCase(Locale.ROOT);
        if (s.contains("license") || s.contains("info_") || s.endsWith(".html"))
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
                    System.out.println("Delete Library:"+s);
                return true;
            } else {
                if (DebugMode)
                    System.out.println("Allow Library:"+s);
            }
        }
        return false;
    }

    @Override
    public void processResource(String s, InputStream inputStream, List<Relocator> list) {

    }


    @Override
    public boolean hasTransformedResource() {
        return true;
    }

    @Override
    public void modifyOutputStream(JarOutputStream jarOutputStream) {

    }
}
