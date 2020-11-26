import cn.hutool.core.util.StrUtil;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.sql.SQLException;

public class JsoupMain {

	private static final Log log = LogFactory.get();

	public static void main(String[] args) throws IOException, SQLException {
		jsoup();
//		CronUtil.schedule("0 0/5 * * * ?", (Task) () -> {
//			try {
//				jsoup();
//			} catch (IOException e) {
//				e.printStackTrace();
//			} catch (SQLException e) {
//				e.printStackTrace();
//			}
//		});
//
//		CronUtil.setMatchSecond(true);
//		CronUtil.start();
	}

	public static void jsoup() throws IOException, SQLException {
		for (int i = 1; i < 2; i++) {
			soupData(i);
		}
	}

	private static void soupData(int pageNum) throws IOException, SQLException {
		String url = "https://www.efw.cn/Sale/-p" + pageNum + "-r70-s90";
		Document doc = Jsoup.connect(url)
				.header("Accept", "*/*")
				.header("Accept-Encoding", "gzip, deflate")
				.header("Accept-Language", "zh-CN,zh;q=0.8,en-US;q=0.5,en;q=0.3")
				.header("Referer", "https://www.baidu.com/")
				.header("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:48.0) Gecko/20100101 Firefox/48.0")
				.timeout(5000)
				.get();

		Elements select = doc.select(".box12 li");

		for (Element element : select) {
			String titleText = element.select(".mb10").text();
			log.info(titleText);

			String address = element.select(".box12tb1").text();
			String[] addressSplice = address.split("\\|");
			String communityName = addressSplice[0].trim();//小区名称
			String addressDetails = addressSplice[1].trim();

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
				if (StrUtil.contains(text,"地铁")) {
					nearBackground = "近地铁";
				}
			}
			log.info(nearBackground);

			String price = element.select(".price1.f30").text();
			log.info(price);//价格

			String price_single = element.select(".text4.f14").text().replace("单价","").replace(" 元/平", "");
			log.info(price_single);


			System.out.println();
//			Db.use().insert(
//					Entity.create("jsoup")
//							.set("url_path", element.toString())
//			);
		}
	}
}
