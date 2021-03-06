package net.syamn.voteban;

import java.io.IOException;
import java.util.HashMap;
import java.util.logging.Logger;

import net.syamn.utils.LogUtil;
import net.syamn.utils.Metrics;
import net.syamn.voteban.Commands.VoteCommand;
import net.syamn.voteban.Vote.Vote;
import net.syamn.voteban.VoteActions.BanHandler;

import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;

public class VoteBan extends JavaPlugin {
    // Logger
    public final static Logger log = Logger.getLogger("Minecraft");
    public final static String logPrefix = "[net.syamn.voteban] ";
    public final static String msgPrefix = "&c[net.syamn.voteban] &f";

    // Listener

    // Private classes
    private ConfigurationManager config;
    private BanHandler banHandler;

    // Public variable
    public HashMap<String, Vote> votes = new HashMap<String, Vote>();

    // Instance
    private static VoteBan instance;

    /**
     * プラグイン起動処理
     */
    public void onEnable() {
        instance = this;
        LogUtil.init(this);
        
        config = new ConfigurationManager(this);

        // 設定読み込み
        try {
            config.loadConfig(true);
        } catch (Exception ex) {
            log.warning(logPrefix + "an error occured while trying to load the config file.");
            ex.printStackTrace();
        }
        
        // コマンド登録
        getServer().getPluginCommand("vote").setExecutor(new VoteCommand(this));
        log.info(logPrefix + "Initialized Command.");

        // BANを行うプラグインの決定とハンドラ初期化
        banHandler = new BanHandler(this);
        boolean gban = config.isGlobalBan;
        switch (banHandler.setupBanHandler(this)) {
            case VANILLA:
                log.info(logPrefix + "Didn't Find ban plugin, using vanilla.");
                break;
            case MCBANS:
                log.info(logPrefix + "MCBans (version 3.8+) plugin found, using that.");
                if (gban)
                    log.info(logPrefix + "Enabled Global BAN!");
                else
                    log.info(logPrefix + "Disabled Global BAN. Using local type BAN.");
                break;
            case GLIZER:
                log.info(logPrefix + "glizer plugin found, using that.");
                if (gban) log.info(logPrefix + "Disabled Global BAN. Using local type BAN.");
                break;
            case EASYBAN:
                log.info(logPrefix + "EasyBan plugin found, using that.");
                break;
            case ULTRABAN:
                log.info(logPrefix + "UltraBan plugin found, using that.");
                break;
            case DYNBAN:
                log.info(logPrefix + "DynamicBan plugin found, using that.");
                break;
            default:
                log.warning(logPrefix + "Error occurred on setupBanHandler (Honeychest.class)");
                break;
        }

        // メッセージ表示
        PluginDescriptionFile pdfFile = this.getDescription();
        log.info("[" + pdfFile.getName() + "] version " + pdfFile.getVersion() + " is enabled!");
        
        // Metricsセットアップ
        setupMetrics();
    }

    /**
     * プラグイン停止処理
     */
    public void onDisable() {
        // メッセージ表示
        PluginDescriptionFile pdfFile = this.getDescription();
        log.info("[" + pdfFile.getName() + "] version " + pdfFile.getVersion() + " is disabled!");
    }

    /**
     * Metricsセットアップ
     */
    public void setupMetrics() {
        try {
            Metrics metrics = new Metrics(this);
            metrics.start();
        } catch (IOException ex) {
            log.warning("cannot send metrics data!");
            ex.printStackTrace();
        }
    }

    /* getter */

    /**
     * 設定マネージャを返す
     * 
     * @return ConfigurationManager
     */
    public ConfigurationManager getConfigs() {
        return config;
    }

    /**
     * BANハンドラを返す
     * 
     * @return BanHandler
     */
    public BanHandler getBansHandler() {
        return banHandler;
    }

    /**
     * インスタンスを返す
     * 
     * @return VoteBanインスタンス
     */
    public static VoteBan getInstance() {
        return instance;
    }

    public void deflog(String line) {
        if (config.logToFileFlag && !config.logDetailFlag) {
            LogUtil.log(config.logFilePath, line);
        }
    }
}
