import cn.hutool.core.util.StrUtil;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import cn.hutool.poi.excel.BigExcelWriter;
import cn.hutool.poi.excel.ExcelUtil;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

public class JsoupMain {

	private static final Log log = LogFactory.get();

	public static void main(String[] args) throws IOException, SQLException {
		jsoup();
	}

	public static void jsoup() throws IOException, SQLException {
		String url = "https://www.efw.cn/Sale/-p1-r70-s90";
		Document doc = Jsoup.connect(url)
				.header("Accept", "*/*")
				.header("Accept-Encoding", "gzip, deflate")
				.header("Accept-Language", "zh-CN,zh;q=0.8,en-US;q=0.5,en;q=0.3")
				.header("Referer", "https://www.baidu.com/")
				.header("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:48.0) Gecko/20100101 Firefox/48.0")
				.timeout(5000)
				.get();
		int lastPageNum = getLastPageNum(doc);

		BigExcelWriter writer = ExcelUtil.getBigWriter("WuxiEfwHouseReport.xlsx");
		writer.merge(11, "无锡e房网二手房70-90平米");

		for (int i = 1; i <= lastPageNum; i++) {
			log.error("开始爬取第 " + i + " 页");
			soupData(i, writer);
		}

		writer.close();
	}

	private static void soupData(int pageNum, BigExcelWriter writer) throws IOException, SQLException {
		String url = "https://www.efw.cn/Sale/-p" + pageNum + "-r70-s90";
		Document doc = Jsoup.connect(url)
				.header("Accept", "*/*")
				.header("Accept-Encoding", "gzip, deflate")
				.header("Accept-Language", "zh-CN,zh;q=0.8,en-US;q=0.5,en;q=0.3")
				.header("Referer", "https://www.baidu.com/")
				.header("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:48.0) Gecko/20100101 Firefox/48.0")
				.timeout(5000)
				.get();

		ArrayList<Map<String, Object>> rows = new ArrayList<>();

		Elements select = doc.select(".box12 li");
		for (int i = 1; i <= select.size(); i++) {
			Element element = select.get(i - 1);

			Elements titleElements = element.select(".mb10 a");
			String titleText = titleElements.text();
			String href = titleElements.attr("href");
			log.info(titleText + " " + href);

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

			String price_single = element.select(".text4.f14").text().replace("单价", "").replace(" 元/平", "");
			log.info(price_single);//单价

			Map<String, Object> row = new LinkedHashMap<>();
			row.put("序号", (pageNum - 1) * 20 + i);
			row.put("标题", titleText);
			row.put("URL地址", "https://www.efw.cn/" + href);
			row.put("小区名称", communityName);
			row.put("详细地址", addressDetails);
			row.put("朝向", direction);
			row.put("装修情况", decoration);
			row.put("楼层", floor);
			row.put("几室几厅", structure);
			row.put("是否近地铁", nearBackground);
			row.put("价格", price);
			row.put("单价", price_single);

			rows.add(row);
		}

		Boolean isWriteKeyAsHead = pageNum == 1;
		writer.write(rows, isWriteKeyAsHead);
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
