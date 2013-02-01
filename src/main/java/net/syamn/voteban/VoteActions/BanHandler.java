package net.syamn.voteban.VoteActions;

import java.util.logging.Logger;

import net.syamn.voteban.Actions;
import net.syamn.voteban.ConfigurationManager;
import net.syamn.voteban.VoteBan;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import com.mcbans.firestar.mcbans.MCBans;
import com.mcbans.firestar.mcbans.api.MCBansAPI;

public class BanHandler {
	public final static Logger log = VoteBan.log;
	public final static String logPrefix = VoteBan.logPrefix;
	public final static String msgPrefix = VoteBan.msgPrefix;

	@SuppressWarnings("unused")
	private final VoteBan plugin;
	private ConfigurationManager config;
	private BanMethod banMethod = BanMethod.VANILLA;
	private MCBansAPI mcbApi;

	public BanHandler(final VoteBan plugin){
		this.plugin = plugin;
		config = plugin.getConfigs();
	}

	/**
	 * BANを行うプラグインをセットアップする
	 * @param plugin プラグインのインスタンス
	 * @return BANを行うプラグインを列挙したBanMethod列挙体
	 */
	public BanMethod setupBanHandler(JavaPlugin plugin){
		// MCBans
		Plugin checkMCBans = plugin.getServer().getPluginManager().getPlugin("MCBans");

		// glizer
		Plugin checkGl = plugin.getServer().getPluginManager().getPlugin("glizer");
		if (checkGl == null){
			checkGl = plugin.getServer().getPluginManager().getPlugin("Glizer");
		}

		// EasyBan
		Plugin checkEB = plugin.getServer().getPluginManager().getPlugin("EasyBan");
		if (checkEB == null){
			checkEB = plugin.getServer().getPluginManager().getPlugin("easyban");
		}

		// UltraBan
		Plugin checkUB = plugin.getServer().getPluginManager().getPlugin("UltraBan");
		if (checkUB == null){
			checkUB = plugin.getServer().getPluginManager().getPlugin("ultraban");
		}

		// DynamicBan
		Plugin checkDynB =plugin.getServer().getPluginManager().getPlugin("DynamicBan");
		if (checkDynB == null){
			checkDynB = plugin.getServer().getPluginManager().getPlugin("dynamicban");
		}

		// 他のBAN関係のプラグインを追加する時はここに

		// MCBans
		if (checkMCBans != null){
			// バージョンチェック
			if (checkMCBans.getDescription().getVersion().trim().startsWith("3")){
				log.warning("Old MCBans plugin found but Observer supports the version 4.0+");
				banMethod = BanMethod.VANILLA;
			}else{
				mcbApi = ((MCBans) checkMCBans).getAPI(plugin);
				log.info(logPrefix+ "MCBans plugin 4.0+ Found! Using that!");
				banMethod = BanMethod.MCBANS;
			}
		}else if (checkGl != null){
			banMethod = BanMethod.GLIZER;
		}else if (checkEB != null){
			banMethod = BanMethod.EASYBAN;
		}else if (checkUB != null){
			banMethod = BanMethod.ULTRABAN;
		}else if (checkDynB != null){
			banMethod = BanMethod.DYNBAN;
		}else{
			// サポートしているBANプラグインが見つからなかった
			banMethod = BanMethod.VANILLA;
		}

		// BANを行うプラグインを返す
		return banMethod;
	}

