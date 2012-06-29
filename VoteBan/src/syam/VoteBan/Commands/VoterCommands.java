package syam.VoteBan.Commands;

import java.util.Map.Entry;
import java.util.logging.Logger;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import syam.VoteBan.Actions;
import syam.VoteBan.VoteBan;
import syam.VoteBan.Vote.Vote;
import syam.VoteBan.Vote.VoteOption;
import syam.VoteBan.Vote.VoteType;

public class VoterCommands {
	public final static Logger log = VoteBan.log;
	public final static String logPrefix = VoteBan.logPrefix;
	public final static String msgPrefix = VoteBan.msgPrefix;

	private final VoteBan plugin;
	public VoterCommands(final VoteBan plugin, final CommandSender sender, final VoteOption option, final String[] args){
		this.plugin = plugin;
		run(sender, option, args);
	}

	/**
	 * 投票コマンドが呼ばれた - /vote (yes|no) [player]
	 * @param sender CommandSender
	 * @param option VoteOption
	 * @param args args[]
	 * @return true
	 */
	private boolean run(final CommandSender sender, final VoteOption option, final String[] args){
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

		// 投票オプション判定
		if (option == VoteOption.INVALID || option == VoteOption.ABSTENTION){
			Actions.message(null, player, "&c無効な投票種類です！");
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
			vote.voters.put(player, option);

			// メッセージ表示
			Actions.message(null, player, "&a投票に"+getOptionName(option)+"投票しました！");
			vote.log(" + Player "+player.getName()+ " Voted to "+option.name());
			return true;
		}

		// TODO: 2つ以上投票が進行中の場合は対象者名が必要 /yes (targetname)
		else{
			// メッセージ表示
			Actions.message(null, player, "&a2つ以上の同時進行投票には未対応です");
			return true;
		}
	}

	/**
	 * VoteOptionのオプション日本語名を得る
	 * @param option VoteOption
	 * @return 日本語名
	 */
	private String getOptionName(final VoteOption option){
		String s = "";

		switch (option){
			case YES:
				s = "賛成"; break;
			case NO:
				s = "反対"; break;
			case ABSTENTION:
				s = "棄権"; break;
			case INVALID:
				s = "無効"; break;
			default:
				s = "null"; break;
		}

		return s;
	}
}
