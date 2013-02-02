package net.syamn.voteban.Commands;

import java.io.UnsupportedEncodingException;
import java.util.logging.Logger;

import net.syamn.utils.StrUtil;
import net.syamn.utils.Util;
import net.syamn.voteban.VoteBan;
import net.syamn.voteban.Vote.Vote;
import net.syamn.voteban.Vote.VoteType;
import net.syamn.voteban.VoteActions.BanMethod;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class StartCommands {
    public final static Logger log = VoteBan.log;
    public final static String logPrefix = VoteBan.logPrefix;
    public final static String msgPrefix = VoteBan.msgPrefix;

    private final VoteBan plugin;

    public StartCommands(final VoteBan plugin, final CommandSender sender, final VoteType type, final String[] args) {
        this.plugin = plugin;
        run(sender, type, args);
    }

    /**
     * 投票開始のコマンドが呼ばれた - /vote (type) (user) [reason]
     * 
     * @param sender
     *            CommandSender
     * @param type
     *            VoteType
     * @param args
     *            args[]
     * @return true
     */
    private boolean run(final CommandSender sender, final VoteType type, final String[] args) {
        if (type != null) {
            // コンソールチェック
            if (!(sender instanceof Player)) {
                Util.message(sender, "&cThis command cannot use from console!");
                return true;
            }
            Player player = (Player) sender;
            // 権限チェック
            if (!sender.hasPermission("voteban.startvote." + type.name().toLowerCase())) {
                Util.message(sender, "&cYou don't have permission to use this!");
                return true;
            }
            // 引数チェック
            if (args.length < 3) {
                Util.message(sender, "&c理由を記入してください！");
                return true;
            }
            // 人数チェック
            if (plugin.getConfigs().voteStartMinPlayers > plugin.getServer().getOnlinePlayers().length) {
                Util.message(sender, "&c投票開始に必要なオンライン人数が足りません！" + "(" + plugin.getServer().getOnlinePlayers().length + "/" + plugin.getConfigs().voteStartMinPlayers + ")");
                return true;
            }

            // 対象プレイヤーチェック
            OfflinePlayer checkTarget = Bukkit.getServer().getOfflinePlayer(args[1]);
            if (!checkTarget.isOnline()) {
                Util.message(sender, "&cそのプレイヤーはオフラインです！");
                return true;
            }
            Player target = (Player) checkTarget;

            // 自分自身のチェック
            if (target == player) {
                Util.message(sender, "&c自分を投票することはできません！");
                return true;
            }

            // 理由メッセージ結合
            String reason = args[2];
            int len = args.length;
            for (int i = 4; len >= i; i++) {
                reason = reason + " " + args[i - 1];
            }

            // MCBans用に全角文字のチェック
            try {
                if (!plugin.getConfigs().fixedReasonFlag && plugin.getBansHandler().getBanMethod() == BanMethod.MCBANS && StrUtil.containsZen(reason)) {
                    Util.message(player, "&c理由は英語で記述してください");
                    return true;
                }
            } catch (UnsupportedEncodingException ex) {
                ex.printStackTrace();
            }

            // 既にそのプレイヤーへの投票が進行中でないかチェック
            if (plugin.votes.containsKey(target.getName())) {
                Util.message(sender, "&cそのプレイヤーへの投票は既に進行中です！");
                return true;
            }

            // TODO: 開発後このチェックを削除する
            if (plugin.votes.size() > 0) {
                Util.message(sender, "&c既に進行中の投票があります！投票の同時進行には未対応です！");
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
