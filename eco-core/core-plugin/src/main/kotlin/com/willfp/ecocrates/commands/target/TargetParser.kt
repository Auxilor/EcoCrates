package com.willfp.ecocrates.commands.target

/** What a give/take/preview command acts on, as typed in the type slot. */
enum class TargetType(
    val token: String,
    val variants: List<String>,
    val variantRequired: Boolean,
    val defaultVariant: String?
) {
    KEY("key", listOf("physical", "virtual"), false, "virtual"),
    ENVOY("envoy", listOf("flare", "compass"), true, null),
    POUCH("pouch", emptyList(), false, null),
    CRATE("crate", emptyList(), false, null);

    companion object {
        fun fromToken(raw: String?): TargetType? =
            raw?.let { token -> entries.firstOrNull { it.token.equals(token, ignoreCase = true) } }
    }
}

/** Which pre-typed syntax, if any, a command still accepts. */
enum class LegacySyntax {
    /** `<crate> [physical|virtual|flare|compass] [amount]` */
    GIVE,

    /** `<crate>` */
    PREVIEW,

    NONE
}

/**
 * @param keyViaCrate True for legacy key commands, where [id] is a crate ID
 * whose shared key must be looked up.
 */
data class ParsedTarget(
    val type: TargetType,
    val id: String,
    val variant: String?,
    val amount: Int,
    val isLegacy: Boolean,
    val keyViaCrate: Boolean
)

enum class TargetParseError {
    MISSING_TYPE,
    INVALID_TYPE,
    MISSING_ID,
    MISSING_VARIANT,
    INVALID_VARIANT,
    INVALID_AMOUNT
}

sealed interface TargetParseResult {
    data class Success(val target: ParsedTarget) : TargetParseResult

    /** [type] is set when the error concerns a known type, e.g. which variants it accepts. */
    data class Failure(val error: TargetParseError, val type: TargetType? = null) : TargetParseResult
}

/**
 * Parses the `<type> <id> [variant] [amount]` part of a command, falling back
 * to the old untyped syntax when the first argument isn't a type token.
 * Doesn't check that IDs exist - that's up to the caller.
 */
object TargetParser {
    private val legacyEnvoyVariants = TargetType.ENVOY.variants

    fun parse(args: List<String>, allowed: Set<TargetType>, legacy: LegacySyntax): TargetParseResult {
        val first = args.firstOrNull() ?: return TargetParseResult.Failure(TargetParseError.MISSING_TYPE)

        val type = TargetType.fromToken(first)

        if (type == null) {
            return when (legacy) {
                LegacySyntax.GIVE -> parseLegacyGive(args)
                LegacySyntax.PREVIEW -> TargetParseResult.Success(
                    ParsedTarget(TargetType.CRATE, first, null, 1, isLegacy = true, keyViaCrate = false)
                )
                LegacySyntax.NONE -> TargetParseResult.Failure(TargetParseError.INVALID_TYPE)
            }
        }

        if (type !in allowed) {
            return TargetParseResult.Failure(TargetParseError.INVALID_TYPE, type)
        }

        val id = args.getOrNull(1) ?: return TargetParseResult.Failure(TargetParseError.MISSING_ID, type)

        if (type.variants.isEmpty()) {
            val amount = parseAmount(args.getOrNull(2))
                ?: return TargetParseResult.Failure(TargetParseError.INVALID_AMOUNT, type)
            return TargetParseResult.Success(
                ParsedTarget(type, id, null, amount, isLegacy = false, keyViaCrate = false)
            )
        }

        val rawVariant = args.getOrNull(2)
        val variant = type.variants.firstOrNull { it.equals(rawVariant, ignoreCase = true) }

        if (variant != null) {
            val amount = parseAmount(args.getOrNull(3))
                ?: return TargetParseResult.Failure(TargetParseError.INVALID_AMOUNT, type)
            return TargetParseResult.Success(
                ParsedTarget(type, id, variant, amount, isLegacy = false, keyViaCrate = false)
            )
        }

        if (type.variantRequired) {
            val error = if (rawVariant == null) TargetParseError.MISSING_VARIANT else TargetParseError.INVALID_VARIANT
            return TargetParseResult.Failure(error, type)
        }

        // Optional variant omitted: whatever follows the ID is the amount.
        val amount = parseAmount(rawVariant)
            ?: return TargetParseResult.Failure(TargetParseError.INVALID_AMOUNT, type)
        return TargetParseResult.Success(
            ParsedTarget(type, id, type.defaultVariant, amount, isLegacy = false, keyViaCrate = false)
        )
    }

    private fun parseLegacyGive(args: List<String>): TargetParseResult {
        val id = args[0]
        val rawVariant = args.getOrNull(1)

        val envoyVariant = legacyEnvoyVariants.firstOrNull { it.equals(rawVariant, ignoreCase = true) }

        if (envoyVariant != null) {
            val amount = parseAmount(args.getOrNull(2))
                ?: return TargetParseResult.Failure(TargetParseError.INVALID_AMOUNT, TargetType.ENVOY)
            return TargetParseResult.Success(
                ParsedTarget(TargetType.ENVOY, id, envoyVariant, amount, isLegacy = true, keyViaCrate = false)
            )
        }

        val keyVariant = if (rawVariant.equals("physical", ignoreCase = true)) "physical" else "virtual"
        val amount = parseAmount(args.getOrNull(2))
            ?: return TargetParseResult.Failure(TargetParseError.INVALID_AMOUNT, TargetType.KEY)

        return TargetParseResult.Success(
            ParsedTarget(TargetType.KEY, id, keyVariant, amount, isLegacy = true, keyViaCrate = true)
        )
    }

    /** @return 1 if [raw] is absent, the parsed value if it's a valid amount (>= 1), or null if it's present but invalid. */
    private fun parseAmount(raw: String?): Int? =
        if (raw == null) 1 else raw.toIntOrNull()?.takeIf { it >= 1 }

    /** The new-syntax command equivalent to [target], for deprecation messages. */
    fun format(subcommand: String, playerArg: String?, target: ParsedTarget, includeAmount: Boolean): String =
        listOfNotNull(
            "/ecocrates",
            subcommand,
            playerArg,
            target.type.token,
            target.id,
            target.variant,
            if (includeAmount) target.amount.toString() else null
        ).joinToString(" ")
}
