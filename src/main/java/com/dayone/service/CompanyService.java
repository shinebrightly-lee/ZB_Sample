package com.dayone.service;

import com.dayone.exception.impl.*;
import com.dayone.model.*;
import com.dayone.persist.entity.*;
import com.dayone.persist.repository.*;
import com.dayone.scraper.*;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.Trie;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CompanyService {

    private final Scraper yahooFinanceScraper;
    private final CompanyRepository companyRepository;
    private final DividendRepository dividendRepository;

    private final Trie<String, String> trie;

    public Company save(String ticker) {

        boolean exists = companyRepository.existsByTicker(ticker);

        if(exists) {
            throw new RuntimeException("already exists ticker -> " + ticker);
        }

        return storeCompanyAndDividend(ticker);
    }

    public Page<CompanyEntity> getAllCompany(Pageable pageable) {
        return companyRepository.findAll(pageable);
    }

    public void addAutoCompleteKeyword(String keyword) {
        trie.put(keyword, null);
    }

    public List<String> autoComplete(String keyword) {
        return new ArrayList<>(trie.prefixMap(keyword).keySet());
    }

    public void deleteAutoCompleteKeyword(String keyword) {
        trie.remove(keyword);
    }

    public List<String> getCompanyNamesByKeyword(String keyword) {
        Pageable limit = PageRequest.of(0, 10);
        Page<CompanyEntity> companyEntities = companyRepository.findByNameStartingWithIgnoreCase(keyword, limit);
        return companyEntities.stream()
                .map(CompanyEntity::getName)
                .collect(Collectors.toList());
    }

    private Company storeCompanyAndDividend(String ticker) {

        // ticker 를 기준으로 회사를 스크래핑
        Company company = yahooFinanceScraper.scrapCompanyByTicker(ticker);
        if (ObjectUtils.isEmpty(company)) {
            throw new RuntimeException("failed to scrap ticker -> " + ticker);
        }

        // 해당 회사가 존재할 경우, 회사의 배당금 정보를 스크래핑
        ScrapedResult scrapedResult = yahooFinanceScraper.scrap(company);

        // 스크래핑 결과
        CompanyEntity companyEntity = companyRepository.save(new CompanyEntity(company));
        List<DividendEntity> dividendEntities = scrapedResult.getDividends().stream()
                .map(e -> new DividendEntity(companyEntity.getId(), e))
                .collect(Collectors.toList());

        dividendRepository.saveAll(dividendEntities);

        return company;
    }

    public String deleteCompany(String ticker) {

        CompanyEntity company = companyRepository.findByTicker(ticker)
                .orElseThrow(NoCompanyException::new);

        dividendRepository.deleteAllByCompanyId(company.getId());
        companyRepository.delete(company);

        // Trie 에 있는 데이터 삭제
        deleteAutoCompleteKeyword(company.getName());

        return company.getName();
    }
}