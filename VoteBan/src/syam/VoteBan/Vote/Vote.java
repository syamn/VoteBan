package syam.VoteBan.Vote;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.logging.Logger;

import org.bukkit.entity.Player;

import syam.VoteBan.Actions;
import syam.VoteBan.VoteBan;
import syam.VoteBan.Util.Util;

public class Vote {
	public final static Logger log = VoteBan.log;
	public final static String logPrefix = VoteBan.logPrefix;
	public final static String msgPrefix = VoteBan.msgPrefix;

	private final VoteBan plugin;

	// 投票に関するデータ
	private String VoteID; // 一意な投票ID

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
	// 賛成割合
	double yesPerc = 0.0;
	// 投票割合
	double votePerc = 0.0;

	/**
	 * コンストラクタ
	 * @param plugin
	 * @param voter
	 * @param voted
	 * @param vote
	 */
	public Vote(final VoteBan plugin, Player target, Player starter, String reason, VoteType type){
		this.plugin = plugin;

		// 投票ID作成 (playername-yyMMdd-HHmmss);
		SimpleDateFormat sdf = new SimpleDateFormat("yyMMdd-HHmmss");
		this.VoteID = target.getName() + "_" + sdf.format(new Date());

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
		int sec = plugin.getConfigs().voteTimeInSeconds;
		// メッセージ表示
		Actions.broadcastMessage("&c[Vote] &d'&6"+target.getName()+"&d'への &c"+type.name()+" &d投票を'&6"+starter.getName()+"&d'が開始しました");
		Actions.broadcastMessage(" &d理由: &f"+reason);
		Actions.broadcastMessage(" &c"+sec+"秒&d以内に投票を行ってください | 賛成: &f/vote yes &d| &d反対: &f/vote no");
		Actions.broadcastMessage(" &d成立条件: &6投票率 &f"+plugin.getConfigs().voteNeedVoterPerc+"% 以上 &6 賛成率 &f"+plugin.getConfigs().voteAcceptPerc+"% 以上");

		// ロギング
		if (plugin.getConfigs().logDetailFlag && plugin.getConfigs().logToFileFlag){
			Actions.log(plugin.getConfigs().logFilePath,
					starter.getName()+ " Started "+type.name()+" Vote against "+target.getName()+" VoteID: "+VoteID);
		}
		Actions.deflog(starter.getName()+ " Started "+type.name()+" Vote against "+target.getName());
		Actions.deflog("Reason: "+reason);
		log("========================================");
		log(" "+starter.getName()+ " Started "+type.name()+" Vote against "+target.getName());
		log(" Vote Reason: "+reason);
		log("========================================");

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
	 * 各投票ごとのログを取る
	 * @param line
	 */
	public void log(String line){
		// 設定確認
		if (plugin.getConfigs().logDetailFlag){
			String filepath = plugin.getConfigs().detailDirectory + VoteID + ".log";
			Actions.log(filepath, line);
		}
	}

	/**
	 * 投票者をチェックして、達しているか判定する
	 */
	public void checkvotes(){
		// 成立に必要な割合を取得
		double threshold = plugin.getConfigs().voteAcceptPerc;
		double needPerc = plugin.getConfigs().voteNeedVoterPerc;
		int onlines = plugin.getServer().getOnlinePlayers().length;

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
		abs = onlines - voters.size();

		VoteResult result;
		yesPerc = Util.getPercent(yes, yes+no);

		// 賛成票が1に満たない場合は0%設定
		if (yes < 1){
			yesPerc = 0.0;
		}

		// 結果判定
		if (yesPerc >= threshold){
			result = VoteResult.ACCEPTED;
		}else{
			result = VoteResult.DENIED;
		}

		// 投票総数が一定数以下ならすべて無効
		votePerc = Util.getPercent(yes + no, onlines);
		if (votePerc < needPerc){
			result = VoteResult.VOID;
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
				Actions.broadcastMessage(" &6結果&f: "+result.name()+"["+yesPerc+"%] - "+"&c賛&f:"+yes+" / &b反&f:"+no+" / &6棄&f:"+abs+" | &d投票率: &f"+votePerc+"%");

				// 投票不成立 何もしない
				break;

			// 成立
			case ACCEPTED:
				// メッセージ表示
				Actions.broadcastMessage("&c[Vote] &d'&6"+target.getName()+"&d'への &c"+type.name()+" &d投票は成立しました");
				Actions.broadcastMessage(" &d投票理由: &f"+reason);
				Actions.broadcastMessage(" &6結果&f: "+result.name()+"["+yesPerc+"%] - "+"&c賛&f:"+yes+" / &b反&f:"+no+" / &6棄&f:"+abs+" | &d投票率: &f"+votePerc+"%");

				// 投票成立 投票種類によって処理を行う
				accepted();
				break;

			// キャンセル
			case VOID:
				// メッセージ表示
				Actions.broadcastMessage("&c[Vote] &d'&6"+target.getName()+"&d'への &c"+type.name()+" &d投票は無効です");
				Actions.broadcastMessage(" &d投票理由: &f"+reason);
				Actions.broadcastMessage(" &6結果&f: "+result.name()+"["+yesPerc+"%] - "+"&c賛&f:"+yes+" / &b反&f:"+no+" / &6棄&f:"+abs+" | &d投票率: &f"+votePerc+"%");

				// 投票キャンセル 何もしない
				break;
			// キャンセル
			case CANCELLED:
				// メッセージ表示
				Actions.broadcastMessage("&c[Vote] &d'&6"+target.getName()+"&d'への &c"+type.name()+" &d投票はキャンセルされました");
				Actions.broadcastMessage(" &d投票理由: &f"+reason);
				Actions.broadcastMessage(" &6結果&f: "+result.name()+"["+yesPerc+"%] - "+"&c賛&f:"+yes+" / &b反&f:"+no+" / &6棄&f:"+abs+" | &d投票率: &f"+votePerc+"%");

				// 投票キャンセル 何もしない
				break;
		}

		// ロギング
		int total = yes + no + abs + invalid;
		Actions.deflog(type.name()+" Vote against "+target.getName()+" Finished. Result: "+result.name());
		Actions.deflog("Percentage: "+yesPerc+"% - YES:"+yes+" NO:"+no+" ABS:"+abs+" INV:"+invalid+" - TOTAL: "+total);
		log("========================================");
		log(" Vote Finished. Result: "+result.name());
		log(" Percentage: "+yesPerc+"% - YES:"+yes+" NO:"+no+" ABS:"+abs+" INV:"+invalid+" - TOTAL: "+total);
		log("========================================");

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
				// 設定フラグの参照
				String BanReason = reason;
				if (plugin.getConfigs().fixedReasonFlag){
					BanReason = plugin.getConfigs().fixedReason;
					BanReason = BanReason.replaceAll("!perc!", yesPerc+"%");
					BanReason = BanReason.replaceAll("!yes!", String.valueOf(yes));
				}
				plugin.getBansHandler().ban(target, starter.getName(), BanReason);
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
