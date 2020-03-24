package io.github.terra121.letsencryptcraft;
import org.apache.commons.io.IOUtils;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.cert.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LetsEncryptAdder
{
    public static void addLetsEncryptCertificate() throws Exception
    {
        InputStream cert = LetsEncryptAdder.class.getResourceAsStream("/assets/terra121/letsencryptroot/lets-encrypt-x3-cross-signed.der");

        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        Path ksPath = Paths.get(System.getProperty("java.home"),"lib", "security", "cacerts");
        keyStore.load(Files.newInputStream(ksPath), "changeit".toCharArray());

        CertificateFactory cf = CertificateFactory.getInstance("X.509");

        InputStream caInput = new BufferedInputStream(cert);
        Certificate crt = cf.generateCertificate(caInput);

        keyStore.setCertificateEntry("lets-encrypt-x3-cross-signed", crt);

        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(keyStore);
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, tmf.getTrustManagers(), null);
        SSLContext.setDefault(sslContext);
    }

    public static void doStuff(ILetsEncryptMod mod)
    {
        String version = System.getProperty("java.version");
        Pattern p = Pattern.compile("^(\\d+\\.\\d+).*?_(\\d+).*");
        Matcher matcher = p.matcher(version);
        String majorVersion;
        int minorVersion;
        if (matcher.matches())
        {
            majorVersion = matcher.group(1);
            minorVersion = Integer.valueOf(matcher.group(2));
        } else {
            majorVersion = "1.7";
            minorVersion = 110;
            mod.info("Regex to parse Java version failed - applying anyway.");
        }

        switch (majorVersion)
        {
            case "1.7":
                if (minorVersion >= 111)
                {
                    mod.info("Not running as Java version is at least Java 7u111.");
                    return;
                }
                break;
            case "1.8":
                if (minorVersion >= 101)
                {
                    mod.info("Not running as Java version is at least Java 8u101.");
                    return;
                }
                break;
        }

        String body = "";
        try {
            mod.info("Adding Let's Encrypt certificate...");
            LetsEncryptAdder.addLetsEncryptCertificate();
            mod.info("Done, attempting to connect to https://helloworld.letsencrypt.org...");
            URL url = new URL("https://helloworld.letsencrypt.org");
            URLConnection conn = url.openConnection();
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            InputStream inputStream = conn.getInputStream();
            body = IOUtils.toString(inputStream);
        } catch (Exception e) {
            mod.error("An error occurred whilst adding the Let's Encrypt root certificate. I'm afraid you wont be able to access resources with a Let's Encrypt certificate D:", e);
        }

        if (body.isEmpty())
        {
            mod.error("An unknown error occurred whilst adding the Let's Encrypt root certificate. I'm afraid you may not be able to access resources with a Let's Encrypt certificate D:");
        } else {
            mod.info("Done - you are now able to access resources with a Let's Encrypt certificate :D");
        }
    }
}