package syam.VoteBan.Commands;

import java.util.Map.Entry;
import java.util.logging.Logger;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import syam.VoteBan.Actions;
import syam.VoteBan.VoteBan;
import syam.VoteBan.Vote.Vote;
import syam.VoteBan.Vote.VoteOption;

public class YesCommand implements CommandExecutor{
	public final static Logger log = VoteBan.log;
	public final static String logPrefix = VoteBan.logPrefix;
	public final static String msgPrefix = VoteBan.msgPrefix;

	private final VoteBan plugin;
	public YesCommand(final VoteBan plugin){
		this.plugin = plugin;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String commandLabel, String[] args){
		// yes コマンド
		// コンソールチェック
		if (!(sender instanceof Player)){
			Actions.message(sender, null, "&cThis command cannot use from console!");
			return true;
		}
		Player player = (Player)sender;

		if(plugin.votes.size() < 1){
			Actions.message(null, player, "&c現在進行中の投票はありません");
			return true;
		}

		// 1つしか進行していない場合は進行中の投票に対して投票を行う
		else if(plugin.votes.size() == 1){
			Vote vote = null;
			for (Entry<String, Vote> entry : plugin.votes.entrySet()) {
				vote = entry.getValue();
			}
			// 投票可能かチェック
			if (!vote.canVote(player)){
				return true;
			}

			// 賛成投票
			vote.voters.put(player, VoteOption.YES);

			// メッセージ表示
			Actions.message(null, player, "&a投票に賛成しました！");
			return true;
		}

		// TODO: 2つ以上投票が進行中の場合は対象者名が必要 /yes (targetname)
		else{
			// メッセージ表示
			Actions.message(null, player, "&a2つ以上の同時進行投票には未対応です");
			return true;
		}
	}
}
