package com.example.base;

import com.example.utils.ConfigManager;
import io.appium.java_client.MobileElement;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.service.local.AppiumDriverLocalService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.remote.DesiredCapabilities;

import java.net.MalformedURLException;
import java.net.URL;


/**
 * Selenium webDriver manager
 *
 * @author Author Name
 */
public class MobileDriverManager {

    private static final Logger LOGGER = LogManager.getLogger(MobileDriverManager.class);
    private static final ThreadLocal<AndroidDriver<MobileElement>> WEB_DRIVER_THREAD_LOCAL = new ThreadLocal<>();
    public static URL appiumUrl;
    private static AppiumDriverLocalService appiumDriverLocalService;


    private MobileDriverManager() {
    }

    public static AndroidDriver<MobileElement> getDriver() {
        return WEB_DRIVER_THREAD_LOCAL.get();
    }

    private static void setDriver(AndroidDriver<MobileElement> androidDriver) {
        WEB_DRIVER_THREAD_LOCAL.set(androidDriver);
    }

    public static void quitDriver() {
        if (WEB_DRIVER_THREAD_LOCAL.get() != null) {
            WEB_DRIVER_THREAD_LOCAL.get().quit();
        }
    }

    public static void initDriver(DesiredCapabilities userProvidedCapabilities) {
        try {
            DesiredCapabilities desiredCapabilities = new DesiredCapabilities();
            if (userProvidedCapabilities != null) {
                LOGGER.info("Merging user provided selenium capabilities");
                desiredCapabilities.merge(userProvidedCapabilities);
                LOGGER.info(desiredCapabilities);
            } else {
                LOGGER.info("User provided capability is null. Ignoring...");
            }
            AndroidDriver<MobileElement> androidDriver = new AndroidDriver<>(appiumUrl, desiredCapabilities);
            MobileDriverManager.setDriver(androidDriver);
            LOGGER.info("Android Driver successfully initialized. Session id : [{}]", androidDriver.getSessionId());
        } catch (Exception e) {
            LOGGER.error(e);
            throw new RuntimeException(e);
        }
    }

    public static void startAppium() {
        appiumDriverLocalService = AppiumDriverLocalService.buildDefaultService();
        appiumDriverLocalService.start();
    }

    public static void stopAppium() {
        appiumDriverLocalService.stop();
    }

    private static class AppiumConfig {

        private static final Boolean useDefaultAppiumSettings = Boolean
                .parseBoolean(ConfigManager.getConfigProperty("appium.set.default"));

        public URL getAppiumUrl(URL defaultUrl) throws MalformedURLException {
            return !useDefaultAppiumSettings ? new URL(ConfigManager.getConfigProperty("appium.url")) : defaultUrl;
        }

        public String getAppiumMainFilePath() {
            return !useDefaultAppiumSettings ? ConfigManager.getConfigProperty("appium.main.file.path") : System.getProperty("user.home")
                    .concat("/AppData/Local/Programs/Appium/resources/app/node_modules/appium/build/lib/main.js");
        }

        public Boolean isEnableAppiumLogs() {
            return Boolean.parseBoolean(ConfigManager.getConfigProperty("enable.appium.logs"));
        }
    }
}
