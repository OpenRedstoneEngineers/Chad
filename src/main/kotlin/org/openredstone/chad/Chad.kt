package org.openredstone.chad

import com.uchuhimo.konf.Config
import com.uchuhimo.konf.source.yaml
import mu.KotlinLogging
import org.javacord.api.DiscordApiBuilder
import org.javacord.api.entity.message.MessageBuilder
import org.javacord.api.entity.message.MessageType
import org.javacord.api.entity.message.embed.EmbedBuilder
import org.openredstone.chad.commands.*
import org.openredstone.chad.commands.dsl.command
import java.awt.Color
import java.util.*
import kotlin.concurrent.schedule
import kotlin.system.exitProcess

/**
 * The global logger for Chad.
 */
val logger = KotlinLogging.logger("Chad")

/**
 * The main function.
 */
fun main(args: Array<String>) {
    // argument parsing
    if (args.size != 1) {
        eprintln("Expected one argument, got ${args.size}")
        eprintln("Usage: Chad config")
        exitProcess(1)
    }
    val configFile = args[0]

    // loading config
    val config = Config { addSpec(ChadSpec) }.from.yaml.watchFile(configFile)
    var chadConfig = config[ChadSpec.chad]
    val database = Sql(chadConfig.databaseFile)
    database.initTables()

    // logging properties
    val loggingConfig = chadConfig.logging
    System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", loggingConfig.defaultLevel)
    System.setProperty("org.slf4j.simpleLogger.log.Chad", loggingConfig.chadLevel)
    System.setProperty("org.slf4j.simpleLogger.log.Spoiler listener", loggingConfig.chadLevel)
    System.setProperty("org.slf4j.simpleLogger.showDateTime", "true")
    System.setProperty("org.slf4j.simpleLogger.dateTimeFormat", chadConfig.logging.dateTimeFormat)
    System.setProperty("org.slf4j.simpleLogger.showThreadName", "false")

    logger.info("Loading Chad...")
    logger.info("Notification channel ID: ${chadConfig.notificationChannelId}")
    logger.info("Command character: '${chadConfig.commandChar}'")
    logger.info("Disable spoilers: ${chadConfig.disableSpoilers}")

    val discordApi = DiscordApiBuilder()
        .setToken(chadConfig.botToken)
        .setAllIntents()
        .login()
        .join()
        .apply { updateActivity(chadConfig.playingMessage) }

    val discordServer = discordApi.getServerById(chadConfig.serverId).get()

    val commands = concurrentMapOf<String, Command>()

    fun regenerateHelpCommand() {
        commands["help"] = helpCommand(commands)
    }

    fun reloadCommands() {
        chadConfig = config[ChadSpec.chad]
        val authorizedRoles = chadConfig.authorizedDiscordRoles

        logger.info("(Re)loading commands...")

        commands.apply {
            clear()
            put("poll", pollCommand)
            put("roll", rollCommand)
            put("lmgtfy", lmgtfy)
            put("apply", applyCommand)
            put("insult", insultCommand(chadConfig.insults))
            put("add", command(authorizedRoles) {
                val name by required()
                val messages by vararg()
                reply {
                    val msg = messages.joinToString(separator = " ")
                    val cmd = command {
                        reply { msg }
                    }
                    database.insertCommand(name, msg)
                    commands[name] = cmd
                    regenerateHelpCommand()
                    "Done!"
                }
            })
            put("remove", command(authorizedRoles) {
                val name by required()
                reply {
                    database.removeCommand(name)
                    commands.remove(name)
                    regenerateHelpCommand()
                    "Done!"
                }
            })
            put("delete", command(authorizedRoles) {
                val reason by vararg()
                reply {
                    val realReason = reason.joinToString(" ")
                    val target = message.referencedMessage.toNullable()
                    if (message.type != MessageType.REPLY || target == null) {
                        return@reply "Unable to delete, I do not know what to delete"
                    }
                    val removedContentChannel =
                        discordApi.getTextChannelById(chadConfig.removedContentChannelId).toNullable()
                            ?: return@reply "uhoh !"
                    val server = discordApi.getServerById(chadConfig.serverId).toNullable() ?: return@reply "uhoh !"
                    target.reply("Message deleted by <@!${message.author.id}>: \"$realReason\"")
                        .thenCompose {
                            val displayName = target.userAuthor.toNullable()?.getNickname(server)?.toNullable()
                            val embed = EmbedBuilder().apply {
                                setAuthor(
                                    "ORE Moderation Services",
                                    "https://youtu.be/dQw4w9WgXcQ",
                                    "https://openredstone.org/wp-content/uploads/2018/07/icon-mini.png"
                                )
                                addField("Staff Member", "<@!${message.author.id}>")
                                if (displayName != null) {
                                    addInlineField("User", "<@!${target.author.id}>")
                                    addInlineField("Display Name", displayName)
                                } else {
                                    addField("User", "<@!${target.author.id}>")
                                }
                                addField("Reason", realReason)
                                addInlineField("Channel", "<#${it.channel.id}>")
                                addInlineField(
                                    "Context",
                                    "https://discord.com/channels/${chadConfig.serverId}/" +
                                            "${chadConfig.removedContentChannelId}/${it.id}"
                                )
                                setColor(Color.RED)
                                setFooter("FootORE")
                                setThumbnail("https://cdn.discordapp.com/emojis/892499052942463027.webp")
                            }
                            MessageBuilder().copy(target).addEmbed(embed).send(removedContentChannel)
                        }
                        .thenCompose {
                            target.delete()
                        }
                        .thenCompose {
                            message.delete()
                        }
                    "" // totally a bad hack for now
                }
            })
            put("pikl", command(authorizedRoles) {
                val name by required()
                fun parseId() = Regex("""<@!?([0-9]{10,20})>""").find(name)?.groupValues?.last()
                fun gimmiePikl() = discordServer.getRolesByName("pikl")?.firstOrNull()
                reply {
                    val piklRole = gimmiePikl() ?: return@reply "No pikl rank :("
                    val discordId = parseId() ?: return@reply "Invalid user."
                    val user = discordApi.getUserById(discordId).get()
                    val roles = user.getRoles(discordServer)
                    if (roles.none { role -> role.name == "pikl" }) {
                        user.addRole(piklRole)
                    }
                    Timer().schedule(120000) {
                        user.removeRole(piklRole)
                    }
                    "<@${discordId}> got pikl'd."
                }
            })
            put("authorized", command(authorizedRoles) {
                reply { "authorized !" }
            })
            put("reload", command(authorizedRoles) {
                reply {
                    reloadCommands()
                    "Done!"
                }
            })
            putAll(database.getCommands().mapValues { staticCommand(it.value) })
            put("help", helpCommand(this))
        }
    }

    reloadCommands()

    logger.info("Loaded the following Discord commands: ${commands.keys.joinToString()}")
    logger.info("Starting listeners...")

    startDiscordListeners(
        discordApi,
        CommandExecutor(chadConfig.commandChar, commands),
        chadConfig.disableSpoilers,
        chadConfig.welcomeChannelId,
        chadConfig.greetings,
        chadConfig.ingameBotRoleId,
        chadConfig.gameChatChannelId,
    )

    if (chadConfig.enableNotificationRoles) NotificationManager(
        discordApi,
        chadConfig.notificationChannelId,
        chadConfig.notifications,
    )

    logger.info("Done")
}
