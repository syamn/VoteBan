package syam.VoteBan;

import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import syam.VoteBan.Vote.Vote;
import syam.VoteBan.Vote.VoteType;

public class VoteBanCommand implements CommandExecutor{
	public final static Logger log = VoteBan.log;
	public final static String logPrefix = VoteBan.logPrefix;
	public final static String msgPrefix = VoteBan.msgPrefix;

	private final VoteBan plugin;
	public VoteBanCommand(final VoteBan plugin){
		this.plugin = plugin;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String commandLabel, String[] args){
		// vote reload - 設定再読み込み
		if (args.length >= 1 && args[0].equalsIgnoreCase("reload")){
			// 権限チェック
			if (!sender.hasPermission("voteban.admin")){
				Actions.message(sender, null, "&cYou don't have permission to use this!");
				return true;
			}
			try{
				plugin.getConfigs().loadConfig(false);
			}catch (Exception ex){
				log.warning(logPrefix+"an error occured while trying to load the config file.");
				ex.printStackTrace();
				return true;
			}
			Actions.message(sender, null, "&aConfiguration reloaded!");
			return true;
		}

		// vote ban (player) (reason) - BAN投票開始
		if (args.length >= 2 && args[0].equalsIgnoreCase("ban")){
			// コンソールチェック
			if (!(sender instanceof Player)){
				Actions.message(sender, null, "&cThis command cannot use from console!");
				return true;
			}
			Player player = (Player)sender;
			// 権限チェック
			if (!sender.hasPermission("banvote.startvote.ban")){
				Actions.message(sender, null, "&cYou don't have permission to use this!");
				return true;
			}
			// 引数チェック
			if (args.length < 3){
				Actions.message(sender, null, "&c理由を記入してください！");
				return true;
			}

			// 対象プレイヤーチェック
			OfflinePlayer checkTarget = Bukkit.getServer().getOfflinePlayer(args[1]);
			if (!checkTarget.isOnline()){
				Actions.message(sender, null, "&cそのプレイヤーはオフラインです！");
				return true;
			}

			Player target = (Player)checkTarget;
			// 理由メッセージ結合
			String reason = args[2];
			int len = args.length;
			for (int i = 4; len >= i; i++){
				reason = reason + " " + args[i-1];
			}

			// 既にそのプレイヤーへの投票が進行中でないかチェック
			if (plugin.votes.containsKey(target.getName())){
				Actions.message(sender, null, "&cそのプレイヤーへの投票は既に進行中です！");
				return true;
			}

			// 新規投票の開始
			Vote vote = new Vote(plugin, target, player, reason, VoteType.BAN);
			plugin.votes.put(target.getName(), vote);

			vote.start();
		}

		// コマンドヘルプを表示
		Actions.message(sender, null, "&c===================================");
		Actions.message(sender, null, "&bVoteBan Plugin version &3%version &bby syamn");
		Actions.message(sender, null, " &b<>&f = required, &b[]&f = optional");
		Actions.message(sender, null, " /vote reload &7- &fReloading config.yml");
		Actions.message(sender, null, "&c===================================");

		return true;
	}
}
