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
 * 爬取 lianjia 70-90 平米的二手房数据 https://wx.lianjia.com/ershoufang/pg1a3/
 * 如果售价发生变化则保存多个版本以分析价格趋势
 *
 * @author Chengloong
 * @Date 2020-11-27
 */
public class JsoupMain {

	public static void main(String[] args) {


		//每日凌晨 2 点执行 SpiderEfw
		CronUtil.schedule("0 0 2 * * ?", (Task) () -> {
			try {
				SpiderEfw.spider();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		});

		//每日凌晨 2:10 点执行 SpiderLianJia
		CronUtil.schedule("0 10 2 * * ?", (Task) () -> {
			try {
				SpiderLianJia.spider();
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

}
