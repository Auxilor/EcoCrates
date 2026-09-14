package com.willfp.ecocrates.commands.target

import com.willfp.ecocrates.commands.target.TargetParseResult.Failure
import com.willfp.ecocrates.commands.target.TargetParseResult.Success
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class TargetParserTest {
    private val giveTypes = setOf(TargetType.KEY, TargetType.ENVOY, TargetType.POUCH)
    private val previewTypes = setOf(TargetType.CRATE, TargetType.POUCH)

    private fun give(vararg args: String) = TargetParser.parse(args.toList(), giveTypes, LegacySyntax.GIVE)
    private fun preview(vararg args: String) = TargetParser.parse(args.toList(), previewTypes, LegacySyntax.PREVIEW)
    private fun ok(result: TargetParseResult) = assertIs<Success>(result).target
    private fun err(result: TargetParseResult) = assertIs<Failure>(result).error

    @Test
    fun `key with variant and amount`() {
        assertEquals(
            ParsedTarget(TargetType.KEY, "demo_key", "physical", 5, isLegacy = false, keyViaCrate = false),
            ok(give("key", "demo_key", "physical", "5"))
        )
    }

    @Test
    fun `key defaults to virtual and amount can follow the id directly`() {
        assertEquals(
            ParsedTarget(TargetType.KEY, "demo_key", "virtual", 3, isLegacy = false, keyViaCrate = false),
            ok(give("key", "demo_key", "3"))
        )
    }

    @Test
    fun `type token is case insensitive and variant is normalised`() {
        val target = ok(give("KEY", "demo_key", "Physical"))
        assertEquals(TargetType.KEY, target.type)
        assertEquals("physical", target.variant)
        assertEquals(1, target.amount)
    }

    @Test
    fun `envoy requires a variant`() {
        assertEquals(TargetParseError.MISSING_VARIANT, err(give("envoy", "airdrop")))
        assertEquals(TargetParseError.INVALID_VARIANT, err(give("envoy", "airdrop", "physical")))
        assertEquals("compass", ok(give("envoy", "airdrop", "compass", "2")).variant)
    }

    @Test
    fun `pouch takes no variant`() {
        assertEquals(
            ParsedTarget(TargetType.POUCH, "mining", null, 4, isLegacy = false, keyViaCrate = false),
            ok(give("pouch", "mining", "4"))
        )
    }

    @Test
    fun `bad or missing amount becomes one`() {
        assertEquals(1, ok(give("pouch", "mining", "lots")).amount)
        assertEquals(1, ok(give("pouch", "mining", "-3")).amount)
    }

    @Test
    fun `missing pieces are reported`() {
        assertEquals(TargetParseError.MISSING_TYPE, err(give()))
        assertEquals(TargetParseError.MISSING_ID, err(give("pouch")))
    }

    @Test
    fun `type not allowed for this command`() {
        assertEquals(TargetParseError.INVALID_TYPE, err(give("crate", "demo")))
        assertEquals(TargetParseError.INVALID_TYPE, err(preview("key", "demo_key")))
    }

    @Test
    fun `legacy give defaults to a virtual key via the crate`() {
        assertEquals(
            ParsedTarget(TargetType.KEY, "demo_crate", "virtual", 1, isLegacy = true, keyViaCrate = true),
            ok(give("demo_crate"))
        )
    }

    @Test
    fun `legacy give physical with amount`() {
        assertEquals(
            ParsedTarget(TargetType.KEY, "demo_crate", "physical", 5, isLegacy = true, keyViaCrate = true),
            ok(give("demo_crate", "physical", "5"))
        )
    }

    @Test
    fun `legacy give envoy item`() {
        assertEquals(
            ParsedTarget(TargetType.ENVOY, "airdrop", "flare", 2, isLegacy = true, keyViaCrate = false),
            ok(give("airdrop", "flare", "2"))
        )
    }

    @Test
    fun `legacy preview is a crate`() {
        assertEquals(
            ParsedTarget(TargetType.CRATE, "demo_crate", null, 1, isLegacy = true, keyViaCrate = false),
            ok(preview("demo_crate"))
        )
    }

    @Test
    fun `no legacy support means unknown token is invalid type`() {
        assertEquals(
            TargetParseError.INVALID_TYPE,
            err(TargetParser.parse(listOf("demo_crate"), giveTypes, LegacySyntax.NONE))
        )
    }

    @Test
    fun `id equal to a type token parses as new syntax`() {
        assertEquals(TargetParseError.MISSING_ID, err(preview("crate")))
    }

    @Test
    fun `format builds the equivalent new command`() {
        val target = ParsedTarget(TargetType.KEY, "demo_key", "physical", 5, isLegacy = true, keyViaCrate = false)
        assertEquals("/ecocrates give Steve key demo_key physical 5", TargetParser.format("give", "Steve", target, true))

        val pouch = ParsedTarget(TargetType.POUCH, "mining", null, 1, isLegacy = false, keyViaCrate = false)
        assertEquals("/ecocrates giveall pouch mining 1", TargetParser.format("giveall", null, pouch, true))
        assertEquals("/ecocrates preview pouch mining", TargetParser.format("preview", null, pouch, false))
    }
}
