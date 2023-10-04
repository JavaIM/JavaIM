package org.yuezhikong;

import org.apache.maven.plugins.shade.relocation.Relocator;
import org.apache.maven.plugins.shade.resource.ResourceTransformer;

import java.io.InputStream;
import java.util.List;
import java.util.jar.JarOutputStream;

public class LibraryTransformers implements ResourceTransformer {
    private final boolean DebugMode = false;
    /**
     * 判断是否是一个不排除的Library
     * @param FileName Library名称
     * @param AllowName 允许的Library名称
     * @return True为是，False为否
     */
    public boolean isAllowLibrary(String FileName,String AllowName)
    {
        if (FileName.equals(AllowName+".dll"))
        {
            return true;
        }
        return FileName.equals("lib" + AllowName + ".so");
    }
    @Override
    public boolean canTransformResource(String s) {
        if (s.endsWith(".dll") || s.endsWith(".so"))
        {
            if (!isAllowLibrary(s, "glass") &&
                !isAllowLibrary(s, "glassgtk2") &&
                !isAllowLibrary(s, "glassgtk3") &&
                !isAllowLibrary(s, "javafx_font") &&
                !isAllowLibrary(s, "javafx_font_freetype") &&
                !isAllowLibrary(s, "javafx_font_pango") &&
                !isAllowLibrary(s, "javafx_iio") &&
                !isAllowLibrary(s, "prism_common") &&
                !isAllowLibrary(s, "prism_es2") &&
                !isAllowLibrary(s, "prism_sw") &&
                !isAllowLibrary(s, "decora_sse") &&
                !isAllowLibrary(s, "prism_d3d") &&
                !s.contains("jnidispatch.dll") &&
                !s.contains("jnidispatch.so") &&
                !s.contains("sqlitejdbc.dll") &&
                !s.contains("libsqlitejdbc.so"))
            {
                if (DebugMode)
                    System.out.println("Delete Library:"+s);
                return true;
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
