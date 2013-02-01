package net.syamn.voteban.Commands;

import java.util.logging.Logger;

import net.syamn.voteban.Actions;
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
		Actions.message(sender, null, "&c===================================");
		Actions.message(sender, null, "&bVoteBan Plugin version &3%version &bby syamn");
		Actions.message(sender, null, " &b<>&f = required, &b[]&f = optional");
		Actions.message(sender, null, " /vote ban (name) (reason) &7- &fBan Voting");
		Actions.message(sender, null, " /vote kick (name) (reason) &7- &fKick Voting");
		Actions.message(sender, null, " /vote yes &7- &fVoting for YES");
		Actions.message(sender, null, " /vote no &7- &fVoting for NO");
		Actions.message(sender, null, " &7/vote reload - &fReloading config.yml");
		Actions.message(sender, null, "&c===================================");

		return true;
	}
}
