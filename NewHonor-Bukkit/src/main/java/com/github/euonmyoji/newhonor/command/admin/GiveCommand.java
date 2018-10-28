package com.github.euonmyoji.newhonor.command.admin;

import com.github.euonmyoji.newhonor.NewHonor;
import com.github.euonmyoji.newhonor.api.configuration.PlayerConfig;
import com.github.euonmyoji.newhonor.configuration.Log;
import net.yeah.mungsoup.mung.command.SubCommand;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * @author MungSoup
 */
public class GiveCommand {

    @SubCommand(
            command = "Admin give",
            args = "<玩家> <头衔>",
            description = "${newhonor.command.describe.admin.give}",
            permission = "newhonor.admin",
            hover = "§b就是给予玩家头衔啦!"
    )
    public void execute(CommandSender sender, Player player, String id) {
        PlayerConfig playerConfig = null;
        try {
            playerConfig = PlayerConfig.get(player.getUniqueId());
            if (playerConfig.giveHonor(id)) {
                sender.sendMessage(String.format("%s成功给予%s%s头衔!", NewHonor.prefix, player.getName(), id));
                new Log(player).logGet(sender, id);
                return;
            }
            sender.sendMessage(NewHonor.prefix + "给予头衔失败!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
