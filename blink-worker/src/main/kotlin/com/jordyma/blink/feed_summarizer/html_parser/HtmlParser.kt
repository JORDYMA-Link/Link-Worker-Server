package com.jordyma.blink.feed_summarizer.html_parser

import com.jordyma.blink.logger
import io.github.bonigarcia.wdm.WebDriverManager
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.safety.Safelist
import org.openqa.selenium.By
import org.openqa.selenium.NoSuchFrameException
import org.openqa.selenium.WebDriver
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.ExpectedConditions.frameToBeAvailableAndSwitchToIt
import org.openqa.selenium.support.ui.WebDriverWait
import org.springframework.stereotype.Service
import java.time.Duration

@Service
class HtmlParser {

    fun fetchHtmlContent(url: String): ParseContent {
        //val document: Document = ajaxTest(url).let { Jsoup.parse(it) }
        val fetchedContent = fetchDynamicContent(url)
        val document: Document = fetchedContent.content.let { Jsoup.parse(it) }
        var content = if (document.select("body").html() != "") document.select("body").html() else document.html()
        content = cleanHtml(content)

        return ParseContent(content, fetchedContent.thumbnailImage)
    }


    fun fetchTitle(url: String): String {
        val document = Jsoup.connect(url).get()
        return document.title()  // HTML에서 <title> 태그의 내용을 가져옴
    }


    // 동적 컨텐츠를 가져오기 위해 Selenium WebDriver를 사용
    private fun fetchDynamicContent(url: String): ParseContent {
        var content = ""
        var thumbnailImage = ""

        // Set up Selenium WebDriver (ChromeDriver in this case)
        // WebDriverManager.chromedriver().clearDriverCache().setup()
        // TODO 브라우저 버전 하드코딩 말고 환경변수같은걸로 빼야 관리에 용이할것 같아요
        WebDriverManager.chromedriver().clearDriverCache().browserVersion("127.0.6533.72").setup()

        val options = ChromeOptions()
        options.addArguments(
            "--lang=ko",
            "--headless",
            "--remote-allow-origins=*",
            "--disable-setuid-sandbox",
            "--disable-gpu",
            "--no-sandbox",
            "--disable-dev-shm-usage",
            "--disable-popup-blocking",
            "--blink-settings=imagesEnabled=false"
        )
        var driver: WebDriver = ChromeDriver(options)

        try {
            // Load the page and wait for JavaScript execution
            driver.get(url)
            // val wait = WebDriverWait(driver, Duration.ofSeconds(40)) //페이지 불러오는 여유시간
            logger().info("++++++++++++++++++++++===================+++++++++++++selenium: " + driver.title)

            // Get the rendered HTML
            try {
                // iframe이 존재할 때까지 대기하고, 해당 프레임으로 전환
                frameToBeAvailableAndSwitchToIt("mainFrame")
            } catch (e: NoSuchFrameException) {
                logger().info("mainFrame iframe을 찾을 수 없습니다. 기본 콘텐츠에서 계속 진행합니다.")
                driver.switchTo().defaultContent()
            }

            // 페이지 내용 가져오기
            content = when {
                isElementLoaded(driver, By.tagName("body")) && driver.findElement(By.tagName("body")).text.isNotEmpty() -> {
                    driver.findElement(By.tagName("body")).text
                }
                isElementLoaded(driver, By.id("content-area")) && driver.findElement(By.id("content-area")).text.isNotEmpty() -> {
                    driver.findElement(By.id("content-area")).text
                }
                else -> driver.pageSource
            }
            logger().info("++++++++++++++++++++++===================+++++++++++++content: " + content)


            // 메타데이터 추출
            val document = Jsoup.parse(driver.pageSource)
            val metaTags = document.select("meta")

            // 썸네일 (og:image)
            thumbnailImage = metaTags.firstOrNull { it.attr("property") == "og:image" }
                ?.attr("content").toString()
            logger().info("Thumbnail: $thumbnailImage")

        } finally {
            // Close the browser
            driver.quit()
        }

        return ParseContent(content, thumbnailImage)
    }

    private fun cleanHtml(html: String): String {
        // 기본적으로 텍스트 포맷팅에 필요한 몇 가지 태그만 허용해서 html 정제
        return Jsoup.clean(html, Safelist.relaxed())
    }

    private fun isElementLoaded(driver: WebDriver, locator: By): Boolean {
        return try {
            val wait = WebDriverWait(driver, Duration.ofSeconds(20))
            wait.until(ExpectedConditions.presenceOfElementLocated(locator))
            wait.until(ExpectedConditions.visibilityOfElementLocated(locator))
            true
        } catch (e: Exception) {
            logger().info("Element not loaded: $locator")
            false
        }
    }
}