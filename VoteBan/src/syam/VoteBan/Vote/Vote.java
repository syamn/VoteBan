package syam.VoteBan.Vote;

import java.util.HashMap;
import java.util.Map.Entry;
import java.util.logging.Logger;

import org.bukkit.entity.Player;

import syam.VoteBan.Actions;
import syam.VoteBan.VoteBan;

public class Vote {
	public final static Logger log = VoteBan.log;
	public final static String logPrefix = VoteBan.logPrefix;
	public final static String msgPrefix = VoteBan.msgPrefix;

	private final VoteBan plugin;

	// 投票に関するデータ
	private Player target; // 投票対象のプレイヤー
	private Player starter; // 投票を開始したプレイヤー
	private String reason; // 投票を開始した理由
	private VoteType type; // 投票の種類

	private int time; // 投票時間
	private int remaining; // 残り時間
	private boolean started = false; // スタートしているかどうか

	public HashMap<Player, VoteOption> voters; // 投票者一覧

	// 最終カウンタ
	int yes = 0;	// VoteOption.YES
	int no = 0; 	// VoteOption.NO
	int abs = 0;	// VoteOption.ABSTENTION
	int invalid = 0;// else

	/**
	 * コンストラクタ
	 * @param plugin
	 * @param voter
	 * @param voted
	 * @param vote
	 */
	public Vote(final VoteBan plugin, Player target, Player starter, String reason, VoteType type){
		this.plugin = plugin;

		// 投票データ設定
		this.target = target;
		this.starter = starter;
		this.reason = reason;
		this.type = type;
	}

	/**
	 * 投票を開始する
	 */
	public void start(){

		// メッセージ表示
		Actions.broadcastMessage("&c[Vote] &d'&6"+target.getName()+"&d'への &c"+type.name()+" &d投票を'&6"+starter.getName()+"&d'が開始しました");
		Actions.broadcastMessage(" &d理由: &f"+reason);

		// タイマー起動
		timer();
		started = true;
	}

	/**
	 * 投票者をチェックして、達しているか判定する
	 */
	public void checkvotes(){
		// 成立に必要な人数を計算
		int threshold = plugin.getServer().getOnlinePlayers().length / 2;

		// 集計
		for (Entry<Player, VoteOption> entry : voters.entrySet()) {
			if (entry.getValue() == VoteOption.YES){
				yes++;
			}else if (entry.getValue() == VoteOption.NO){
				no++;
			}else{
				invalid++;
			}
		}

		// 未投票者は棄権にカウント
		abs = plugin.getServer().getOnlinePlayers().length - voters.size();

		VoteResult result;
		// 結果判定
		if (yes >= threshold){
			result = VoteResult.ACCEPTED;
		}else{
			result = VoteResult.DENIED;
		}

		finished(result);
	}

	/**
	 * 投票結果をチェックして処理を分ける
	 * @param result
	 */
	private void finished(VoteResult result){
		// 結果の処理
		if (result == VoteResult.DENIED){
			// メッセージ表示
			Actions.broadcastMessage("&c[Vote] &d'&6"+target.getName()+"&d'への &c"+type.name()+" &d投票は削除されました");
			Actions.broadcastMessage(" &d投票理由: &f"+reason);
			Actions.broadcastMessage(" &6結果&f: "+result.name()+" - "+"&c賛&f:"+yes+" / &b反&f:"+no+" / &6棄&f:"+abs);

			// 投票不成立 何もしない

		}else if(result == VoteResult.ACCEPTED){
			// メッセージ表示
			Actions.broadcastMessage("&c[Vote] &d'&6"+target.getName()+"&d'への &c"+type.name()+" &d投票は成立しました");
			Actions.broadcastMessage(" &d投票理由: &f"+reason);
			Actions.broadcastMessage(" &6結果&f: "+result.name()+" - "+"&c賛&f:"+yes+" / &b反&f:"+no+" / &6棄&f:"+abs);

			// 投票成立 投票種類によって処理を行う

		}else{
			// メッセージ表示
			Actions.broadcastMessage("&c[Vote] &d'&6"+target.getName()+"&d'への &c"+type.name()+" &d投票はキャンセルされました");
			Actions.broadcastMessage(" &d投票理由: &f"+reason);
			Actions.broadcastMessage(" &6結果&f: "+result.name()+" - "+"&c賛&f:"+yes+" / &b反&f:"+no+" / &6棄&f:"+abs);

			// 投票キャンセル 何もしない
		}

		// 終了
		remove();
	}

	/**
	 * 投票成立時の処理を行う
	 */
	public void accepted(){

	}

	/**
	 * 投票終了
	 */
	public void remove(){
		plugin.votes.remove(target.getName());
	}

	/**
	 * 投票タイマー
	 */
	public void timer(){
		int voteTimeInSeconds = plugin.getConfigs().voteTimeInSeconds;
		// タイマータスク
		plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable(){
			// スタート
			public void run(){
				// 指定した時間が経過した
				checkvotes();
			}
		}, voteTimeInSeconds * 20L); // 設定秒 * 20(tics)
	}
}
