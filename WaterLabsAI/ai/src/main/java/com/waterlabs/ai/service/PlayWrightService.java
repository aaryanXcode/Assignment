package com.waterlabs.ai.service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.WaitForSelectorState;
import com.waterlabs.ai.dto.PropertyDTO;
import com.waterlabs.ai.dto.ScrapeFiltersDTO;
import com.waterlabs.ai.exceptions.ScrapperFailedException;

@Service
public class PlayWrightService {

    private static final Logger log = LoggerFactory.getLogger(PlayWrightService.class);
    private static final Pattern NUM = Pattern.compile("(\\d+)");
    private static final Pattern RANGE = Pattern.compile("(\\d+)\\s*to\\s*(\\d+)");
    private static final Pattern AGE_RANGE = Pattern.compile("(\\d+)\\s*to\\s*(\\d+)\\s*years?", Pattern.CASE_INSENSITIVE);
    private static final Pattern AGE_SINGLE = Pattern.compile("(\\d+)\\s*years?", Pattern.CASE_INSENSITIVE);
    private static final Pattern AGE_LT = Pattern.compile("less than\\s*(\\d+)\\s*years?", Pattern.CASE_INSENSITIVE);
    private static final Pattern AGE_NEW = Pattern.compile("new|under construction", Pattern.CASE_INSENSITIVE);


    private final Playwright playwright;

    public PlayWrightService(Playwright playwright) {
        this.playwright = playwright;
    }

    public List<PropertyDTO> getPropertyDetails(ScrapeFiltersDTO filters) {
        log.info("Starting scrape — filters: {}", filters);
        try (Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
             BrowserContext context = browser.newContext(new Browser.NewContextOptions()
                     // Mimic a real Chrome user-agent so MagicBricks bot detection doesn't trigger
                     .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
                     // Realistic viewport
                     .setViewportSize(1366, 768)
                     // Tell the site JS that plugins exist (headless Chromium reports 0 plugins)
                     .setJavaScriptEnabled(true))) {

            Page page = context.newPage();

            page.navigate("https://www.magicbricks.com/");
            page.waitForLoadState();
            log.info("MagicBricks loaded");

            // Rent
            page.locator("#tabRENT").click();
            page.waitForTimeout(1500);

            // City
            log.info("Typing city: {}", filters.city());
            Locator keyword = page.locator("#keyword");
            keyword.click();
            keyword.pressSequentially(filters.city(), new Locator.PressSequentiallyOptions().setDelay(100));

            // MagicBricks keeps #serachSuggest in the DOM but toggles it via CSS (display/opacity),
            // so Playwright's VISIBLE check never passes. Instead we wait until the container has
            // at least one child item attached, then force-dispatch a click to bypass any overlay.
            page.locator("#serachSuggest .mb-search__auto-suggest__item").first()
                    .waitFor(new Locator.WaitForOptions()
                            .setState(WaitForSelectorState.ATTACHED)
                            .setTimeout(15000));
            page.locator("#serachSuggest .mb-search__auto-suggest__item").first()
                    .dispatchEvent("click");
            page.waitForTimeout(1000);
            log.info("City selected");

            // Property Type
            log.info("Setting property type: {}", filters.propertyType());
            page.locator("#buy_proertyTypeDefault").click();
            page.locator("#residential_0").waitFor(new Locator.WaitForOptions()
                    .setState(WaitForSelectorState.ATTACHED)
                    .setTimeout(10000));

            // MagicBricks checkbox IDs: residential_0 = Flat/Apartment, residential_1 = House/Villa
            boolean wantFlat = filters.propertyType().equalsIgnoreCase("Apartment");
            Locator flatCheckbox  = page.locator("#residential_0");
            Locator houseCheckbox = page.locator("#residential_1");

            if (wantFlat) {
                if (houseCheckbox.isChecked()) {
                    page.locator("label[for='residential_1']").click();
                    page.waitForCondition(() -> !houseCheckbox.isChecked());
                }
                if (!flatCheckbox.isChecked()) {
                    page.locator("label[for='residential_0']").click();
                    page.waitForCondition(() -> flatCheckbox.isChecked());
                }
            } else {
                if (flatCheckbox.isChecked()) {
                    page.locator("label[for='residential_0']").click();
                    page.waitForCondition(() -> !flatCheckbox.isChecked());
                }
                if (!houseCheckbox.isChecked()) {
                    page.locator("label[for='residential_1']").click();
                    page.waitForCondition(() -> houseCheckbox.isChecked());
                }
            }

            // BHK — IDs: bhkFlatHouse_0=1BHK, _1=2BHK, _2=3BHK, _3=4BHK, _4=5BHK
            String[] bhkLabels = {"1", "2", "3", "4", "5+"};
            for (int i = 0; i < bhkLabels.length; i++) {
                boolean wanted = filters.bhk().contains(bhkLabels[i]);
                Locator cb = page.locator("#bhkFlatHouse_" + i);
                if (cb.count() == 0) continue;
                boolean checked = cb.isChecked();
                if (wanted && !checked) {
                    page.locator("label[for='bhkFlatHouse_" + i + "']").click();
                    final int idx = i;
                    page.waitForCondition(() -> page.locator("#bhkFlatHouse_" + idx).isChecked());
                } else if (!wanted && checked) {
                    page.locator("label[for='bhkFlatHouse_" + i + "']").click();
                    final int idx = i;
                    page.waitForCondition(() -> !page.locator("#bhkFlatHouse_" + idx).isChecked());
                }
            }
            page.keyboard().press("Escape");
            log.info("BHK set: {}", filters.bhk());

            // Budget
            page.locator("#rent_budget_lbl").click();
            page.locator("#budgetMin").waitFor(new Locator.WaitForOptions()
                    .setState(WaitForSelectorState.VISIBLE)
                    .setTimeout(10000));
            page.locator("#budgetMin").fill(String.valueOf(filters.minBudget()));
            page.locator("#budgetMax").fill(String.valueOf(filters.maxBudget()));
            page.keyboard().press("Escape");
            String minStr = String.valueOf(filters.minBudget());
            page.waitForCondition(() ->
                    page.locator("#rent_budget_lbl").textContent().contains(minStr));
            log.info("Budget set: {}-{}", filters.minBudget(), filters.maxBudget());

            // Search
            log.info("Submitting search...");
            page.locator(".mb-search__btn").click();
            page.waitForLoadState();
            page.waitForTimeout(5000);
            log.info("Search results loaded");

            return scrapeListings(page, filters);

        } catch (Exception ex) {
            log.error("Failed to scrape properties.", ex);
            throw new ScrapperFailedException("MagicBricks scraping failed: " + ex.getMessage());
        }
    }

