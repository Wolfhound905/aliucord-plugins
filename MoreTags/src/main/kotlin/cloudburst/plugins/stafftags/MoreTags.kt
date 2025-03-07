package cloudburst.plugins.moretags

import com.aliucord.annotations.AliucordPlugin
import com.aliucord.entities.Plugin
import com.aliucord.patcher.Hook
import com.aliucord.patcher.PreHook
import com.aliucord.patcher.Patcher
import com.aliucord.Utils
import android.content.Context
import android.view.View

import com.discord.widgets.chat.list.adapter.WidgetChatListAdapterItemMessage
import com.discord.models.message.Message
import com.discord.models.member.GuildMember
import com.aliucord.wrappers.ChannelWrapper
import com.aliucord.wrappers.GuildMemberWrapper
import com.discord.api.user.User
import com.discord.stores.StoreStream
import com.discord.utilities.permissions.PermissionUtils
import android.widget.TextView
import android.graphics.Color
import com.discord.api.permission.*
import com.lytefast.flexinput.R
import com.discord.utilities.color.ColorCompat

@AliucordPlugin
class MoreTags : Plugin() {

    //val cache: HashMap<Pair<Long, Long>, String?> = hashMapOf();

    override fun start(context: Context) {
        with(WidgetChatListAdapterItemMessage::class.java) { 
            patcher.patch(getDeclaredMethod("configureItemTag", Message::class.java), Hook { callFrame -> try {
                val tag = getDeclaredField("itemTag").let {
                    it.isAccessible = true
                    it.get(callFrame.thisObject) as TextView?
                }
                if (tag == null) return@Hook

                val msg = callFrame.args[0] as Message

                if (msg.webhookId != null && msg.author.f() == "0000") {
                    tag.apply {
                        text = "WEBHOOK";
                        visibility = View.VISIBLE
                    }
                    return@Hook
                }

                val channel = ChannelWrapper(StoreStream.getChannels().getChannel(msg.channelId))
                if (!channel.isGuild()) return@Hook

                val member = StoreStream.getGuilds().getMember(channel.guildId, msg.author.i())
                if (member == null) return@Hook;

                val tagStr = getTag(channel.guildId, member)
                if (tagStr == null) return@Hook;
                tag.apply {
                    text = tagStr;
                    visibility = View.VISIBLE
                }

            } catch (ignored: Throwable) {
                Patcher.logger.error(ignored)
            }})
        }
    }

    override fun stop(context: Context) = patcher.unpatchAll()

    private fun contrastColor(color: Int): Int {
        val c = Color.valueOf(color)
        if ((c.red()*76.2 + c.green()*149.69 + c.blue()*29.07) > 186) return Color.BLACK.toInt()
        else return Color.WHITE.toInt()
    }

    private fun getTag(guildId: Long, member: GuildMember): String? {
        val guild = StoreStream.getGuilds().getGuild(guildId)
        if (guild == null) return null;

        if (guild.isOwner(member.userId)) return "OWNER"
        val roleList = StoreStream.getGuilds().roles.get(guildId)
        if (roleList == null) return null;

        var isAdmin = false
        var isMod = false
        var isStaff = false
        Patcher.logger.info(guild.roles.toString())
        for (roleId in member.roles) {
            val role = roleList.get(roleId)
            if (role == null) continue
            val perms = role.h()
            if (PermissionUtils.can(Permission.ADMINISTRATOR, perms)) {
                isAdmin = true
                break
            }
            if (PermissionUtils.can(Permission.MANAGEMENT_PERMISSIONS, perms) 
                || PermissionUtils.can(Permission.MANAGE_CHANNELS, perms) 
                || PermissionUtils.can(Permission.MANAGE_ROLES, perms)) {
                isStaff = true
            }
            if (PermissionUtils.can(Permission.KICK_MEMBERS, perms) 
                || PermissionUtils.can(Permission.BAN_MEMBERS, perms) 
                || PermissionUtils.can(Permission.MANAGE_MESSAGES, perms)) {
                isMod = true
            }
            
        }
        
        if (isAdmin) return "ADMIN"
        else if (isStaff) return "STAFF"
        else if (isMod) return "MOD"

        return null
    }
}