package main.config;

import com.neovisionaries.ws.client.WebSocketFactory;
import okhttp3.OkHttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.concurrent.TimeUnit;

@Configuration
public class DiscordHttpConfig {

    @Bean
    public WebSocketFactory telegramWebSocketFactory() {
        WebSocketFactory factory = new WebSocketFactory().setConnectionTimeout(30_000);

        if (Config.IS_PROXY) {
            // Port 10808 accepts HTTP CONNECT as well as SOCKS.
            factory.getProxySettings()
                    .setHost(Config.PROXY_IP)
                    .setPort(10808);
        }

        return factory;
    }

    @Bean
    public OkHttpClient telegramOkHttpClient() {
        if (Config.IS_PROXY) {
            Proxy proxy = new Proxy(Proxy.Type.SOCKS, new InetSocketAddress(Config.PROXY_IP, 10808));

            return new OkHttpClient.Builder()
                    .proxy(proxy)
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(80, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .build();
        }

        return new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(80, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
    }
}