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
	private final VoteType type; // 投票の種類

	private int time; // 投票時間
	private int remaining; // 残り時間
	private boolean started = false; // スタートしているかどうか

	public HashMap<Player, VoteOption> voters = new HashMap<Player, VoteOption>(); // 投票者一覧

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
	 * プレイヤーが投票可能かチェックする
	 * @param player チェック対象のプレイヤー
	 * @return trueなら投票可能、falseなら不可能
	 */
	public boolean canVote(Player player){
		if (target == player){
			Actions.message(null, player, "&c自分自身の投票に参加することはできません");
			return false;
		}
		// TODO: 権限チェックを追加、投票可能者数で最終決定のパーセンテージを取る

		if (voters.containsKey(player)){
			Actions.message(null, player, "&cあなたは既に投票しています！");
			return false;
		}

		// 投票可能
		return true;
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

		// 賛成票が1に満たない場合は拒否
		if (yes < 1){
			result = VoteResult.DENIED;
		}

		// 結果で処理を分ける
		finished(result);
	}

	/**
	 * 投票結果をチェックして処理を分ける
	 * @param result
	 */
	private void finished(VoteResult result){
		// 結果の処理
		switch(result){
			// 拒否
			case DENIED:
				// メッセージ表示
				Actions.broadcastMessage("&c[Vote] &d'&6"+target.getName()+"&d'への &c"+type.name()+" &d投票は拒否されました");
				Actions.broadcastMessage(" &d投票理由: &f"+reason);
				Actions.broadcastMessage(" &6結果&f: "+result.name()+" - "+"&c賛&f:"+yes+" / &b反&f:"+no+" / &6棄&f:"+abs);

				// 投票不成立 何もしない
				break;

			// 成立
			case ACCEPTED:
				// メッセージ表示
				Actions.broadcastMessage("&c[Vote] &d'&6"+target.getName()+"&d'への &c"+type.name()+" &d投票は成立しました");
				Actions.broadcastMessage(" &d投票理由: &f"+reason);
				Actions.broadcastMessage(" &6結果&f: "+result.name()+" - "+"&c賛&f:"+yes+" / &b反&f:"+no+" / &6棄&f:"+abs);

				// 投票成立 投票種類によって処理を行う
				accepted();
				break;

			// キャンセル
			case CANCELLED:
				// メッセージ表示
				Actions.broadcastMessage("&c[Vote] &d'&6"+target.getName()+"&d'への &c"+type.name()+" &d投票はキャンセルされました");
				Actions.broadcastMessage(" &d投票理由: &f"+reason);
				Actions.broadcastMessage(" &6結果&f: "+result.name()+" - "+"&c賛&f:"+yes+" / &b反&f:"+no+" / &6棄&f:"+abs);

				// 投票キャンセル 何もしない
				break;
		}

		// 終了
		remove();
	}

	/**
	 * 投票成立時の処理を行う
	 */
	public void accepted(){
		switch (type){
			// BAN
			case BAN:
				plugin.getBansHandler().ban(target, starter.getName(), reason);
				break;

			// Kick
			case KICK:
				// オンラインチェックを行う
				if (target == null || !target.isOnline()){
					break;
				}
				plugin.getBansHandler().kick(target, starter.getName(), reason);
				break;
		}
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
