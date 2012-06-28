package syam.VoteBan;

import java.util.logging.Logger;

import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;

public class VoteBan extends JavaPlugin{
	// Logger
	public final static Logger log = Logger.getLogger("Minecraft");
	public final static String logPrefix = "[VoteBan] ";
	public final static String msgPrefix = "&c[VoteBan] &f";

	// Listener

	// Private classes
	private ConfigurationManager config;

	// Instance
	private static VoteBan instance;

	/**
	 * プラグイン起動処理
	 */
	public void onEnable(){
		instance = this;
		config = new ConfigurationManager(this);

		// 設定読み込み
		try{
			config.loadConfig(true);
		}catch (Exception ex){
			log.warning(logPrefix+"an error occured while trying to load the config file.");
			ex.printStackTrace();
		}

		// コマンド登録
		getServer().getPluginCommand("vote").setExecutor(new VoteBanCommand(this));
		log.info(logPrefix+ "Initialized Command.");

		// メッセージ表示
		PluginDescriptionFile pdfFile=this.getDescription();
		log.info("["+pdfFile.getName()+"] version"+pdfFile.getVersion()+" is enabled!");
	}
	/**
	 * プラグイン停止処理
	 */
	public void onDisable(){
		// メッセージ表示
		PluginDescriptionFile pdfFile=this.getDescription();
		log.info("["+pdfFile.getName()+"] version"+pdfFile.getVersion()+" is disabled!");
	}

	/* getter */

	/**
	 * 設定マネージャを返す
	 * @return ConfigurationManager
	 */
	public ConfigurationManager getConfigs(){
		return config;
	}

	/**
	 * インスタンスを返す
	 * @return VoteBanインスタンス
	 */
	public static VoteBan getInstance(){
		return instance;
	}
}
