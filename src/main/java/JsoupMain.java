import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.cron.CronUtil;
import cn.hutool.cron.task.Task;
import cn.hutool.db.Db;
import cn.hutool.db.Entity;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutorService;

/**
 * 爬取 efw 70-90 平米的二手房数据 https://www.efw.cn/Sale/-p1-r70-s90
 * 如果售价发生变化则保存多个版本以分析价格趋势
 *
 * @author Chengloong
 * @Date 2020-11-27
 */
public class JsoupMain {

	private static final Log log = LogFactory.get();

	private static ExecutorService executor = ThreadUtil.newExecutor(5);

	public static void main(String[] args) {
		//每日凌晨 2 点执行
		CronUtil.schedule("0 0 2 * * ?", (Task) () -> {
			try {
				spider();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		});

		//每小时查询一次数据库，防止数据库链接释放导致程序出错
		CronUtil.schedule("0 0 0/1 * * ?", (Task) () -> {
			try {
				Db.use().query("select 1");
			} catch (SQLException e) {
				e.printStackTrace();
			}
		});

		CronUtil.setMatchSecond(true);
		CronUtil.start();
	}

	/**
	 * 爬虫主程序
	 */
	public static void spider() throws IOException, SQLException {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		log.error("开始爬取 " + sdf.format(new Date()) + " 日数据");

		String url = "https://www.efw.cn/Sale/-p1-r70-s90";
		Document doc = getDocument(url);
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
		Document doc = getDocument(url);

		ArrayList<Map<String, Object>> rows = new ArrayList<>();

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

			String price_single = element.select(".text4.f14").text().replace("单价", "").replace(" 元/平", "");
			log.info(price_single);//单价

			DecimalFormat df = new DecimalFormat("0.00");
			double priceDouble = Double.parseDouble(price);
			Double area = priceDouble * 10000 / Double.parseDouble(price_single);

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
			cmd.setNearBackground(nearBackground);
			cmd.setPrice(df.format(priceDouble));
			cmd.setPrice_single(price_single);

			saveToDB(titleText, houseId, df, priceDouble, cmd);
		}

	}

	/**
	 * 保存入库
	 */
	private static void saveToDB(String titleText, String houseId, DecimalFormat df, double priceDouble, CreateCmd cmd) throws SQLException {
		//查询数据库是否存在数据，如果没有就 insert
		List<Entity> entityList = Db.use().query("select * from house where house_id = ? order by create_date desc limit 1", houseId);
		if (entityList.size() > 0) {
			Entity entity = entityList.get(0);
			String old_price = entity.get("total_price").toString();
			//如果价格发生变化，保存新的记录
			if (!StrUtil.equals(old_price, df.format(priceDouble))) {
				log.error(titleText + " 价格发生了变化");
				int version = Integer.parseInt(entity.get("version") + "") + 1;
				cmd.setVersion(version);
				insertInDB(cmd);
			}
		} else {
			cmd.setVersion(1);
			insertInDB(cmd);
		}
	}

	/**
	 * 保存数据到数据库
	 */
	private static void insertInDB(CreateCmd cmd) throws SQLException {
		Db.use().insert(
				Entity.create("house")
						.set("title_text", cmd.getTitleText())
						.set("house_id", cmd.getHouseId())
						.set("url_path", "https://www.efw.cn/" + cmd.getHref())
						.set("community_name", cmd.getCommunityName())
						.set("address_details", cmd.getAddressDetails())
						.set("direction", cmd.getDirection())
						.set("decoration", cmd.getDecoration())
						.set("floor", cmd.getFloor())
						.set("structure", cmd.getStructure())
						.set("area", cmd.getArea().replace("平方", ""))
						.set("nearBackground", cmd.getNearBackground())
						.set("total_price", cmd.getPrice())
						.set("price_single", cmd.getPrice_single())
						.set("create_date", new Date())
						.set("version", cmd.getVersion())
		);
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

	private static Document getDocument(String url) throws IOException {
		return Jsoup.connect(url)
				.header("Accept", "*/*")
				.header("Accept-Encoding", "gzip, deflate")
				.header("Accept-Language", "zh-CN,zh;q=0.8,en-US;q=0.5,en;q=0.3")
				.header("Referer", "https://www.baidu.com/")
				.header("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:48.0) Gecko/20100101 Firefox/48.0")
				.timeout(5000)
				.get();
	}
}
