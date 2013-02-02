package net.syamn.voteban.Commands;

import java.util.logging.Logger;

import net.syamn.utils.Util;
import net.syamn.voteban.VoteBan;
import net.syamn.voteban.Vote.VoteOption;
import net.syamn.voteban.Vote.VoteType;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class VoteCommand implements CommandExecutor{
	public final static Logger log = VoteBan.log;
	public final static String logPrefix = VoteBan.logPrefix;
	public final static String msgPrefix = VoteBan.msgPrefix;

	private final VoteBan plugin;
	public VoteCommand(final VoteBan plugin){
		this.plugin = plugin;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String commandLabel, String[] args){
		// vote reload - 設定再読み込み
		if (args.length >= 1 && args[0].equalsIgnoreCase("reload")){
			// 権限チェック
			if (!sender.hasPermission("voteban.admin")){
			        Util.message(sender, "&cYou don't have permission to use this!");
				return true;
			}
			try{
				plugin.getConfigs().loadConfig(false);
			}catch (Exception ex){
				log.warning(logPrefix+"an error occured while trying to load the config file.");
				ex.printStackTrace();
				return true;
			}
			Util.message(sender, "&aConfiguration reloaded!");
			return true;
		}

		// vote (yes|no) [player] - 投票
		if (args.length >= 1){
			// 投票オプションの判定
			VoteOption option = null;
			for (VoteOption vo : VoteOption.values()){
				if (args[0].equalsIgnoreCase(vo.name().toLowerCase())){
					option = vo;
				}
			}

			if (option != null){
				// VoterCommandsクラスに処理を渡す
				@SuppressWarnings("unused")
				VoterCommands votervote = new VoterCommands(plugin, sender, option, args);
				return true;
			}
		}

		// vote ban (player) (reason) - BAN投票開始
		if (args.length >= 2){
			// このif文だけで複数の投票種類に対応させる
			VoteType type = null;
			for (VoteType vt : VoteType.values()){
				if (args[0].equalsIgnoreCase(vt.name().toLowerCase())){
					type = vt;
				}
			}

			if (type != null){
				// StartCommandsクラスに処理を渡す
				@SuppressWarnings("unused")
				StartCommands start = new StartCommands(plugin, sender, type, args);
				return true;
			}
		}

		// コマンドヘルプを表示
		Util.message(sender, "&c===================================");
		Util.message(sender, "&bVoteBan Plugin version &3%version &bby syamn");
		Util.message(sender, " &b<>&f = required, &b[]&f = optional");
		Util.message(sender, " /vote ban (name) (reason) &7- &fBan Voting");
		Util.message(sender, " /vote kick (name) (reason) &7- &fKick Voting");
		Util.message(sender, " /vote yes &7- &fVoting for YES");
		Util.message(sender, " /vote no &7- &fVoting for NO");
		Util.message(sender, " &7/vote reload - &fReloading config.yml");
		Util.message(sender, "&c===================================");

		return true;
	}
}
