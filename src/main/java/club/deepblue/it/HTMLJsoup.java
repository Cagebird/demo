package club.deepblue.it;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.parser.Tag;
import org.jsoup.select.Elements;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.logging.SimpleFormatter;

public class HTMLJsoup {

    static class AppInfo {
        private String pkgName;
        private String appName;
        private String appDesc;
        private String appSize;
        private String appVersion;
        private String appDeveloper;
        private String releaseTime;
        private String imgUrl;

        public String getPkgName() {
            return pkgName;
        }

        public void setPkgName(String pkgName) {
            this.pkgName = pkgName;
        }

        public String getAppName() {
            return appName;
        }

        public void setAppName(String appName) {
            this.appName = appName;
        }

        public String getAppDesc() {
            return appDesc;
        }

        public void setAppDesc(String appDesc) {
            this.appDesc = appDesc;
        }

        public String getAppVersion() {
            return appVersion;
        }

        public void setAppVersion(String appVersion) {
            this.appVersion = appVersion;
        }

        public String getAppDeveloper() {
            return appDeveloper;
        }

        public void setAppDeveloper(String appDeveloper) {
            this.appDeveloper = appDeveloper;
        }

        public String getReleaseTime() {
            return releaseTime;
        }

        public void setReleaseTime(String releaseTime) {
            this.releaseTime = releaseTime;
        }

        public String getAppSize() {
            return appSize;
        }

        public void setAppSize(String appSize) {
            this.appSize = appSize;
        }

        public String getImgUrl() {
            return imgUrl;
        }

        public void setImgUrl(String imgUrl) {
            this.imgUrl = imgUrl;
        }


        @Override
        public String toString() {
            return "AppInfo{" +
                    "pkgName='" + pkgName + '\'' +
                    ", appName='" + appName + '\'' +
                    ", appDesc='" + appDesc + '\'' +
                    ", appSize='" + appSize + '\'' +
                    ", appVersion='" + appVersion + '\'' +
                    ", appDeveloper='" + appDeveloper + '\'' +
                    ", releaseTime='" + releaseTime + '\'' +
                    ", imgUrl='" + imgUrl + '\'' +
                    '}';
        }
    }

    public static void main(String[] args) throws IOException {
        List<AppInfo> appInfoList = new ArrayList<>(5000);
        for (int i = 0; i < 20; i++) {
            appInfoList.addAll(getBatchAppInfo(i));
        }
        JSON json = (JSON) JSONObject.toJSON(appInfoList);
        File file = new File("appinfo.json");
        if (!file.exists()) {
            file.createNewFile();
        }
        OutputStream outputStream = new FileOutputStream(file);
        OutputStream bufferedOutputStream = new BufferedOutputStream(outputStream);
        bufferedOutputStream.write(json.toString().getBytes());
        bufferedOutputStream.close();
    }

    public static List<AppInfo> getBatchAppInfo(Integer pageSize) throws IOException {
        List<AppInfo> result = new ArrayList<>();
        Document document = Jsoup.connect("https://www.coolapk.com/apk?p=" + pageSize).get();
        Element appLeftList = document.getElementsByClass("app_left_list").get(0);
        for (Node node : appLeftList.childNodes()) {
            if (node instanceof Element) {
                Element e = (Element) node;
                if (e.tag().equals(Tag.valueOf("a"))) {
                    String href = e.attr("href");
                    String pkgName = href.replaceAll("/apk/", "");
                    AppInfo appInfo = getAppInfo(pkgName);
                    result.add(appInfo);
                }
            }
        }
        return result;
    }

    public static AppInfo getAppInfo(String pkgName) throws IOException {
        AppInfo appInfo = new AppInfo();
        Document document = Jsoup.connect("https://www.coolapk.com/apk/" + pkgName).get();
        Element appLeft = document.getElementsByClass("app_left").get(0);

        Element apkLeftOne = appLeft.getElementsByClass("apk_left_one").get(0);

        Element apkTopbar = apkLeftOne.getElementsByClass("apk_topbar").get(0);

        Elements detailAppTitle = apkTopbar.getElementsByClass("detail_app_title");
        List<Node> nodes = detailAppTitle.get(0).childNodes();
        appInfo.setAppName(nodes.get(0).toString().trim());
        appInfo.setAppVersion(nodes.get(1).childNode(0).toString().trim());
        Element apkTopbaMessage = apkTopbar.getElementsByClass("apk_topba_message").get(0);
        appInfo.setAppSize(apkTopbaMessage.text().split("/")[0].trim());

        Element imgElement = apkTopbar.getElementsByTag("img").get(0);
        appInfo.setImgUrl(imgElement.attr("src").trim());

        Element apkLeftTwo = appLeft.getElementsByClass("apk_left_two").get(0);

        Elements apkLeftTitle = apkLeftTwo.getElementsByClass("apk_left_title");
        Element descElement = null;
        Element detailsElement = null;
        for (Element element : apkLeftTitle) {
            String title = element.getElementsByTag("p").get(0).text();
            if ("应用简介".equals(title)) {
                descElement = element.getElementsByClass("apk_left_title_info").get(0);
            }
            if ("详细信息".equals(title)) {
                detailsElement = element.getElementsByClass("apk_left_title_info").get(0).getElementsByTag("p").get(0);
            }
        }
        assert descElement != null;
        String desc = transformTimeDesc(descElement);

        appInfo.setAppDesc(desc);
        assert detailsElement != null;

        for (Node detail : detailsElement.childNodes()) {
            String[] split = detail.toString().split("：");
            switch (split[0].trim()) {
                case "应用包名": {
                    appInfo.setPkgName(split[1].trim());
                    break;
                }
                case "更新时间": {
                    appInfo.setReleaseTime(transformTime(split[1].trim()));
                    break;
                }
                case "开发者名称": {
                    appInfo.setAppDeveloper(split[1].trim());
                    break;
                }
                default:
            }

        }
        return appInfo;
    }

    private static String transformTimeDesc(Element descElement) {
        if (descElement.tag().equals(Tag.valueOf("br"))) {
            return "\n";
        }
        StringBuilder descBuilder = new StringBuilder();
        for (Node childNode : descElement.childNodes()) {
            if (childNode instanceof TextNode) {
                String text = childNode.toString();
                descBuilder.append(text);
            }
            if (childNode instanceof Element) {
                descBuilder.append(transformTimeDesc((Element) childNode));
            }
        }

        return descBuilder.toString();
    }

    private static String transformTime(String s) {
        int i = s.lastIndexOf("个星期前");
        Calendar calendar = Calendar.getInstance();

        SimpleDateFormat sf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        int amount = -1 * Integer.parseInt("" + s.charAt(0));
        if (i > 0) {
            calendar.add(Calendar.WEEK_OF_MONTH, amount);
            return sf.format(calendar.getTime());
        }

        i = s.lastIndexOf("天前");
        if (i > 0) {
            calendar.add(Calendar.DAY_OF_MONTH, amount);
            return sf.format(calendar.getTime());
        }
        return s;
    }
}
