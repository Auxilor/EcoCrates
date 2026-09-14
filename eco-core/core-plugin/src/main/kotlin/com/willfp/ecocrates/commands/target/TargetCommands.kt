package com.willfp.ecocrates.commands.target

import com.willfp.ecocrates.crate.Crates
import com.willfp.ecocrates.plugin
import org.bukkit.command.CommandSender
import org.bukkit.util.StringUtil

/** A parsed target whose legacy crate ID has been swapped for its key ID, and which has been validated. */
data class ResolvedTarget(
    val target: GiveTarget,
    val parsed: ParsedTarget
)

/** Bukkit-side glue between commands and [TargetParser]: messages, validation, tab completion. */
object TargetCommands {
    private val amounts = listOf("1", "2", "3", "4", "5", "10")

    /** @return The parsed target, or null after telling [sender] what was wrong. */
    fun parse(
        sender: CommandSender,
        args: List<String>,
        allowed: Set<TargetType>,
        legacy: LegacySyntax
    ): ParsedTarget? =
        when (val result = TargetParser.parse(args, allowed, legacy)) {
            is TargetParseResult.Success -> result.target
            is TargetParseResult.Failure -> {
                sender.sendMessage(errorMessage(result, allowed))
                null
            }
        }

    private fun errorMessage(failure: TargetParseResult.Failure, allowed: Set<TargetType>): String {
        val types = allowed.joinToString(", ") { it.token }

        return when (failure.error) {
            TargetParseError.MISSING_TYPE -> plugin.langYml.getMessage("must-specify-type").replace("%types%", types)
            TargetParseError.INVALID_TYPE -> plugin.langYml.getMessage("invalid-type").replace("%types%", types)
            TargetParseError.MISSING_ID -> plugin.langYml.getMessage("must-specify-id")
            TargetParseError.MISSING_VARIANT, TargetParseError.INVALID_VARIANT ->
                plugin.langYml.getMessage("invalid-variant")
                    .replace("%variants%", failure.type?.variants?.joinToString(", ") ?: "")
            TargetParseError.INVALID_AMOUNT -> plugin.langYml.getMessage("invalid-amount")
        }
    }

    /** @return The resolved target, or null after telling [sender] what was wrong. */
    fun resolveGive(sender: CommandSender, parsed: ParsedTarget): ResolvedTarget? {
        val normalised = if (parsed.keyViaCrate) {
            val crate = Crates.getByID(parsed.id)

            if (crate == null) {
                sender.sendMessage(plugin.langYml.getMessage("invalid-crate"))
                return null
            }

            parsed.copy(id = crate.sharedKey.id, keyViaCrate = false)
        } else {
            parsed
        }

        val target = GiveTargets.forType(normalised.type)

        if (target == null) {
            sender.sendMessage(
                plugin.langYml.getMessage("invalid-type")
                    .replace("%types%", GiveTargets.giveTypes.joinToString(", ") { it.token })
            )
            return null
        }

        val error = target.validationError(normalised.id, normalised.variant)

        if (error != null) {
            sender.sendMessage(error)
            return null
        }

        return ResolvedTarget(target, normalised)
    }

    fun warnIfLegacy(
        sender: CommandSender,
        subcommand: String,
        playerArg: String?,
        target: ParsedTarget,
        includeAmount: Boolean
    ) {
        if (!target.isLegacy) {
            return
        }

        sender.sendMessage(
            plugin.langYml.getMessage("deprecated-command-syntax")
                .replace("%command%", TargetParser.format(subcommand, playerArg, target, includeAmount))
        )
    }

    fun idsFor(type: TargetType): List<String> =
        GiveTargets.forType(type)?.ids() ?: Crates.values().map { it.id }

    /** Completes `<type> <id> [variant] [amount]`. [args] excludes any player argument. */
    fun tabComplete(args: List<String>, allowed: Set<TargetType>, includeAmounts: Boolean): List<String> {
        if (args.isEmpty()) {
            return allowed.map { it.token }
        }

        val type = TargetType.fromToken(args[0])?.takeIf { it in allowed }
        val amountOptions = if (includeAmounts) amounts else emptyList()

        val options = when (args.size) {
            1 -> allowed.map { it.token }
            2 -> type?.let { idsFor(it) } ?: emptyList()
            3 -> when {
                type == null -> emptyList()
                type.variants.isEmpty() -> amountOptions
                type.variantRequired -> type.variants
                else -> type.variants + amountOptions
            }
            4 -> if (type != null && type.variants.isNotEmpty()) amountOptions else emptyList()
            else -> emptyList()
        }

        return StringUtil.copyPartialMatches(args.last(), options, mutableListOf())
    }
}
