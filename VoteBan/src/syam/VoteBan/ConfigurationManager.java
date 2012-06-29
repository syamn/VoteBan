package syam.VoteBan;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;
import java.nio.channels.FileChannel;
import java.util.logging.Logger;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public class ConfigurationManager {
	public final static Logger log = VoteBan.log;
	public final static String logPrefix = VoteBan.logPrefix;
	public final static String msgPrefix = VoteBan.msgPrefix;

	private JavaPlugin plugin;
	private FileConfiguration conf;

	private static File pluginDir = new File("plugins", "VoteBan");

	// デフォルトの設定定数
	private final String defaultLogPath = "plugins/VoteBan/vote.log";
	private final String defaultDetailDirectory = "plugins/VoteBan/detail/";
	private final String defaultFixedReason = "Accepted BAN Vote! (!perc!:!yes!)";

	// 設定項目
	/* Basic Configs */
	// デフォルトの投票受付時間
	public int voteTimeInSeconds = new Integer(180);
	public double voteAcceptPerc = new Double(60.0);
	public int voteStartMinPlayers = new Integer(4);

	/* Logging Configs */
	public boolean logToFileFlag = new Boolean(true);
	public String logFilePath = defaultLogPath;
	public boolean logDetailFlag = new Boolean(true);
	public String detailDirectory = defaultDetailDirectory;

	/* Bans Configs */
	public boolean isGlobalBan = new Boolean(false);
	public boolean fixedReasonFlag = new Boolean(true);
	public String fixedReason = defaultFixedReason;
	// 設定ここまで

	/**
	 * コンストラクタ
	 * @param plugin
	 */
	public ConfigurationManager(final JavaPlugin plugin){
		this.plugin = plugin;
		pluginDir = this.plugin.getDataFolder();
	}

	/**
	 * 設定をファイルから読み込む
	 * @param initialLoad
	 */
	public void loadConfig(boolean initialLoad){
		// ディレクトリ作成
		createDirs();

		// 設定ファイルパス取得
		File file = new File(pluginDir, "config.yml");
		// 無ければデフォルトコピー
		if (!file.exists()){
			extractResource("/config.yml", pluginDir, false, true);
			log.info(logPrefix+ "config.yml is not found! Created default config.yml!");
		}

		// 項目取得
		/* Basic Configs */
		voteTimeInSeconds = plugin.getConfig().getInt("VoteTimeInSeconds", 180);
		voteAcceptPerc = plugin.getConfig().getDouble("Percentage", 60.0);
		voteStartMinPlayers = plugin.getConfig().getInt("VoteStartMinPlayers", 4);

		/* Logging Configs */
		logToFileFlag = plugin.getConfig().getBoolean("LogToFile", true);
		logFilePath = plugin.getConfig().getString("LogPath", defaultLogPath);
		logDetailFlag = plugin.getConfig().getBoolean("LogDetail", true);
		detailDirectory = plugin.getConfig().getString("DetailDirectory", defaultDetailDirectory);

		/* Bans Configs */
		isGlobalBan = plugin.getConfig().getBoolean("GlobalBan", false);
		fixedReasonFlag = plugin.getConfig().getBoolean("UseFixedBanReason", true);
		fixedReason = plugin.getConfig().getString("BanReason", defaultFixedReason);

		// 設定ファイルのバージョンチェックを行う
		double version = plugin.getConfig().getDouble("Version");
		checkver(version);

		// 詳細ログ格納用ディレクトリを作成する
		createDir(new File(detailDirectory));
	}

	/**
	 * 設定ファイルに設定を書き込む (コメントが消えるため使わない)
	 * @throws Exception
	 */
	public void save() throws Exception{
		plugin.saveConfig();
	}

	/**
	 * 必要なディレクトリ群を作成する
	 */
	private void createDirs(){
		createDir(plugin.getDataFolder());
	}

	/**
	 * 存在しないディレクトリを作成する
	 * @param dir File 作成するディレクトリ
	 */
	private static void createDir(File dir){
		// 既に存在すれば作らない
		if (dir.isDirectory()){
			return;
		}
		if (!dir.mkdir()){
			log.warning(logPrefix+ "Can't create directory: "+dir.getName());
		}
	}

	/**
	 * 設定ファイルのバージョンをチェックする
	 * @param ver
	 */
	private void checkver(final double ver){
		double configVersion = ver; // 設定ファイルのバージョン
		double nowVersion = 0.1D; // プラグインのバージョン
		try{
			nowVersion = Double.parseDouble(VoteBan.getInstance().getDescription().getVersion());
		}catch (NumberFormatException ex){
			log.warning(logPrefix+ "Cannot parse version string!");
		}

		// 比較 設定ファイルのバージョンが古ければ config.yml を上書きする
		if (configVersion < nowVersion){
			// 先に古い設定ファイルをリネームする
			String destName = "oldconfig-v"+configVersion+".yml";
			String srcPath = new File(plugin.getDataFolder(), "config.yml").getPath();
			String destPath = new File(plugin.getDataFolder(), destName).getPath();
			try{
				copyTransfer(srcPath, destPath);
				log.info(logPrefix+ "Copied old config.yml to "+destName+"!");
			}catch(Exception ex){
				log.warning(logPrefix+ "Cannot copy old config.yml!");
			}

			// config.ymlと言語ファイルを強制コピー
			extractResource("/config.yml", plugin.getDataFolder(), true, false);

			log.info(logPrefix+ "Deleted existing configuration file and generate a new one!");
		}
	}

	/**
	 * リソースファイルをファイルに出力する
	 * @param from 出力元のファイルパス
	 * @param to 出力先のファイルパス
	 * @param force jarファイルの更新日時より新しいファイルが既にあっても強制的に上書きするか
	 * @param checkenc 出力元のファイルを環境によって適したエンコードにするかどうか
	 * @author syam
	 */
	static void extractResource(String from, File to, boolean force, boolean checkenc){
		File of = to;

		// ファイル展開先がディレクトリならファイルに変換、ファイルでなければ返す
		if (to.isDirectory()){
			String filename = new File(from).getName();
			of = new File(to, filename);
		}else if(!of.isFile()){
			log.warning(logPrefix+ "not a file:" + of);
			return;
		}

		// ファイルが既に存在する場合は、forceフラグがtrueでない限り展開しない
		if (of.exists() && !force){
			return;
		}

		OutputStream out = null;
		InputStream in = null;
		InputStreamReader reader = null;
		OutputStreamWriter writer =null;
		DataInputStream dis = null;
		try{
			// jar内部のリソースファイルを取得
			URL res = VoteBan.class.getResource(from);
			if (res == null){
				log.warning(logPrefix+ "Can't find "+ from +" in plugin Jar file");
				return;
			}
			URLConnection resConn = res.openConnection();
			resConn.setUseCaches(false);
			in = resConn.getInputStream();

			if (in == null){
				log.warning(logPrefix+ "Can't get input stream from " + res);
			}else{
				// 出力処理 ファイルによって出力方法を変える
				if (checkenc){
					// 環境依存文字を含むファイルはこちら環境

					reader = new InputStreamReader(in, "UTF-8");
					writer = new OutputStreamWriter(new FileOutputStream(of)); // 出力ファイルのエンコードは未指定 = 自動で変わるようにする

					int text;
					while ((text = reader.read()) != -1){
						writer.write(text);
					}
				}else{
					// そのほか

					out = new FileOutputStream(of);
					byte[] buf = new byte[1024]; // バッファサイズ
					int len = 0;
					while((len = in.read(buf)) >= 0){
						out.write(buf, 0, len);
					}
				}
			}
		}catch (Exception ex){
			ex.printStackTrace();
		}finally{
			// 後処理
			try{
				if (out != null)
					out.close();
				if (in != null)
					in.close();
				if (reader != null)
					reader.close();
				if (writer != null)
					writer.close();
			}catch (Exception ex){}
		}
	}
	/**
	 * コピー元のパス[srcPath]から、コピー先のパス[destPath]へファイルのコピーを行います。
	 * コピー処理にはFileChannel#transferToメソッドを利用します。
	 * コピー処理終了後、入力・出力のチャネルをクローズします。
	 * @param srcPath コピー元のパス
	 * @param destPath  コピー先のパス
	 * @throws IOException 何らかの入出力処理例外が発生した場合
	 */
	public static void copyTransfer(String srcPath, String destPath) throws IOException {
		FileChannel srcChannel = new FileInputStream(srcPath).getChannel();
		FileChannel destChannel = new FileOutputStream(destPath).getChannel();
		try {
		    srcChannel.transferTo(0, srcChannel.size(), destChannel);
		} finally {
		    srcChannel.close();
		    destChannel.close();
		}
	}

	public static File getJarFile(){
		return new File("plugins", "VoteBan.jar");
	}
}
