package com.dayone.scraper;

import com.dayone.model.Company;
import com.dayone.model.Dividend;
import com.dayone.model.ScrapedResult;
import com.dayone.model.constants.Month;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
public class YahooFinanceScraper implements Scraper {

    private static final String STATISTICS_URL = "https://finance.yahoo.com/quote/%s/history?period1=%d&period2=%d&interval=1mo";
    private static final String SUMMARY_URL = "https://finance.yahoo.com/quote/%s?p=%s";

    private static final long START_TIME = 86400;   // 60 * 60 * 24


    @Override
    public ScrapedResult scrap(Company company) {

        ScrapedResult scrapedResult = null;

        try {
            long now = System.currentTimeMillis() / 1000;   // 초 단위로 변경, 1970/01/01 부터 현재까지 걸린 시간 의미
            String url = String.format(STATISTICS_URL, company.getTicker(), START_TIME, now);

            Connection connect = Jsoup.connect(url);
            Document document = connect.get();

            Elements parsingDivs = document.body().getElementsByAttributeValue("data-test", "historical-prices");
            Element tableElement = parsingDivs.get(0);

            Element tbody = tableElement.child(1);

            List<Dividend> dividends = new ArrayList<>();

            for (Element e : tbody.children()) {
                String text = e.text();

                if (!text.endsWith("Dividend")) {
                    continue;
                }

                String[] split = text.split(" ");
                int month = Month.strToNumber(split[0]);
                int day = Integer.parseInt(split[1].replace(",", ""));
                int year = Integer.parseInt(split[2]);
                String dividend = split[3];

                if(month < 0) {
                    throw new RuntimeException("unexpected Month enum value -> " + split[0]);
                }

                dividends.add(new Dividend(LocalDateTime.of(year, month, day, 0, 0), dividend));

            }
            scrapedResult = ScrapedResult.builder()
                    .company(company)
                    .dividends(dividends)
                    .build();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return scrapedResult;
    }

    @Override
    public Company scrapCompanyByTicker(String ticker) {
        String url = String.format(SUMMARY_URL, ticker, ticker);

        try {
            Document document = Jsoup.connect(url).get();
            Element titleEle = document.getElementsByTag("h1").get(0);
            String title = titleEle.text().split(" - ")[1].trim();

            return new Company(ticker, title);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
