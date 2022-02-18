import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.json.JSON;
import cn.hutool.json.JSONUtil;
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

public class SpiderLianJia {
    private static final Log log = LogFactory.get();
    private static ExecutorService executor = ThreadUtil.newExecutor(5);

    public static void main(String[] args) throws IOException, SQLException {
        spider();
    }

    public static void spider() throws IOException, SQLException {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        log.error("开始爬取 " + sdf.format(new Date()) + " 日数据");

        String url = "https://wx.lianjia.com/ershoufang/binhu/a3/";
        Document doc = SpiderConfig.getDocument(url);
        int lastPageNum = getLastPageNum(doc);

        for (int i = 1; i <= lastPageNum; i++) {
            int finalI = i;
            executor.execute(() -> {
                try {
                    soupData(finalI);
                } catch (IOException | SQLException e) {
                    e.printStackTrace();
                }
            });
        }

    }

    private static void soupData(int pageNum) throws IOException, SQLException {
        log.error("开始爬取第 " + pageNum + " 页");
        String url = "https://wx.lianjia.com/ershoufang/binhu/pg" + pageNum + "a3";
        Document doc = SpiderConfig.getDocument(url);

        Elements select = doc.select(".sellListContent .info");
        for (int i = 1; i <= select.size(); i++) {
            Element info = select.get(i - 1);

            Element titleElement = info.select(".title").get(0).children().get(0);
            String titleText = titleElement.text();
            String href = titleElement.attr("href");
            String houseId = href.replace("https://wx.lianjia.com/ershoufang/", "").replace(".html", "");
            log.info(titleText + " " + href + " " + houseId);


            Elements floodElements = info.select(".flood .positionInfo").get(0).children();
            String communityName = floodElements.get(2).text();//小区名称
            String addressDetails = floodElements.get(2).text();//详细地址
            log.info(communityName + " " + addressDetails);

            String tag = info.select(".address .houseInfo").text();
            String[] tagSplice = tag.split("\\|");
            String direction = tagSplice[2].trim();//朝向
            String decoration = tagSplice[3].trim();//装修情况
            String floor = tagSplice[4].trim();//楼层
            String structure = tagSplice[0].trim();//几室几厅
            log.info(direction + " " + decoration + " " + floor + " " + structure);

            DecimalFormat df = new DecimalFormat("0.00");
            String priceText = info.select(".priceInfo .totalPrice").text();
            String price = df.format(Double.valueOf(priceText.replace("万","")));
            log.info(price);//价格

            String price_single = info.select(".priceInfo .unitPrice").text().replace(",", "").replace("元/平", "");
            log.info(price_single);//单价

            Double area = Double.valueOf(tagSplice[1].replace("平米", ""));

            CreateCmd cmd = new CreateCmd();
            cmd.setTitleText(titleText);
            cmd.setHref(href);
            cmd.setHouseId(houseId);
            cmd.setCommunityName(communityName);
            cmd.setAddressDetails(addressDetails);
            cmd.setDirection(direction);
            cmd.setDecoration(decoration);
            cmd.setFloor(floor);
            cmd.setStructure(structure);
            cmd.setArea(df.format(area));
            cmd.setPrice(price);
            cmd.setPrice_single(price_single);
            cmd.setSource("LianJia");

            SpiderDB.saveToDB(titleText, houseId, df, Double.valueOf(price), cmd);
        }



    }

    private static int getLastPageNum(Document doc) {
        Elements lastPageElement = doc.select(".page-box .house-lst-page-box");
        String attr = lastPageElement.get(0).attr("page-data");
        JSON parse = JSONUtil.parse(attr);
        int lastPage = (int)parse.getByPath("totalPage");

        log.info(String.valueOf(lastPage));
        return lastPage;
    }
}