	/**
	 * プレイヤーをBANする
	 * @param player BAN対象のプレイヤー
	 * @param sender BANを行ったプレイヤー(String)
	 * @param reason BANの理由
	 */
	public void ban(Player player, String sender, String reason){
		// 連携プラグインによって処理を分ける
		switch (banMethod){
			case VANILLA: // バニラ サポートプラグインが入っていない場合は通常のBAN処理
				player.kickPlayer(reason);
				// コンソールから ban (playername) 実行
				Actions.executeCommandOnConsole("ban " + player.getName());
				break;
			case MCBANS: // MCBans
				player.kickPlayer(reason);
				ban_MCBans(player, sender, reason);
				break;
			case GLIZER: // glizer
				ban_glizer(player, reason);
				break;
			case EASYBAN: // EasyBan
				ban_EB(player, reason);
				break;
			case ULTRABAN: // UltraBan
				ban_UB(player, reason);
				break;
			case DYNBAN: // DynamicBan
				ban_DynB(player, reason);
				break;
			default: // Exception: Undefined banMethod
				log.warning(logPrefix+"Error occurred on banning player (BanHandler.class)");
				break;
		}
	}
	/**
	 * プレイヤーをKickする
	 * @param player Kick対象のプレイヤー
	 * @param sender Kickを行ったプレイヤー(String)
	 * @param reason Kickの理由
	 */
	public void kick(Player player, String sender, String reason){
		// 連携プラグインによって処理を分ける
		switch (banMethod){
			case VANILLA: // バニラ サポートプラグインが入っていない場合は通常のKick処理
				player.kickPlayer(reason);
				break;
			case MCBANS: // MCBans
				kick_MCBans(player, sender, reason);
				break;
			case GLIZER: // glizer
				kick_glizer(player, reason);
				break;
			case EASYBAN: // EasyBan
				kick_EB(player, reason);
				break;
			case ULTRABAN: // UltraBan
				kick_UB(player, reason);
				break;
			case DYNBAN: // DynamicBan
				kick_DynB(player, reason);
				break;
			default: // Exception: Undefined banMethod
				player.kickPlayer(reason);
				log.warning(logPrefix+"Error occurred on kicking player (BanHandler.class)");
				break;
		}
	}

	/**
	 * BanMethodを返す
	 * @return BanMethod
	 */
	public BanMethod getBanMethod(){
		return banMethod;
	}

	/**
	 * MCBansを使ってBANを行う
	 * @param player BAN対象のプレイヤー
	 * @param sender BANの送信者
	 * @param reason BANの理由
	 */
	private void ban_MCBans(Player player, String sender, String reason){
		if (config.isGlobalBan){
			mcbApi.globalBan(player.getName(), sender, reason);
		}else{
			mcbApi.localBan(player.getName(), sender, reason);
		}
	}
	/**
	 * MCBansを使ってKickを行う
	 * @param player Kick対象のプレイヤー
	 * @param sender Kickの送信者
	 * @param reason Kickの理由
	 */
	private void kick_MCBans(Player player, String sender, String reason){
		mcbApi.kick(player.getName(), sender, reason);
	}

	/**
	 * glizerを使ってローカル/グローバルBANを行う
	 * @param player 対象プレイヤー
	 * @param reason 理由
	 */
	private void ban_glizer(Player player, String reason){
		if (config.isGlobalBan){ // グローバル
			Actions.executeCommandOnConsole("globalban " + player.getName() + " " + reason);
		}else{ // ローカル
			Actions.executeCommandOnConsole("localban " + player.getName() + " " + reason);
		}
	}
	/**
	 * glizerを使ってKickを行う
	 * @param player 対象プレイヤー
	 * @param reason 理由
	 */
	private void kick_glizer(Player player, String reason){
		Actions.executeCommandOnConsole("kick " + player.getName() + " " + reason);
	}

	/**
	 * EasyBanを使ってBANを行う
	 * @param player 対象プレイヤー
	 * @param reason 理由
	 */
	private void ban_EB(Player player, String reason){
		Actions.executeCommandOnConsole("eban " + player.getName() + " " + reason);
	}
	/**
	 * EasyBanを使ってKickを行う
	 * @param player 対象プレイヤー
	 * @param reason 理由
	 */
	private void kick_EB(Player player, String reason){
		Actions.executeCommandOnConsole("ekick " + player.getName() + " " + reason);
	}

	/**
	 * UltraBanを使ってBANを行う
	 * @param player 対象プレイヤー
	 * @param reason 理由
	 */
	private void ban_UB(Player player, String reason){
		Actions.executeCommandOnConsole("ban " + player.getName() + " " + reason);
		// IPBANも可能
		//Actions.executeCommandOnConsole("ipban " + player.getName() + " " + reason);
	}
	/**
	 * UltraBanを使ってKickを行う
	 * @param player 対象プレイヤー
	 * @param reason 理由
	 */
	private void kick_UB(Player player, String reason){
		Actions.executeCommandOnConsole("eban " + player.getName() + " " + reason);
	}

	/**
	 * DynamicBanを使ってBANを行う
	 * @param player 対象プレイヤー
	 * @param reason 理由
	 */
	private void ban_DynB(Player player, String reason){
		Actions.executeCommandOnConsole("dynban " + player.getName() + " " + reason);
	}
	/**
	 * DynamicBanを使ってKickを行う
	 * @param player 対象プレイヤー
	 * @param reason 理由
	 */
	private void kick_DynB(Player player, String reason){
		Actions.executeCommandOnConsole("dynkick " + player.getName() + " " + reason);
	}
}
