package red.man10.man10drugplugin;


import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.logging.Level;

/**
 * Created by Mr_IK on 2019/04/29.
 */

public class MySQLManagerV2 {

    private boolean connected = false;
    private String conName;
    public  Boolean debugMode = false;
    private JavaPlugin plugin;
    private String HOST = null;
    private String DB = null;
    private String USER = null;
    private String PASS = null;
    private String PORT = null;

    private HashMap<Integer, MySQLConnectV2> connects;

    ////////////////////////////////
    //      コンストラクタ
    ////////////////////////////////
    public MySQLManagerV2(JavaPlugin plugin, String name) {
        this.connects = new HashMap<>();
        this.plugin = plugin;
        this.conName = name;
        this.connected = false;
        loadConfig();

        int result = Connect(HOST, DB, USER, PASS, PORT);

        if(result == -1){
            plugin.getLogger().info("Unable to establish a MySQL connection.");
        }

        //テーブル作成などはここ

        //drug table
        execute("CREATE TABLE if not exists drug_dependence " +
                "(uuid text," +
                "player text," +
                "drug_name text," +
                "used_count int," +
                "used_level int," +
                "used_time text," +
                "level int," +
                "immunity int," +
                "symptoms_total int);");

        //logger table
        execute("CREATE TABLE if not exists log " +
                "(uuid text, " +
                "player text, " +
                "drug_name text, " +
                "date text);");

        //drug box
        execute("CREATE TABLE if not exists box " +
                "(id int,"  +
                "one text,"     +
                "two text,"     +
                "three text,"   +
                "four text,"    +
                "five text,"    +
                "six text,"     +
                "seven text,"   +
                "eight text,"   +
                "nine text);");

    }


    /////////////////////////////////
    //       設定ファイル読み込み
    /////////////////////////////////
    public void loadConfig(){
        plugin.getLogger().info("MySQL Config loading");
        plugin.reloadConfig();
        HOST = plugin.getConfig().getString("mysql.host");
        USER = plugin.getConfig().getString("mysql.user");
        PASS = plugin.getConfig().getString("mysql.pass");
        PORT = plugin.getConfig().getString("mysql.port");
        DB = plugin.getConfig().getString("mysql.db");
        plugin.getLogger().info("Config loaded");
    }


    ////////////////////////////////
    //  接続
    ////////////////////////////////
    public int Connect(String host, String db, String user, String pass,String port) {
        this.HOST = host;
        this.DB = db;
        this.USER = user;
        this.PASS = pass;
        int data = connects.size()+1;
        connects.put(connects.size()+1,new MySQLConnectV2(host, db, user, pass,port));
        if(connects.get(data).open() == null){
            Bukkit.getLogger().info("failed to open MYSQL");
            return -1;
        }
        this.plugin.getLogger().info("[" + this.conName + "] Connected to the database.");
        return data;
    }

    ////////////////////////////////
    //     行数を数える
    ////////////////////////////////
    public int countRows(String table) {
        int count = 0;
        ResultSet set = this.query(String.format("SELECT * FROM %s", new Object[]{table})).rs;

        try {
            while(set.next()) {
                ++count;
            }
        } catch (SQLException var5) {
            Bukkit.getLogger().log(Level.SEVERE, "Could not select all rows from table: " + table + ", error: " + var5.getErrorCode());
        }

        return count;
    }

    ////////////////////////////////
    //     レコード数
    ////////////////////////////////
    public int count(String table) {
        int count = 0;
        ResultSet set = this.query(String.format("SELECT count(*) from %s", table)).getRs();

        try {
            count = set.getInt("count(*)");

        } catch (SQLException var5) {
            Bukkit.getLogger().log(Level.SEVERE, "Could not select all rows from table: " + table + ", error: " + var5.getErrorCode());
            return -1;
        }

        return count;
    }

    ////////////////////////////////
    //      実行
    ////////////////////////////////
    public boolean execute(String query) {
        int data = connects.size()+1;
        connects.put(connects.size()+1,new MySQLConnectV2(this.HOST, this.DB, this.USER, this.PASS,this.PORT));
        if(connects.get(data).open() == null){
            Bukkit.getLogger().info("failed to open MYSQL");
            return false;
        }
        boolean ret = true;
        if (debugMode){
            plugin.getLogger().info("query:" + query);
        }

        try {
            connects.get(data).getSt().execute(query);
        } catch (SQLException var3) {
            this.plugin.getLogger().info("[" + this.conName + "] Error executing statement: " +var3.getErrorCode() +":"+ var3.getLocalizedMessage());
            this.plugin.getLogger().info(query);
            ret = false;
        }

        connects.get(data).close();
        return ret;
    }

    ////////////////////////////////
    //      クエリ
    ////////////////////////////////
    public Query query(String query) {
        int data = connects.size()+1;
        connects.put(connects.size()+1,new MySQLConnectV2(this.HOST, this.DB, this.USER, this.PASS,this.PORT));
        if(connects.get(data).open() == null){
            Bukkit.getLogger().info("failed to open MYSQL");
            return null;
        }
        ResultSet rs = null;
        if (debugMode){
            plugin.getLogger().info("query:" + query);
        }

        try {
            rs = connects.get(data).getSt().executeQuery(query);
        } catch (SQLException var4) {
            this.plugin.getLogger().info("[" + this.conName + "] Error executing query: " + var4.getErrorCode());
            this.plugin.getLogger().info(query);
        }

        //query.close();
        return new Query(rs,connects.get(data));
    }

    public int notClosedConnectionCount(){
        int count = 0;
        for(MySQLConnectV2 connect : connects.values()){
            if(!connect.isClosed()){
                count++;
            }
        }
        return count;
    }

    public void forceCloseAllConnection(){
        for(MySQLConnectV2 connect : connects.values()){
            if(!connect.isClosed()){
                connect.close();
            }
        }
    }

    public class Query {
        private ResultSet rs = null;
        private MySQLConnectV2 connect;

        public Query(ResultSet rs,MySQLConnectV2 connect){
            this.connect = connect;
            this.rs = rs;
        }

        public ResultSet getRs() {
            return rs;
        }

        public void close(){
            try {
                rs.close();
                connect.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }


}
