package syam.VoteBan.Commands;

import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import syam.VoteBan.Actions;
import syam.VoteBan.VoteBan;
import syam.VoteBan.Vote.Vote;
import syam.VoteBan.Vote.VoteOption;
import syam.VoteBan.Vote.VoteType;

public class StartCommands {
	public final static Logger log = VoteBan.log;
	public final static String logPrefix = VoteBan.logPrefix;
	public final static String msgPrefix = VoteBan.msgPrefix;

	private final VoteBan plugin;
	public StartCommands(final VoteBan plugin, final CommandSender sender, final VoteType type, final String[] args){
		this.plugin = plugin;
		run(sender, type, args);
	}

	/**
	 * 投票開始のコマンドが呼ばれた - /vote (type) (user) [reason]
	 * @param sender CommandSender
	 * @param type VoteType
	 * @param args args[]
	 * @return true
	 */
	private boolean run(final CommandSender sender, final VoteType type, final String[] args){
		if (type != null){
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

			// TODO: 開発後このチェックを削除する
			if (plugin.votes.size() > 0){
				Actions.message(sender, null, "&c既に進行中の投票があります！投票の進行には未対応です！");
				return true;
			}

			// 新規投票の開始
			Vote vote = new Vote(plugin, target, player, reason, type);
			plugin.votes.put(target.getName(), vote);

			vote.start();
		}
		return true;
	}
}