    private List<PropertyDTO> scrapeListings(Page page, ScrapeFiltersDTO filters) {
        List<PropertyDTO> results = new ArrayList<>();

        Locator listWrappers = page.locator(".mb-srp__list[id^='cardid']");
        int count = listWrappers.count();
        log.info("Found {} property blocks on page", count);

        for (int i = 0; i < count && results.size() < 10; i++) {
            try {
                Locator wrapper = listWrappers.nth(i);
                String wrapperId = wrapper.getAttribute("id");
                String propertyId = wrapperId != null ? wrapperId.replace("cardid", "") : "";

                Locator card = wrapper.locator(".mb-srp__card").first();
                if (card.count() == 0) continue;

                String title = safeText(card, ".mb-srp__card--title");
                String society = safeText(card, ".mb-srp__card__society--name");
                String floorText = safeText(card, "[data-summary='floor'] .mb-srp__card__summary--value");
                String priceText = safeText(card, ".mb-srp__card__price--amount");

                String area = safeText(card, "[data-summary='carpet-area'] .mb-srp__card__summary--value");
                if (area.isEmpty()) {
                    area = safeText(card, "[data-summary='super-area'] .mb-srp__card__summary--value");
                }

                int floor = parseFloor(floorText);
                int price = parsePrice(priceText);

                // Cheap filters first — skip the expensive detail-page visit if these already fail
                if (floor < filters.minFloor() || price < filters.minBudget() || price > filters.maxBudget()) {
                    log.debug("Skipped (floor/price): {} | floor={} | price={}", title, floor, price);
                    continue;
                }

                String url = getListingUrl(page, card);
                int age = scrapeAgeFromDetailPage(page, url);

                // Only reject if age is explicitly known and fails the filter.
                // age == -1 means MagicBricks didn't list it — include with ageInYears=-1 (shown as N/A).
                if (age != -1 && age > filters.maxAge()) {
                    log.info("Skipped (age={}): {}", age, title);
                    continue;
                }

                results.add(new PropertyDTO(propertyId, title, society, price, floor, age, area, url));
                log.info("Added: {} | floor={} | price={} | age={} | url={}", title, floor, price, age, url);

            } catch (Exception cardEx) {
                log.warn("Skipping card {} due to error: {}", i, cardEx.getMessage());
            }
        }

        if (results.size() < 10) {
            log.warn("Only found {} matching properties out of {} scanned. " +
                      "Consider adding pagination to scan more listings.", results.size(), count);
        }

        return results;
    }
    
