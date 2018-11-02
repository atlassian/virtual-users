package com.atlassian.performance.tools.virtualusers

import com.atlassian.performance.tools.virtualusers.api.browsers.HeadlessChromeBrowser
import org.openqa.selenium.chrome.ChromeDriverService
import org.openqa.selenium.chrome.ChromeOptions

@Deprecated(
    message = "Do not use."
)
/**
 * I know it's not an API and it doesn't have to be deprecated, but deprecation makes it more visible and increases
 * chances that we'll remove it in the next MAJOR release.
 */
internal class GoogleChromeWithInsecureConnectionSupport : HeadlessChromeBrowser() {
    override fun configure(options: ChromeOptions, service: ChromeDriverService.Builder) {
        options.addArguments("--ignore-certificate-errors")
        super.configure(options, service)
    }
}