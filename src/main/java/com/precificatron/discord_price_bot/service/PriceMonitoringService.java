package com.precificatron.discord_price_bot.service;

import jakarta.annotation.PostConstruct;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;

@Service
public class PriceMonitoringService {
    @Value("${monitor.product.url}")
    private String productUrl;

    @Value("${monitor.price.selector}")
    private String priceSelector;

    private BigDecimal lastPrice = null;

    @Scheduled(fixedRate= 10000)
    public void checkPrice() {
        try {
            Document doc = Jsoup.connect(productUrl)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                    .get();
            String priceText= doc.selectFirst(priceSelector).text();

            String cleanText = priceText.replaceAll("[^0-9,]", "")
                    .replace(",", ".");
            BigDecimal price = new BigDecimal(cleanText);
            System.out.println("preço encontrado: " + price);
            System.out.println("Ultimo preço encontrado: " + lastPrice);

            if (lastPrice == null || price.compareTo(lastPrice) < 0) {
                System.out.println("PROMOÇAO ENCONTRADA DE R$" + lastPrice +" para R$" + price);
                lastPrice=price;
            } else {
                System.out.println("nada de promo por aqui");
            }
        }
        catch (IOException e) {
            System.err.println("Erro ao tentar acessar a URL: " + productUrl);
             }
        catch (NullPointerException e) {
            System.err.println("Erro ao extrair o preço. seletor CSS " + priceSelector);
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }
}
