package moyi.yys.commands;

import moyi.yys.NewHonor;
import moyi.yys.configuration.PlayerData;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.text.Text;

public class HonorCommand {
    @SuppressWarnings("ConstantConditions")
    private static CommandSpec use = CommandSpec.builder()
            .arguments(GenericArguments.string(Text.of("id")))
            .executor((src, args) -> {
                if (!(src instanceof Player)) {
                    src.sendMessage(Text.of("[头衔插件]未知发送者,目前该指令近支持玩家自己发送指令修改自己设置。"));
                } else {
                    Task.builder().execute(() -> {
                        PlayerData pd = new PlayerData((User) src);
                        if (pd.setUse(args.<String>getOne(Text.of("id")).get())) {
                            src.sendMessage(Text.of("[头衔插件]修改使用头衔成功"));
                            if (pd.isShowHonor()) {
                                pd.getHonor().ifPresent(text -> NewHonor.usinghonor.put(((User) src).getUniqueId(), text));
                            } else {
                                NewHonor.usinghonor.remove(((User) src).getUniqueId());
                            }
                        } else {
                            src.sendMessage(Text.of("[头衔插件]修改使用头衔失败，可能原因:[头衔未拥有或不存在,储存数据时异常]"));
                            pd.setUse("default");
                            if (pd.isShowHonor()) {
                                pd.getHonor().ifPresent(text -> NewHonor.usinghonor.put(((User) src).getUniqueId(), text));
                            } else {
                                NewHonor.usinghonor.remove(((User) src).getUniqueId());
                            }
                            src.sendMessage(Text.of("[头衔插件]已修改使用头衔为默认头衔default"));
                        }
                    }).async().name("NewHonor - Player Change Using Honor").submit(NewHonor.plugin);
                }
                return CommandResult.success();
            })
            .build();

    @SuppressWarnings("ConstantConditions")
    private static CommandSpec list = CommandSpec.builder()
            .arguments(GenericArguments.onlyOne(GenericArguments.userOrSource(Text.of("user"))))
            .executor((src, args) -> {
                User user = args.<User>getOne(Text.of("user")).get();
                if (!(user.getName().equals(src.getName())) && !src.hasPermission("newhonor.admin")) {
                    src.sendMessage(Text.of("你没有权限查看别人所拥有的权限[权限节点:newhonor.admin"));
                    return CommandResult.empty();
                } else {
                    Task.builder().execute(() -> {
                        PlayerData pd = new PlayerData(user);
                        pd.getHonors().ifPresent(ids -> ids.forEach(id -> {
                            if (NewHonor.hd.getHonor(id).isPresent()) {
                                src.sendMessage(Text.of("头衔id:" + id + ",效果为:", NewHonor.hd.getHonor(id).get()));
                            } else {
                                src.sendMessage(Text.of("你的头衔id:" + id + ",已被服务器删除"));
                                pd.take(id);
                                pd.setUse("default");
                            }
                        }));
                    }).async().name("NewHonor - List Player Honors").submit(NewHonor.plugin);
                    return CommandResult.success();
                }
            })
            .build();

    private static CommandSpec settings = CommandSpec.builder()
            .permission("newhonor.settings")
            .executor((src, args) -> {
                src.sendMessage(Text.of("-------------------------------------"));
                src.sendMessage(Text.of("/honor settings showhonor true/false  显示头衔在聊天栏"));
                src.sendMessage(Text.of("/honor settings displayhonor true/false  显示头衔在头顶[名字前]"));
                src.sendMessage(Text.of("-------------------------------------"));
                return CommandResult.success();
            })
            .child(SettingsChildCommand.showhonor, "showhonor")
            .child(SettingsChildCommand.displayhonor, "displayhonor")
            .build();

    private static CommandSpec admin = CommandSpec.builder()
            .permission("newhonor.admin")
            .executor((src, args) -> {
                src.sendMessage(Text.of("-------------------------------------"));
                src.sendMessage(Text.of("/honor admin add <honorID> <效果>      添加头衔"));
                src.sendMessage(Text.of("/honor admin set <honorID> <效果>      设置头衔"));
                src.sendMessage(Text.of("/honor admin delete <honorID>          删除头衔"));
                src.sendMessage(Text.of("/honor admin give <玩家(们)> <honorID> 给予玩家(们)头衔 "));
                src.sendMessage(Text.of("/honor admin take <玩家(们)> <honorID> 拿走玩家(们)头衔 "));
                src.sendMessage(Text.of("/honor admin list              [假功能]显示全部已添加头衔"));
                src.sendMessage(Text.of("/honor admin reload            重载配置文件并更新缓存"));
                src.sendMessage(Text.of("/honor admin refresh           更新缓存"));
                src.sendMessage(Text.of("-------------------------------------"));
                return CommandResult.success();
            })
            .child(AdminChildCommand.list, "list")
            .child(AdminChildCommand.add, "add")
            .child(AdminChildCommand.delete, "delete")
            .child(AdminChildCommand.set, "set")
            .child(AdminChildCommand.give, "give")
            .child(AdminChildCommand.take, "take")
            .child(AdminChildCommand.refresh, "refresh")
            .child(AdminChildCommand.reload, "reload")
            .build();

    public static CommandSpec honor = CommandSpec.builder()
            .permission("newhonor.use")
            .executor((src, args) -> {
                src.sendMessage(Text.of("-------------------------------------"));
                src.sendMessage(Text.of("插件作者MCBBSID:阴阳师元素祭祀 邮箱1418780411@qq.com"));
                src.sendMessage(Text.of("/honor admin           管理员用指令"));
                src.sendMessage(Text.of("/honor list [用户]     列出拥有的头衔"));
                src.sendMessage(Text.of("/honor use <honorID>  使用头衔"));
                src.sendMessage(Text.of("/honor settings        修改设置"));
                src.sendMessage(Text.of("-------------------------------------"));
                return CommandResult.success();
            })
            .child(settings, "settings")
            .child(admin, "admin")
            .child(use, "use")
            .child(list, "list")
            .build();

}
