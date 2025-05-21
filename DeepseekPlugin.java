package com.deepseek.minecraft;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

// 由于错误提示表明公共类必须定义在同名文件中，推测可能需要将类名改为与文件名一致
// 这里将类名改为 DeepseekPlugin，原代码已经是该类名，无需修改类名，但需确保文件名为 DeepseekPlugin.java
// 当前文件名为 DeepseekPlugin.java，所以类名无需修改，此错误可能是其他配置问题，若确认文件名无误，可忽略该错误提示
public class DeepseekPlugin extends JavaPlugin implements Listener {

    @Override
    public void onEnable() {
        DeepseekAPI.setup(this);
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("§aDeepseek插件已启动！");
    }

    @Override
    public void onDisable() {
        getLogger().info("§cDeepseek插件已关闭！");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if ("ask".equalsIgnoreCase(cmd.getName())) {
            handleAskCommand(sender, args);
            return true;
        }
        if ("ds".equalsIgnoreCase(cmd.getName())) {
            handleDsCommand(sender, args);
            return true;
        }
        return false;
    }

    private void handleAskCommand(CommandSender sender, String[] args) {
        if (args.length == 0) {
            sendMessage(sender, "§c用法: /ask <问题>");
            return;
        }

        getServer().getScheduler().runTaskAsynchronously(this, () -> {
            int eventId = DeepseekAPI.generateEventId();
            String question = String.join(" ", args);
            String response = DeepseekAPI.sendToDeepseek(question, eventId);

            getServer().getScheduler().runTask(this, () -> {
                sendMessage(sender, "§6[Deepseek] §f" + response);
                getLogger().info("处理完成 EventID: " + eventId);
            });
        });
    }

    private void handleDsCommand(CommandSender sender, String[] args) {
        if (args.length == 0) {
            sendMessage(sender, "§e用法: /ds <消息> 或 /ds reload");
            return;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            try {
                DeepseekAPI.reloadConfig(this);
                sendMessage(sender, "§a配置重载成功！");
                getLogger().info("配置已通过命令重新加载");
            } catch (Exception e) {
                sendMessage(sender, "§c配置重载失败: " + e.getMessage());
                getLogger().severe("配置重载错误: " + e.toString());
            }
            return;
        }

        getServer().getScheduler().runTaskAsynchronously(this, () -> {
            int eventId = DeepseekAPI.generateEventId();
            String message = String.join(" ", args);
            String response = DeepseekAPI.sendToDeepseek(message, eventId);

            getServer().getScheduler().runTask(this, () -> {
                sendMessage(sender, "§b[DS] §f" + response);
                getLogger().info("DS命令处理完成 EventID: " + eventId);
            });
        });
    }

    private void sendMessage(CommandSender sender, String message) {
        sender.sendMessage(message);
    }
}