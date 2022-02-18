import cn.hutool.core.util.StrUtil;
import cn.hutool.db.Db;
import cn.hutool.db.Entity;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;

import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.Date;
import java.util.List;

public class SpiderDB {
    private static final Log log = LogFactory.get();
    /**
     * 保存入库
     */
    public static void saveToDB(String titleText, String houseId, DecimalFormat df, double priceDouble, CreateCmd cmd) throws SQLException {
        //查询数据库是否存在数据，如果没有就 insert
        List<Entity> entityList = Db.use().query("select * from house where house_id = ? order by create_date desc limit 1", houseId);
        if (entityList.size() > 0) {
            Entity entity = entityList.get(0);
            String old_price = entity.get("total_price").toString();
            //如果价格发生变化，保存新的记录
            if (!StrUtil.equals(old_price, df.format(priceDouble))) {
                log.error(titleText + " 价格发生了变化");
                insertInDB(cmd,"house_histroy"); //存储到历史记录

                int version = Integer.parseInt(entity.get("version") + "") + 1;
                cmd.setVersion(version);
                updateDB(cmd,"house", (int)entity.get("id"));
            }
        } else {
            cmd.setVersion(1);
            insertInDB(cmd, "house");
        }
    }

    /**
     * 保存数据到数据库
     */
    private static void insertInDB(CreateCmd cmd, String table) throws SQLException {
        Db.use().insert(
                Entity.create(table)
                        .set("title_text", cmd.getTitleText())
                        .set("house_id", cmd.getHouseId())
                        .set("url_path", cmd.getHref())
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
                        .set("source", cmd.getSource())
        );
    }

    private static void updateDB(CreateCmd cmd, String table, int id) throws SQLException {
        Db.use().update(
                Entity.create()
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
                        .set("source", cmd.getSource()),
        Entity.create(table).set("id", id) //where条件
        );
    }

}
