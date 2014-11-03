import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import com.google.common.collect.Sets;
import com.google.common.io.Files;


public class GradleStartCommon
{
    public static Logger LOGGER = LogManager.getLogger("GradleStart");
    
    public static void launch(String mainClass, String[] args)
    {
        try {
            searchCoremods();
            LOGGER = null;
            System.gc();
            Class.forName(mainClass).getDeclaredMethod("main", String[].class).invoke(null, new Object[] {args});
        }
        catch (Exception e)
        {
            Throwables.propagate(e);
        }
    }
    
    private static final String COREMOD_VAR = "fml.coreMods.load";
    private static final String COREMOD_MF  = "FMLCorePlugin";

    public static void searchCoremods() throws IOException
    {
        Set<String> coremodsSet = Sets.newHashSet();
        
        if (!Strings.isNullOrEmpty(System.getProperty(COREMOD_VAR)))
            coremodsSet.addAll(Splitter.on(',').splitToList(System.getProperty(COREMOD_VAR)));
        
        for (URL url : ((URLClassLoader) GradleStartCommon.class.getClassLoader()).getURLs())
        {
            if (!url.getProtocol().startsWith("file")) // because file urls start with file://
                continue; // this isnt a file
            
            File coreMod = new File(url.getFile());
            Manifest manifest = null;
            
            if (coreMod.isDirectory())
            {
                File manifestMF = new File(coreMod, "META-INF/MANIFEST.MF");
                if (manifestMF.exists())
                {
                    FileInputStream stream = new FileInputStream(manifestMF);
                    manifest = new Manifest(stream);
                    stream.close();
                }
            }
            else if (coreMod.getName().endsWith("jar")) // its a jar
            {
                JarFile jar = new JarFile(coreMod);
                manifest = jar.getManifest();
                jar.close();
            }
            
            // we got the manifest? use it.
            if (manifest != null)
            {
                String clazz = manifest.getMainAttributes().getValue(COREMOD_MF);
                if (!Strings.isNullOrEmpty(clazz))
                {
                    LOGGER.info("Found and added coremod: "+clazz);
                    coremodsSet.add(clazz);
                }
            }
        }
        
        System.setProperty(COREMOD_VAR, Joiner.on(',').join(coremodsSet));
    }
}