    private String safeText(Locator scope, String selector) {
        try {
            Locator loc = scope.locator(selector);
            return loc.count() > 0 ? loc.first().textContent().trim() : "";
        } catch (Exception e) {
            return "";
        }
    }

    private int parsePrice(String priceText) {
        if (priceText == null) return -1;
        String digits = priceText.replaceAll("[^0-9]", "");
        return digits.isEmpty() ? -1 : Integer.parseInt(digits);
    }

    private int parseFloor(String floorText) {
        if (floorText == null || floorText.isBlank()) return -1;
        String lower = floorText.toLowerCase();
        if (lower.startsWith("ground") || lower.startsWith("lower basement")) return 0;
        Matcher m = NUM.matcher(floorText);
        return m.find() ? Integer.parseInt(m.group(1)) : -1;
    }

    private int parseAgeIfPresent(String statusValue) {
        if (statusValue == null || !statusValue.toLowerCase().contains("age")) return -1;
        Matcher range = RANGE.matcher(statusValue);
        if (range.find()) return Integer.parseInt(range.group(2));
        Matcher single = NUM.matcher(statusValue);
        return single.find() ? Integer.parseInt(single.group(1)) : -1;
    }

    /**
     * MagicBricks does not expose a static href for individual listings in the
     * search results grid — the detail URL only resolves after a click. This
     * clicks the title, captures wherever navigation lands, then returns.
     */
    private String getListingUrl(Page page, Locator card) {
        try {
            Locator titleLink = card.locator("h2.mb-srp__card--title");
            if (titleLink.count() == 0) return "";

            try {
                Page popup = page.waitForPopup(() -> titleLink.first().click());
                String url = popup.url();
                popup.close();
                return url;
            } catch (Exception noPopup) {
                // Falls back to same-tab navigation
                titleLink.first().click();
                page.waitForLoadState();
                String url = page.url();
                page.goBack();
                page.waitForLoadState();
                page.waitForTimeout(500);
                return url;
            }
        } catch (Exception e) {
            log.warn("Could not resolve listing URL: {}", e.getMessage());
            return "";
        }
    }
    
    private int scrapeAgeFromDetailPage(Page page, String detailUrl) {
        if (detailUrl == null || detailUrl.isBlank()) return -1;
        Page detailPage = null;
        try {
            detailPage = page.context().newPage();
            detailPage.navigate(detailUrl);
            detailPage.waitForLoadState();
            detailPage.waitForTimeout(1000);

            // Find the "Age of Construction" row specifically, not a random text match
            Locator ageRow = detailPage.locator(".mb-ldp__more-dtl__list--item")
                    .filter(new Locator.FilterOptions().setHasText("Age of Construction"));

            if (ageRow.count() == 0) {
                return -1; // not listed on this property
            }

            String ageValue = ageRow.first()
                    .locator(".mb-ldp__more-dtl__list--value")
                    .textContent()
                    .trim();

            return parseAgeValue(ageValue);

        } catch (Exception e) {
            log.warn("Could not fetch age from detail page {}: {}", detailUrl, e.getMessage());
            return -1;
        } finally {
            if (detailPage != null) {
                detailPage.close();
            }
        }
    }

    private int parseAgeValue(String value) {
        if (value == null || value.isBlank()) return -1;

        Matcher lt = AGE_LT.matcher(value);
        if (lt.find()) return Integer.parseInt(lt.group(1));

        Matcher range = AGE_RANGE.matcher(value);
        if (range.find()) return Integer.parseInt(range.group(2)); // upper bound, conservative for your <5yr filter

        Matcher single = AGE_SINGLE.matcher(value);
        if (single.find()) return Integer.parseInt(single.group(1));

        if (AGE_NEW.matcher(value).find()) return 0;

        return -1;
    }
}