import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ExecutorService;

public class SpiderEfw {
    private static final Log log = LogFactory.get();
    private static ExecutorService executor = ThreadUtil.newExecutor(5);

    /**
     * 爬虫主程序
     */
    public static void spider() throws IOException, SQLException {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        log.error("开始爬取 " + sdf.format(new Date()) + " 日数据");

        String url = "https://www.efw.cn/Sale/-p1-r70-s90";
        Document doc = SpiderConfig.getDocument(url);
        int lastPageNum = getLastPageNum(doc);

        for (int i = 1; i <= lastPageNum; i++) {
            int finalI = i;
            executor.execute(() -> {
                try {
                    soupData(finalI);
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            });
        }
    }

    private static void soupData(int pageNum) throws IOException, SQLException {
        log.error("开始爬取第 " + pageNum + " 页");
        String url = "https://www.efw.cn/Sale/-p" + pageNum + "-r70-s90";
        Document doc = SpiderConfig.getDocument(url);

        Elements select = doc.select(".box12 li");
        for (int i = 1; i <= select.size(); i++) {
            Element element = select.get(i - 1);

            Elements titleElements = element.select(".mb10 a");
            String titleText = titleElements.text();
            String href = titleElements.attr("href");
            String houseId = href.replace("/Sale", "").replace(".html", "");
            log.info(titleText + " " + href + " " + houseId);

            String address = element.select(".box12tb1").text();
            String[] addressSplice = address.split("\\|");
            String communityName = addressSplice[0].trim();//小区名称
            String addressDetails = addressSplice[1].trim();//详细地址
            log.info(communityName + " " + addressDetails);

            String tag = element.select(".box12tb2").text();
            String[] tagSplice = tag.split("\\|");
            String direction = tagSplice[0].trim();//朝向
            String decoration = tagSplice[1].trim();//装修情况
            String floor = tagSplice[2].trim();//楼层
            String structure = tagSplice[3].trim();//几室几厅
            log.info(direction + " " + decoration + " " + floor + " " + structure);

            String nearBackground = "";//是否近地铁
            for (Element child : element.select(".text5").get(0).children()) {
                String text = child.text();
                if (StrUtil.contains(text, "地铁")) {
                    nearBackground = "近地铁";
                }
            }
            log.info(nearBackground);

            String price = element.select(".price1.f30").text();
            log.info(price);//价格

            String price_single = element.select(".text4.f14").text().replace("单价", "").replace("元/平", "");
            log.info(price_single);//单价

            DecimalFormat df = new DecimalFormat("0.00");
            double priceDouble = Double.parseDouble(price);
            Double area = priceDouble * 10000 / Double.parseDouble(price_single);

            CreateCmd cmd = new CreateCmd();
            cmd.setTitleText(titleText);
            cmd.setHref("https://www.efw.cn" + href);
            cmd.setHouseId(houseId);
            cmd.setCommunityName(communityName);
            cmd.setAddressDetails(addressDetails);
            cmd.setDirection(direction);
            cmd.setDecoration(decoration);
            cmd.setFloor(floor);
            cmd.setStructure(structure);
            cmd.setArea(df.format(area));
            cmd.setNearBackground(nearBackground);
            cmd.setPrice(df.format(priceDouble));
            cmd.setPrice_single(price_single);
            cmd.setSource("Efw");

            SpiderDB.saveToDB(titleText, houseId, df, priceDouble, cmd);
        }

    }

    /**
     * 获取最后一页
     */
    private static int getLastPageNum(Document doc) {
        Elements lastPageElement = doc.select(".msdn a:last-child");
        String title = lastPageElement.attr("title");
        String lastPage = title.replace("第", "").replace("页", "");
        log.info(lastPage);
        return Integer.parseInt(lastPage);
    }

}
