package Crawl;

import edu.uci.ics.crawler4j.crawler.CrawlConfig;
import edu.uci.ics.crawler4j.crawler.authentication.AuthInfo;
import edu.uci.ics.crawler4j.crawler.authentication.BasicAuthInfo;
import edu.uci.ics.crawler4j.crawler.authentication.FormAuthInfo;
import edu.uci.ics.crawler4j.crawler.authentication.NtAuthInfo;
import edu.uci.ics.crawler4j.fetcher.IdleConnectionMonitorThread;
import edu.uci.ics.crawler4j.fetcher.PageFetcher;
import org.apache.http.HttpHost;
import org.apache.http.NameValuePair;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.NTCredentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.ConnectionKeepAliveStrategy;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.cookie.ClientCookie;
import org.apache.http.impl.client.*;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.ssl.SSLContexts;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.CookieStore;
import java.net.HttpCookie;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;


public class PageFetcherCookie extends PageFetcher {
    public PageFetcherCookie(CrawlConfig config, CookieStore cookieStore) {
        super(config);
        this.connectionMonitorThread.shutdown();
        this.connectionMonitorThread = null;
        RequestConfig requestConfig = RequestConfig.custom().setExpectContinueEnabled(false).setCookieSpec("default").setRedirectsEnabled(false).setSocketTimeout(config.getSocketTimeout()).setConnectTimeout(config.getConnectionTimeout()).build();
        RegistryBuilder<ConnectionSocketFactory> connRegistryBuilder = RegistryBuilder.create();
        connRegistryBuilder.register("http", PlainConnectionSocketFactory.INSTANCE);
        if (config.isIncludeHttpsPages()) {
            try {
                SSLContext sslContext = SSLContexts.custom().loadTrustMaterial(null, (TrustStrategy) (chain, authType) -> true).build();
                SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(sslContext, SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
                connRegistryBuilder.register("https", sslsf);
            } catch (Exception var7) {
                logger.warn("Exception thrown while trying to register https");
                logger.debug("Stacktrace", var7);
            }
        }

        Registry<ConnectionSocketFactory> connRegistry = connRegistryBuilder.build();
        this.connectionManager = new PoolingHttpClientConnectionManager(connRegistry);
        this.connectionManager.setMaxTotal(config.getMaxTotalConnections());
        this.connectionManager.setDefaultMaxPerRoute(config.getMaxConnectionsPerHost());
        HttpClientBuilder clientBuilder = HttpClientBuilder.create();
        clientBuilder.setDefaultRequestConfig(requestConfig);
        BasicCookieStore tmpcookieStore = new BasicCookieStore();
        for (HttpCookie netcookie : cookieStore.getCookies()) {
            BasicClientCookie httpcookie = new BasicClientCookie(netcookie.getName(), netcookie.getValue());
            if (netcookie.getDomain().contains("local")) {
                httpcookie.setDomain("localhost");
            } else {
                httpcookie.setDomain(netcookie.getDomain());
            }
            httpcookie.setPath(netcookie.getPath());
            httpcookie.setVersion(netcookie.getVersion());
            httpcookie.setComment(netcookie.getComment());
            if (netcookie.getMaxAge() != -1) {
                httpcookie.setExpiryDate(new Date(netcookie.getMaxAge() * 1000));
            }
            httpcookie.setAttribute(ClientCookie.DOMAIN_ATTR, "true");
            tmpcookieStore.addCookie(httpcookie);
        }
        BasicClientCookie httpcookie = new BasicClientCookie("crawler", "1");
        httpcookie.setDomain("localhost");
        httpcookie.setAttribute(ClientCookie.DOMAIN_ATTR, "true");
        httpcookie.setPath("/");
        tmpcookieStore.addCookie(httpcookie);
        clientBuilder.setDefaultCookieStore(tmpcookieStore);
        clientBuilder.setConnectionManager(this.connectionManager);
        clientBuilder.setKeepAliveStrategy(new DefaultConnectionKeepAliveStrategy());
        clientBuilder.setUserAgent("Cruzzer");
        clientBuilder.setDefaultHeaders(config.getDefaultHeaders());
        if (config.getProxyHost() != null) {
            if (config.getProxyUsername() != null) {
                BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
                credentialsProvider.setCredentials(new AuthScope(config.getProxyHost(), config.getProxyPort()), new UsernamePasswordCredentials(config.getProxyUsername(), config.getProxyPassword()));
                clientBuilder.setDefaultCredentialsProvider(credentialsProvider);
            }

            HttpHost proxy = new HttpHost(config.getProxyHost(), config.getProxyPort());
            clientBuilder.setProxy(proxy);
            logger.debug("Working through Proxy: {}", proxy.getHostName());
        }

        this.httpClient = clientBuilder.build();
        if (config.getAuthInfos() != null && !config.getAuthInfos().isEmpty()) {
            this.doAuthetication(config.getAuthInfos());
        }

        if (this.connectionMonitorThread == null) {
            this.connectionMonitorThread = new IdleConnectionMonitorThread(this.connectionManager);
        }

        this.connectionMonitorThread.start();
    }


    private void doAuthetication(List<AuthInfo> authInfos) {
        Iterator var2 = authInfos.iterator();

        while (var2.hasNext()) {
            AuthInfo authInfo = (AuthInfo) var2.next();
            if (authInfo.getAuthenticationType() == AuthInfo.AuthenticationType.BASIC_AUTHENTICATION) {
                this.doBasicLogin((BasicAuthInfo) authInfo);
            } else if (authInfo.getAuthenticationType() == AuthInfo.AuthenticationType.NT_AUTHENTICATION) {
                this.doNtLogin((NtAuthInfo) authInfo);
            } else {
                this.doFormLogin((FormAuthInfo) authInfo);
            }
        }

    }

    private void doBasicLogin(BasicAuthInfo authInfo) {
        logger.info("BASIC authentication for: " + authInfo.getLoginTarget());
        HttpHost targetHost = new HttpHost(authInfo.getHost(), authInfo.getPort(), authInfo.getProtocol());
        CredentialsProvider credsProvider = new BasicCredentialsProvider();
        credsProvider.setCredentials(new AuthScope(targetHost.getHostName(), targetHost.getPort()), new UsernamePasswordCredentials(authInfo.getUsername(), authInfo.getPassword()));
        this.httpClient = HttpClients.custom().setDefaultCredentialsProvider(credsProvider).build();
    }

    private void doNtLogin(NtAuthInfo authInfo) {
        logger.info("NT authentication for: " + authInfo.getLoginTarget());
        HttpHost targetHost = new HttpHost(authInfo.getHost(), authInfo.getPort(), authInfo.getProtocol());
        BasicCredentialsProvider credsProvider = new BasicCredentialsProvider();

        try {
            credsProvider.setCredentials(new AuthScope(targetHost.getHostName(), targetHost.getPort()), new NTCredentials(authInfo.getUsername(), authInfo.getPassword(), InetAddress.getLocalHost().getHostName(), authInfo.getDomain()));
        } catch (UnknownHostException var5) {
            logger.error("Error creating NT credentials", var5);
        }

        this.httpClient = HttpClients.custom().setDefaultCredentialsProvider(credsProvider).build();
    }

    private void doFormLogin(FormAuthInfo authInfo) {
        logger.info("FORM authentication for: " + authInfo.getLoginTarget());
        String fullUri = authInfo.getProtocol() + "://" + authInfo.getHost() + ":" + authInfo.getPort() + authInfo.getLoginTarget();
        HttpPost httpPost = new HttpPost(fullUri);
        List<NameValuePair> formParams = new ArrayList();
        formParams.add(new BasicNameValuePair(authInfo.getUsernameFormStr(), authInfo.getUsername()));
        formParams.add(new BasicNameValuePair(authInfo.getPasswordFormStr(), authInfo.getPassword()));

        try {
            UrlEncodedFormEntity entity = new UrlEncodedFormEntity(formParams, "UTF-8");
            httpPost.setEntity(entity);
            this.httpClient.execute(httpPost);
            logger.debug("Successfully Logged in with user: " + authInfo.getUsername() + " to: " + authInfo.getHost());
        } catch (UnsupportedEncodingException var6) {
            logger.error("Encountered a non supported encoding while trying to login to: " + authInfo.getHost(), var6);
        } catch (ClientProtocolException var7) {
            logger.error("While trying to login to: " + authInfo.getHost() + " - Client protocol not supported", var7);
        } catch (IOException var8) {
            logger.error("While trying to login to: " + authInfo.getHost() + " - Error making request", var8);
        }

    }
}
