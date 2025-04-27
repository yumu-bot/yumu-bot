package com.now.nowbot.model

import com.fasterxml.jackson.annotation.*
import com.fasterxml.jackson.databind.JsonNode
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.throwable.ModsException
import org.spring.core.json
import kotlin.reflect.full.companionObjectInstance


sealed interface Mod {
    val type: String

    /**
     * mode 跟 incompatible 不知道用不用得到, 用不到就删了
     */
    val mode: Set<OsuMode>

    @get:JsonIgnore
    val incompatible: Set<Mod>
}

sealed interface ValueMod {
    val value: Int

    operator fun plus(other: ValueMod): Int {
        return value or other.value
    }

    operator fun Int.plus(other: ValueMod): Int {
        return this or other.value
    }
}

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "acronym")
@JsonSubTypes(
    JsonSubTypes.Type(value = LazerMod.Easy::class, name = "EZ"),
    JsonSubTypes.Type(value = LazerMod.NoFail::class, name = "NF"),
    JsonSubTypes.Type(value = LazerMod.HalfTime::class, name = "HT"),
    JsonSubTypes.Type(value = LazerMod.Daycore::class, name = "DC"),
    JsonSubTypes.Type(value = LazerMod.HardRock::class, name = "HR"),
    JsonSubTypes.Type(value = LazerMod.SuddenDeath::class, name = "SD"),
    JsonSubTypes.Type(value = LazerMod.Perfect::class, name = "PF"),
    JsonSubTypes.Type(value = LazerMod.DoubleTime::class, name = "DT"),
    JsonSubTypes.Type(value = LazerMod.Nightcore::class, name = "NC"),
    JsonSubTypes.Type(value = LazerMod.Hidden::class, name = "HD"),
    JsonSubTypes.Type(value = LazerMod.Flashlight::class, name = "FL"),
    JsonSubTypes.Type(value = LazerMod.Blinds::class, name = "BL"),
    JsonSubTypes.Type(value = LazerMod.StrictTracking::class, name = "ST"),
    JsonSubTypes.Type(value = LazerMod.AccuracyChallenge::class, name = "AC"),
    JsonSubTypes.Type(value = LazerMod.TargetPractice::class, name = "TP"),
    JsonSubTypes.Type(value = LazerMod.DifficultyAdjust::class, name = "DA"),
    JsonSubTypes.Type(value = LazerMod.Classic::class, name = "CL"),
    JsonSubTypes.Type(value = LazerMod.Random::class, name = "RD"),
    JsonSubTypes.Type(value = LazerMod.Mirror::class, name = "MR"),
    JsonSubTypes.Type(value = LazerMod.Alternate::class, name = "AL"),
    JsonSubTypes.Type(value = LazerMod.SingleTap::class, name = "SG"),
    JsonSubTypes.Type(value = LazerMod.Autoplay::class, name = "AT"),
    JsonSubTypes.Type(value = LazerMod.Cinema::class, name = "CN"),
    JsonSubTypes.Type(value = LazerMod.Relax::class, name = "RX"),
    JsonSubTypes.Type(value = LazerMod.Autopilot::class, name = "AP"),
    JsonSubTypes.Type(value = LazerMod.SpunOut::class, name = "SO"),
    JsonSubTypes.Type(value = LazerMod.Transform::class, name = "TR"),
    JsonSubTypes.Type(value = LazerMod.Wiggle::class, name = "WG"),
    JsonSubTypes.Type(value = LazerMod.SpinIn::class, name = "SI"),
    JsonSubTypes.Type(value = LazerMod.Grow::class, name = "GR"),
    JsonSubTypes.Type(value = LazerMod.Deflate::class, name = "DF"),
    JsonSubTypes.Type(value = LazerMod.WindUp::class, name = "WU"),
    JsonSubTypes.Type(value = LazerMod.WindDown::class, name = "WD"),
    JsonSubTypes.Type(value = LazerMod.Traceable::class, name = "TC"),
    JsonSubTypes.Type(value = LazerMod.BarrelRoll::class, name = "BR"),
    JsonSubTypes.Type(value = LazerMod.ApproachDifferent::class, name = "AD"),
    JsonSubTypes.Type(value = LazerMod.Muted::class, name = "MU"),
    JsonSubTypes.Type(value = LazerMod.NoScope::class, name = "NS"),
    JsonSubTypes.Type(value = LazerMod.Magnetised::class, name = "MG"),
    JsonSubTypes.Type(value = LazerMod.Repel::class, name = "RP"),
    JsonSubTypes.Type(value = LazerMod.AdaptiveSpeed::class, name = "AS"),
    JsonSubTypes.Type(value = LazerMod.FreezeFrame::class, name = "FR"),
    JsonSubTypes.Type(value = LazerMod.Bubbles::class, name = "BU"),
    JsonSubTypes.Type(value = LazerMod.Synesthesia::class, name = "SY"),
    JsonSubTypes.Type(value = LazerMod.Depth::class, name = "DP"),
    JsonSubTypes.Type(value = LazerMod.TouchDevice::class, name = "TD"),
    JsonSubTypes.Type(value = LazerMod.Bloom::class, name = "BM"),
    JsonSubTypes.Type(value = LazerMod.ScoreV2::class, name = "SV2"),
    JsonSubTypes.Type(value = LazerMod.Swap::class, name = "SW"),
    JsonSubTypes.Type(value = LazerMod.ConstantSpeed::class, name = "CS"),
    JsonSubTypes.Type(value = LazerMod.FloatingFruits::class, name = "FF"),
    JsonSubTypes.Type(value = LazerMod.NoRelease::class, name = "NR"),
    JsonSubTypes.Type(value = LazerMod.FadeIn::class, name = "FI"),
    JsonSubTypes.Type(value = LazerMod.Cover::class, name = "CO"),
    JsonSubTypes.Type(value = LazerMod.DualStages::class, name = "DS"),
    JsonSubTypes.Type(value = LazerMod.Invert::class, name = "IN"),
    JsonSubTypes.Type(value = LazerMod.HoldOff::class, name = "HO"),
    JsonSubTypes.Type(value = LazerMod.Key1::class, name = "1K"),
    JsonSubTypes.Type(value = LazerMod.Key2::class, name = "2K"),
    JsonSubTypes.Type(value = LazerMod.Key3::class, name = "3K"),
    JsonSubTypes.Type(value = LazerMod.Key4::class, name = "4K"),
    JsonSubTypes.Type(value = LazerMod.Key5::class, name = "5K"),
    JsonSubTypes.Type(value = LazerMod.Key6::class, name = "6K"),
    JsonSubTypes.Type(value = LazerMod.Key7::class, name = "7K"),
    JsonSubTypes.Type(value = LazerMod.Key8::class, name = "8K"),
    JsonSubTypes.Type(value = LazerMod.Key9::class, name = "9K"),
    JsonSubTypes.Type(value = LazerMod.Key10::class, name = "10K"),
    JsonSubTypes.Type(value = LazerMod.NoMod::class, name = "NM"),
    JsonSubTypes.Type(value = LazerMod.None::class, name = ""),
)
@JsonInclude(JsonInclude.Include.NON_NULL)
sealed class LazerMod {
    @get:JsonProperty("acronym")
    abstract val acronym: String

    @get:JsonProperty("settings")
    abstract val settings: Any?

    @get:JsonProperty("color")
    abstract val color: String

    class Easy(
        retries: Float? = null,
    ) : LazerMod() {
        @get:JsonProperty("acronym")
        override val acronym: String = type

        @get:JsonProperty("settings")
        override var settings: Any? = null

        @get:JsonProperty("color")
        override val color: String = "#22AC38"

        @JsonProperty("settings")
        private fun setSettings(node: JsonNode) {
            settings = node.json<Value>()
        }

        @get:JsonIgnore
        @set:JsonIgnore
        var retries: Float?
            get() = settings?.let { (it as Value).retries }
            set(v) {
                if (settings == null) settings = Value(retries = v)
                else (settings as Value).retries = v
            }

        private data class Value(
            @JsonProperty("retries") var retries: Float? = null,
        )

        init {
            retries?.let { this.retries = it }
        }

        companion object : Mod, ValueMod {
            override val type: String = "EZ"
            override val mode: Set<OsuMode> = setOf(OsuMode.OSU, OsuMode.TAIKO, OsuMode.CATCH, OsuMode.MANIA)
            override val incompatible: Set<Mod> = setOf(HardRock, AccuracyChallenge, DifficultyAdjust)
            override val value: Int = 2
        }
    }

    class NoFail : LazerMod() {
        @get:JsonProperty("acronym")
        override val acronym: String = type

        @get:JsonProperty("settings")
        override var settings: Any? = null

        @get:JsonProperty("color")
        override val color: String = "#0068B7"

        companion object : Mod, ValueMod {
            override val type: String = "NF"
            override val mode: Set<OsuMode> = setOf(OsuMode.OSU, OsuMode.TAIKO, OsuMode.CATCH, OsuMode.MANIA)
            override val incompatible: Set<Mod> = setOf(SuddenDeath, Perfect, AccuracyChallenge, Cinema)
            override val value: Int = 1
        }
    }

    class HalfTime(
        speedChange: Float? = null,
        adjustPitch: Boolean? = null,
    ) : LazerMod() {
        @get:JsonProperty("acronym")
        override val acronym: String = type

        @get:JsonProperty("settings")
        override var settings: Any? = null

        @get:JsonProperty("color")
        override val color: String = "#BDBDBD"

        @JsonProperty("settings")
        private fun setSettings(node: JsonNode) {
            settings = node.json<Value>()
        }

        @get:JsonIgnore
        @set:JsonIgnore
        var speedChange: Float?
            get() = settings?.let { (it as Value).speedChange }
            set(v) {
                if (settings == null) settings = Value(speedChange = v)
                else (settings as Value).speedChange = v
            }

        @get:JsonIgnore
        @set:JsonIgnore
        var adjustPitch: Boolean?
            get() = settings?.let { (it as Value).adjustPitch }
            set(v) {
                if (settings == null) settings = Value(adjustPitch = v)
                else (settings as Value).adjustPitch = v
            }

        private data class Value(
            @JsonProperty("speed_change") var speedChange: Float? = null,
            @JsonProperty("adjust_pitch") var adjustPitch: Boolean? = null,
        )

        init {
            speedChange?.let { this.speedChange = it }
            adjustPitch?.let { this.adjustPitch = it }
        }

        companion object : Mod, ValueMod {
            override val type: String = "HT"
            override val mode: Set<OsuMode> = setOf(OsuMode.OSU, OsuMode.TAIKO, OsuMode.CATCH, OsuMode.MANIA)
            override val incompatible: Set<Mod> =
                setOf(HalfTime, Daycore, DoubleTime, Nightcore, WindUp, WindDown, AdaptiveSpeed)
            override val value: Int = 256
        }
    }

    class Daycore(
        speedChange: Float? = null,
    ) : LazerMod() {
        @get:JsonProperty("acronym")
        override val acronym: String = type

        @get:JsonProperty("settings")
        override var settings: Any? = null

        @get:JsonProperty("color")
        override val color: String = "#DADADA"

        @JsonProperty("settings")
        private fun setSettings(node: JsonNode) {
            settings = node.json<Value>()
        }

        @get:JsonIgnore
        @set:JsonIgnore
        var speedChange: Float?
            get() = settings?.let { (it as Value).speedChange }
            set(v) {
                if (settings == null) settings = Value(speedChange = v)
                else (settings as Value).speedChange = v
            }

        private data class Value(
            @JsonProperty("speed_change") var speedChange: Float? = null,
        )

        init {
            speedChange?.let { this.speedChange = it }
        }

        companion object : Mod {
            override val type: String = "DC"
            override val mode: Set<OsuMode> = setOf(OsuMode.OSU, OsuMode.TAIKO, OsuMode.CATCH, OsuMode.MANIA)
            override val incompatible: Set<Mod> =
                setOf(HalfTime, Daycore, DoubleTime, Nightcore, WindUp, WindDown, AdaptiveSpeed)

        }
    }

    class HardRock : LazerMod() {
        @get:JsonProperty("acronym")
        override val acronym: String = type

        @get:JsonProperty("settings")
        override var settings: Any? = null

        @get:JsonProperty("color")
        override val color: String = "#D32F2F"

        companion object : Mod, ValueMod {
            override val type: String = "HR"
            override val mode: Set<OsuMode> = setOf(OsuMode.OSU, OsuMode.TAIKO, OsuMode.CATCH, OsuMode.MANIA)
            override val incompatible: Set<Mod> = setOf(Easy, DifficultyAdjust, Mirror)
            override val value: Int = 16
        }
    }

    class SuddenDeath(
        restart: Boolean? = null,
    ) : LazerMod() {
        @get:JsonProperty("acronym")
        override val acronym: String = type

        @get:JsonProperty("settings")
        override var settings: Any? = null

        @get:JsonProperty("color")
        override val color: String = "#FF9800"

        @JsonProperty("settings")
        private fun setSettings(node: JsonNode) {
            settings = node.json<Value>()
        }

        @get:JsonIgnore
        @set:JsonIgnore
        var restart: Boolean?
            get() = settings?.let { (it as Value).restart }
            set(v) {
                if (settings == null) settings = Value(restart = v)
                else (settings as Value).restart = v
            }

        private data class Value(
            @JsonProperty("restart") var restart: Boolean? = null,
        )

        init {
            restart?.let { this.restart = it }
        }

        companion object : Mod, ValueMod {
            override val type: String = "SD"
            override val mode: Set<OsuMode> = setOf(OsuMode.OSU, OsuMode.TAIKO, OsuMode.CATCH, OsuMode.MANIA)
            override val incompatible: Set<Mod> = setOf(NoFail, Perfect, TargetPractice, Cinema)
            override val value: Int = 32
        }
    }

    class Perfect(
        restart: Boolean? = null,
    ) : LazerMod() {
        @get:JsonProperty("acronym")
        override val acronym: String = type

        @get:JsonProperty("settings")
        override var settings: Any? = null

        @get:JsonProperty("color")
        override val color: String = "#FFF100"

        @JsonProperty("settings")
        private fun setSettings(node: JsonNode) {
            settings = node.json<Value>()
        }

        @get:JsonIgnore
        @set:JsonIgnore
        var restart: Boolean?
            get() = settings?.let { (it as Value).restart }
            set(v) {
                if (settings == null) settings = Value(restart = v)
                else (settings as Value).restart = v
            }

        private data class Value(
            @JsonProperty("restart") var restart: Boolean? = null,
        )

        init {
            restart?.let { this.restart = it }
        }

        companion object : Mod, ValueMod {
            override val type: String = "PF"
            override val mode: Set<OsuMode> = setOf(OsuMode.OSU, OsuMode.TAIKO, OsuMode.CATCH, OsuMode.MANIA)
            override val incompatible: Set<Mod> = setOf(NoFail, SuddenDeath, AccuracyChallenge, Cinema)
            override val value: Int = 16384
        }
    }

    class DoubleTime(
        speedChange: Float? = null,
        adjustPitch: Boolean? = null,
    ) : LazerMod() {
        @get:JsonProperty("acronym")
        override val acronym: String = type

        @get:JsonProperty("settings")
        override var settings: Any? = null

        @get:JsonProperty("color")
        override val color: String = "#00A0E9"

        @JsonProperty("settings")
        private fun setSettings(node: JsonNode) {
            settings = node.json<Value>()
        }

        @get:JsonIgnore
        @set:JsonIgnore
        var speedChange: Float?
            get() = settings?.let { (it as Value).speedChange }
            set(v) {
                if (settings == null) settings = Value(speedChange = v)
                else (settings as Value).speedChange = v
            }

        @get:JsonIgnore
        @set:JsonIgnore
        var adjustPitch: Boolean?
            get() = settings?.let { (it as Value).adjustPitch }
            set(v) {
                if (settings == null) settings = Value(adjustPitch = v)
                else (settings as Value).adjustPitch = v
            }

        private data class Value(
            @JsonProperty("speed_change") var speedChange: Float? = null,
            @JsonProperty("adjust_pitch") var adjustPitch: Boolean? = null,
        )

        init {
            speedChange?.let { this.speedChange = it }
            adjustPitch?.let { this.adjustPitch = it }
        }

        companion object : Mod, ValueMod {
            override val type: String = "DT"
            override val mode: Set<OsuMode> = setOf(OsuMode.OSU, OsuMode.TAIKO, OsuMode.CATCH, OsuMode.MANIA)
            override val incompatible: Set<Mod> =
                setOf(HalfTime, Daycore, DoubleTime, Nightcore, WindUp, WindDown, AdaptiveSpeed)
            override val value: Int = 64
        }
    }

    class Nightcore(
        speedChange: Float? = null,
    ) : LazerMod() {
        @get:JsonProperty("acronym")
        override val acronym: String = type

        @get:JsonProperty("settings")
        override var settings: Any? = null

        @get:JsonProperty("color")
        override val color: String = "#9922EE"

        @JsonProperty("settings")
        private fun setSettings(node: JsonNode) {
            settings = node.json<Value>()
        }

        @get:JsonIgnore
        @set:JsonIgnore
        var speedChange: Float?
            get() = settings?.let { (it as Value).speedChange }
            set(v) {
                if (settings == null) settings = Value(speedChange = v)
                else (settings as Value).speedChange = v
            }

        private data class Value(
            @JsonProperty("speed_change") var speedChange: Float? = null,
        )

        init {
            speedChange?.let { this.speedChange = it }
        }

        companion object : Mod, ValueMod {
            override val type: String = "NC"
            override val mode: Set<OsuMode> = setOf(OsuMode.OSU, OsuMode.TAIKO, OsuMode.CATCH, OsuMode.MANIA)
            override val incompatible: Set<Mod> =
                setOf(HalfTime, Daycore, DoubleTime, Nightcore, WindUp, WindDown, AdaptiveSpeed)
            override val value: Int = 512
        }
    }

    class Hidden(
        onlyFadeApproachCircles: Boolean? = null,
    ) : LazerMod() {
        @get:JsonProperty("acronym")
        override val acronym: String = type

        @get:JsonProperty("settings")
        override var settings: Any? = null

        @get:JsonProperty("color")
        override val color: String = "#F8B551"

        @JsonProperty("settings")
        private fun setSettings(node: JsonNode) {
            settings = node.json<Value>()
        }

        @get:JsonIgnore
        @set:JsonIgnore
        var onlyFadeApproachCircles: Boolean?
            get() = settings?.let { (it as Value).onlyFadeApproachCircles }
            set(v) {
                if (settings == null) settings = Value(onlyFadeApproachCircles = v)
                else (settings as Value).onlyFadeApproachCircles = v
            }

        private data class Value(
            @JsonProperty("only_fade_approach_circles") var onlyFadeApproachCircles: Boolean? = null,
        )

        init {
            onlyFadeApproachCircles?.let { this.onlyFadeApproachCircles = it }
        }

        companion object : Mod, ValueMod {
            override val type: String = "HD"
            override val mode: Set<OsuMode> = setOf(OsuMode.OSU, OsuMode.TAIKO, OsuMode.CATCH, OsuMode.MANIA)
            override val incompatible: Set<Mod> =
                setOf(SpinIn, Traceable, ApproachDifferent, Depth, FadeIn, Cover, Flashlight)
            override val value: Int = 8
        }
    }

    class Flashlight(
        followDelay: Float? = null,
        sizeMultiplier: Float? = null,
        comboBasedSize: Boolean? = null,
    ) : LazerMod() {
        @get:JsonProperty("acronym")
        override val acronym: String = type

        @get:JsonProperty("settings")
        override var settings: Any? = null

        @get:JsonProperty("color")
        override val color: String = "#000000"

        @JsonProperty("settings")
        private fun setSettings(node: JsonNode) {
            settings = node.json<Value>()
        }

        @get:JsonIgnore
        @set:JsonIgnore
        var followDelay: Float?
            get() = settings?.let { (it as Value).followDelay }
            set(v) {
                if (settings == null) settings = Value(followDelay = v)
                else (settings as Value).followDelay = v
            }

        @get:JsonIgnore
        @set:JsonIgnore
        var sizeMultiplier: Float?
            get() = settings?.let { (it as Value).sizeMultiplier }
            set(v) {
                if (settings == null) settings = Value(sizeMultiplier = v)
                else (settings as Value).sizeMultiplier = v
            }

        @get:JsonIgnore
        @set:JsonIgnore
        var comboBasedSize: Boolean?
            get() = settings?.let { (it as Value).comboBasedSize }
            set(v) {
                if (settings == null) settings = Value(comboBasedSize = v)
                else (settings as Value).comboBasedSize = v
            }

        private data class Value(
            @JsonProperty("follow_delay") var followDelay: Float? = null,
            @JsonProperty("size_multiplier") var sizeMultiplier: Float? = null,
            @JsonProperty("combo_based_size") var comboBasedSize: Boolean? = null,
        )

        init {
            followDelay?.let { this.followDelay = it }
            sizeMultiplier?.let { this.sizeMultiplier = it }
            comboBasedSize?.let { this.comboBasedSize = it }
        }

        companion object : Mod, ValueMod {
            override val type: String = "FL"
            override val mode: Set<OsuMode> = setOf(OsuMode.OSU, OsuMode.TAIKO, OsuMode.CATCH, OsuMode.MANIA)
            override val incompatible: Set<Mod> = setOf(Blinds, FadeIn, Hidden, Cover)
            override val value: Int = 1024
        }
    }

    class Blinds : LazerMod() {
        @get:JsonProperty("acronym")
        override val acronym: String = type

        @get:JsonProperty("settings")
        override var settings: Any? = null

        @get:JsonProperty("color")
        override val color: String = "#EB6100"

        companion object : Mod {
            override val type: String = "BL"
            override val mode: Set<OsuMode> = setOf(OsuMode.OSU)
            override val incompatible: Set<Mod> = setOf(Flashlight)

        }
    }

    class StrictTracking : LazerMod() {
        @get:JsonProperty("acronym")
        override val acronym: String = type

        @get:JsonProperty("settings")
        override var settings: Any? = null

        @get:JsonProperty("color")
        override val color: String = "#D32F2F"

        companion object : Mod {
            override val type: String = "ST"
            override val mode: Set<OsuMode> = setOf(OsuMode.OSU)
            override val incompatible: Set<Mod> = setOf(TargetPractice, Classic)

        }
    }

    class AccuracyChallenge(
        minimumAccuracy: Float? = null,
        accuracyJudgeMode: String? = null,
        restart: Boolean? = null,
    ) : LazerMod() {
        @get:JsonProperty("acronym")
        override val acronym: String = type

        @get:JsonProperty("settings")
        override var settings: Any? = null

        @get:JsonProperty("color")
        override val color: String = "#9E040D"

        @JsonProperty("settings")
        private fun setSettings(node: JsonNode) {
            settings = node.json<Value>()
        }

        @get:JsonIgnore
        @set:JsonIgnore
        var minimumAccuracy: Float?
            get() = settings?.let { (it as Value).minimumAccuracy }
            set(v) {
                if (settings == null) settings = Value(minimumAccuracy = v)
                else (settings as Value).minimumAccuracy = v
            }

        @get:JsonIgnore
        @set:JsonIgnore
        var accuracyJudgeMode: String?
            get() = settings?.let { (it as Value).accuracyJudgeMode }
            set(v) {
                if (settings == null) settings = Value(accuracyJudgeMode = v)
                else (settings as Value).accuracyJudgeMode = v
            }

        @get:JsonIgnore
        @set:JsonIgnore
        var restart: Boolean?
            get() = settings?.let { (it as Value).restart }
            set(v) {
                if (settings == null) settings = Value(restart = v)
                else (settings as Value).restart = v
            }

        private data class Value(
            @JsonProperty("minimum_accuracy") var minimumAccuracy: Float? = null,
            @JsonProperty("accuracy_judge_mode") var accuracyJudgeMode: String? = null,
            @JsonProperty("restart") var restart: Boolean? = null,
        )

        init {
            minimumAccuracy?.let { this.minimumAccuracy = it }
            accuracyJudgeMode?.let { this.accuracyJudgeMode = it }
            restart?.let { this.restart = it }
        }

        companion object : Mod {
            override val type: String = "AC"
            override val mode: Set<OsuMode> = setOf(OsuMode.OSU, OsuMode.TAIKO, OsuMode.CATCH, OsuMode.MANIA)
            override val incompatible: Set<Mod> = setOf(Easy, NoFail, Perfect, Cinema)

        }
    }

    class TargetPractice(
        seed: Float? = null,
        metronome: Boolean? = null,
    ) : LazerMod() {
        @get:JsonProperty("acronym")
        override val acronym: String = type

        @get:JsonProperty("settings")
        override var settings: Any? = null

        @get:JsonProperty("color")
        override val color: String = "#920783"

        @JsonProperty("settings")
        private fun setSettings(node: JsonNode) {
            settings = node.json<Value>()
        }

        @get:JsonIgnore
        @set:JsonIgnore
        var seed: Float?
            get() = settings?.let { (it as Value).seed }
            set(v) {
                if (settings == null) settings = Value(seed = v)
                else (settings as Value).seed = v
            }

        @get:JsonIgnore
        @set:JsonIgnore
        var metronome: Boolean?
            get() = settings?.let { (it as Value).metronome }
            set(v) {
                if (settings == null) settings = Value(metronome = v)
                else (settings as Value).metronome = v
            }

        private data class Value(
            @JsonProperty("seed") var seed: Float? = null,
            @JsonProperty("metronome") var metronome: Boolean? = null,
        )

        init {
            seed?.let { this.seed = it }
            metronome?.let { this.metronome = it }
        }

        companion object : Mod {
            override val type: String = "TP"
            override val mode: Set<OsuMode> = setOf(OsuMode.OSU)
            override val incompatible: Set<Mod> =
                setOf(SuddenDeath, StrictTracking, Random, SpunOut, Traceable, ApproachDifferent, Depth)

        }
    }

    class DifficultyAdjust(
        circleSize: Float? = null,
        approachRate: Float? = null,
        drainRate: Float? = null,
        overallDifficulty: Float? = null,
        extendedLimits: Boolean? = null,
        scrollSpeed: Float? = null,
        hardRockOffsets: Boolean? = null,
    ) : LazerMod() {
        @get:JsonProperty("acronym")
        override val acronym: String = type

        @get:JsonProperty("settings")
        override var settings: Any? = null

        @get:JsonProperty("color")
        override val color: String = "#601986"

        @JsonProperty("settings")
        private fun setSettings(node: JsonNode) {
            settings = node.json<Value>()
        }

        @get:JsonIgnore
        @set:JsonIgnore
        var circleSize: Float?
            get() = settings?.let { (it as Value).circleSize }
            set(v) {
                if (settings == null) settings = Value(circleSize = v)
                else (settings as Value).circleSize = v
            }

        @get:JsonIgnore
        @set:JsonIgnore
        var approachRate: Float?
            get() = settings?.let { (it as Value).approachRate }
            set(v) {
                if (settings == null) settings = Value(approachRate = v)
                else (settings as Value).approachRate = v
            }

        @get:JsonIgnore
        @set:JsonIgnore
        var drainRate: Float?
            get() = settings?.let { (it as Value).drainRate }
            set(v) {
                if (settings == null) settings = Value(drainRate = v)
                else (settings as Value).drainRate = v
            }

        @get:JsonIgnore
        @set:JsonIgnore
        var overallDifficulty: Float?
            get() = settings?.let { (it as Value).overallDifficulty }
            set(v) {
                if (settings == null) settings = Value(overallDifficulty = v)
                else (settings as Value).overallDifficulty = v
            }

        @get:JsonIgnore
        @set:JsonIgnore
        var extendedLimits: Boolean?
            get() = settings?.let { (it as Value).extendedLimits }
            set(v) {
                if (settings == null) settings = Value(extendedLimits = v)
                else (settings as Value).extendedLimits = v
            }

        @get:JsonIgnore
        @set:JsonIgnore
        var scrollSpeed: Float?
            get() = settings?.let { (it as Value).scrollSpeed }
            set(v) {
                if (settings == null) settings = Value(scrollSpeed = v)
                else (settings as Value).scrollSpeed = v
            }

        @get:JsonIgnore
        @set:JsonIgnore
        var hardRockOffsets: Boolean?
            get() = settings?.let { (it as Value).hardRockOffsets }
            set(v) {
                if (settings == null) settings = Value(hardRockOffsets = v)
                else (settings as Value).hardRockOffsets = v
            }

        private data class Value(
            @JsonProperty("circle_size") var circleSize: Float? = null,
            @JsonProperty("approach_rate") var approachRate: Float? = null,
            @JsonProperty("drain_rate") var drainRate: Float? = null,
            @JsonProperty("overall_difficulty") var overallDifficulty: Float? = null,
            @JsonProperty("extended_limits") var extendedLimits: Boolean? = null,
            @JsonProperty("scroll_speed") var scrollSpeed: Float? = null,
            @JsonProperty("hard_rock_offsets") var hardRockOffsets: Boolean? = null,
        )

        init {
            circleSize?.let { this.circleSize = it }
            approachRate?.let { this.approachRate = it }
            drainRate?.let { this.drainRate = it }
            overallDifficulty?.let { this.overallDifficulty = it }
            extendedLimits?.let { this.extendedLimits = it }
            scrollSpeed?.let { this.scrollSpeed = it }
            hardRockOffsets?.let { this.hardRockOffsets = it }
        }

        companion object : Mod {
            override val type: String = "DA"
            override val mode: Set<OsuMode> = setOf(OsuMode.OSU, OsuMode.TAIKO, OsuMode.CATCH, OsuMode.MANIA)
            override val incompatible: Set<Mod> = setOf(Easy, HardRock)

        }
    }

    class Classic(
        noSliderHeadAccuracy: Boolean? = null,
        classicNoteLock: Boolean? = null,
        alwaysPlayTailSample: Boolean? = null,
        fadeHitCircleEarly: Boolean? = null,
        classicHealth: Boolean? = null,
    ) : LazerMod() {
        @get:JsonProperty("acronym")
        override val acronym: String = type

        @get:JsonProperty("settings")
        override var settings: Any? = null

        @get:JsonProperty("color")
        override val color: String = "#920783"

        @JsonProperty("settings")
        private fun setSettings(node: JsonNode) {
            settings = node.json<Value>()
        }

        @get:JsonIgnore
        @set:JsonIgnore
        var noSliderHeadAccuracy: Boolean?
            get() = settings?.let { (it as Value).noSliderHeadAccuracy }
            set(v) {
                if (settings == null) settings = Value(noSliderHeadAccuracy = v)
                else (settings as Value).noSliderHeadAccuracy = v
            }

        @get:JsonIgnore
        @set:JsonIgnore
        var classicNoteLock: Boolean?
            get() = settings?.let { (it as Value).classicNoteLock }
            set(v) {
                if (settings == null) settings = Value(classicNoteLock = v)
                else (settings as Value).classicNoteLock = v
            }

        @get:JsonIgnore
        @set:JsonIgnore
        var alwaysPlayTailSample: Boolean?
            get() = settings?.let { (it as Value).alwaysPlayTailSample }
            set(v) {
                if (settings == null) settings = Value(alwaysPlayTailSample = v)
                else (settings as Value).alwaysPlayTailSample = v
            }

        @get:JsonIgnore
        @set:JsonIgnore
        var fadeHitCircleEarly: Boolean?
            get() = settings?.let { (it as Value).fadeHitCircleEarly }
            set(v) {
                if (settings == null) settings = Value(fadeHitCircleEarly = v)
                else (settings as Value).fadeHitCircleEarly = v
            }

        @get:JsonIgnore
        @set:JsonIgnore
        var classicHealth: Boolean?
            get() = settings?.let { (it as Value).classicHealth }
            set(v) {
                if (settings == null) settings = Value(classicHealth = v)
                else (settings as Value).classicHealth = v
            }

        private data class Value(
            @JsonProperty("no_slider_head_accuracy") var noSliderHeadAccuracy: Boolean? = null,
            @JsonProperty("classic_note_lock") var classicNoteLock: Boolean? = null,
            @JsonProperty("always_play_tail_sample") var alwaysPlayTailSample: Boolean? = null,
            @JsonProperty("fade_hit_circle_early") var fadeHitCircleEarly: Boolean? = null,
            @JsonProperty("classic_health") var classicHealth: Boolean? = null,
        )

        init {
            noSliderHeadAccuracy?.let { this.noSliderHeadAccuracy = it }
            classicNoteLock?.let { this.classicNoteLock = it }
            alwaysPlayTailSample?.let { this.alwaysPlayTailSample = it }
            fadeHitCircleEarly?.let { this.fadeHitCircleEarly = it }
            classicHealth?.let { this.classicHealth = it }
        }

        companion object : Mod {
            override val type: String = "CL"
            override val mode: Set<OsuMode> = setOf(OsuMode.OSU, OsuMode.TAIKO, OsuMode.CATCH, OsuMode.MANIA)
            override val incompatible: Set<Mod> = setOf(StrictTracking)

        }
    }

    class Random(
        angleSharpness: Float? = null,
        seed: Float? = null,
    ) : LazerMod() {
        @get:JsonProperty("acronym")
        override val acronym: String = type

        @get:JsonProperty("settings")
        override var settings: Any? = null

        @get:JsonProperty("color")
        override val color: String = "#009944"

        @JsonProperty("settings")
        private fun setSettings(node: JsonNode) {
            settings = node.json<Value>()
        }

        @get:JsonIgnore
        @set:JsonIgnore
        var angleSharpness: Float?
            get() = settings?.let { (it as Value).angleSharpness }
            set(v) {
                if (settings == null) settings = Value(angleSharpness = v)
                else (settings as Value).angleSharpness = v
            }

        @get:JsonIgnore
        @set:JsonIgnore
        var seed: Float?
            get() = settings?.let { (it as Value).seed }
            set(v) {
                if (settings == null) settings = Value(seed = v)
                else (settings as Value).seed = v
            }

        private data class Value(
            @JsonProperty("angle_sharpness") var angleSharpness: Float? = null,
            @JsonProperty("seed") var seed: Float? = null,
        )

        init {
            angleSharpness?.let { this.angleSharpness = it }
            seed?.let { this.seed = it }
        }

        companion object : Mod, ValueMod {
            override val type: String = "RD"
            override val mode: Set<OsuMode> = setOf(OsuMode.OSU, OsuMode.TAIKO, OsuMode.MANIA)
            override val incompatible: Set<Mod> = setOf(TargetPractice, Swap)
            override val value: Int = 2097152
        }
    }

    class Mirror(
        reflection: String? = null,
    ) : LazerMod() {
        @get:JsonProperty("acronym")
        override val acronym: String = type

        @get:JsonProperty("settings")
        override var settings: Any? = null

        @get:JsonProperty("color")
        override val color: String = "#007130"

        @JsonProperty("settings")
        private fun setSettings(node: JsonNode) {
            settings = node.json<Value>()
        }

        @get:JsonIgnore
        @set:JsonIgnore
        var reflection: String?
            get() = settings?.let { (it as Value).reflection }
            set(v) {
                if (settings == null) settings = Value(reflection = v)
                else (settings as Value).reflection = v
            }

        private data class Value(
            @JsonProperty("reflection") var reflection: String? = null,
        )

        init {
            reflection?.let { this.reflection = it }
        }

        companion object : Mod, ValueMod {
            override val type: String = "MR"
            override val mode: Set<OsuMode> = setOf(OsuMode.OSU, OsuMode.CATCH, OsuMode.MANIA)
            override val incompatible: Set<Mod> = setOf(HardRock)
            override val value: Int = 1073741824
        }
    }

    class Alternate : LazerMod() {
        @get:JsonProperty("acronym")
        override val acronym: String = type

        @get:JsonProperty("settings")
        override var settings: Any? = null

        @get:JsonProperty("color")
        override val color: String = "#F16DAA"

        companion object : Mod {
            override val type: String = "AL"
            override val mode: Set<OsuMode> = setOf(OsuMode.OSU)
            override val incompatible: Set<Mod> = setOf(SingleTap, Autoplay, Cinema, Relax)

        }
    }

    class SingleTap : LazerMod() {
        @get:JsonProperty("acronym")
        override val acronym: String = type

        @get:JsonProperty("settings")
        override var settings: Any? = null

        @get:JsonProperty("color")
        override val color: String = "#F59AC3"

        companion object : Mod {
            override val type: String = "SG"
            override val mode: Set<OsuMode> = setOf(OsuMode.OSU, OsuMode.TAIKO)
            override val incompatible: Set<Mod> = setOf(Alternate, Autoplay, Cinema, Relax)

        }
    }

    class Autoplay : LazerMod() {
        @get:JsonProperty("acronym")
        override val acronym: String = type

        @get:JsonProperty("settings")
        override var settings: Any? = null

        @get:JsonProperty("color")
        override val color: String = "#00B7EE"

        companion object : Mod, ValueMod {
            override val type: String = "AT"
            override val mode: Set<OsuMode> = setOf(OsuMode.OSU, OsuMode.TAIKO, OsuMode.CATCH, OsuMode.MANIA)
            override val incompatible: Set<Mod> = setOf(
                Alternate, SingleTap, Cinema, Relax, Autopilot, SpunOut, Magnetised, Repel, AdaptiveSpeed, TouchDevice
            )
            override val value: Int = 2048
        }
    }

    class Cinema : LazerMod() {
        @get:JsonProperty("acronym")
        override val acronym: String = type

        @get:JsonProperty("settings")
        override var settings: Any? = null

        @get:JsonProperty("color")
        override val color: String = "#00B7EE"

        companion object : Mod, ValueMod {
            override val type: String = "CN"
            override val mode: Set<OsuMode> = setOf(OsuMode.OSU, OsuMode.TAIKO, OsuMode.CATCH, OsuMode.MANIA)
            override val incompatible: Set<Mod> = setOf(
                NoFail,
                SuddenDeath,
                Perfect,
                AccuracyChallenge,
                Alternate,
                SingleTap,
                Autoplay,
                Cinema,
                Relax,
                Autopilot,
                SpunOut,
                Magnetised,
                Repel,
                AdaptiveSpeed,
                TouchDevice
            )
            override val value: Int = 4194304
        }
    }

    class Relax : LazerMod() {
        @get:JsonProperty("acronym")
        override val acronym: String = type

        @get:JsonProperty("settings")
        override var settings: Any? = null

        @get:JsonProperty("color")
        override val color: String = "#BFC31F"

        companion object : Mod, ValueMod {
            override val type: String = "RX"
            override val mode: Set<OsuMode> = setOf(OsuMode.OSU, OsuMode.TAIKO, OsuMode.CATCH)
            override val incompatible: Set<Mod> = setOf(Alternate, SingleTap, Autoplay, Cinema, Autopilot, Magnetised)
            override val value: Int = 128
        }
    }

    class Autopilot : LazerMod() {
        @get:JsonProperty("acronym")
        override val acronym: String = type

        @get:JsonProperty("settings")
        override var settings: Any? = null

        @get:JsonProperty("color")
        override val color: String = "#B3D465"

        companion object : Mod, ValueMod {
            override val type: String = "AP"
            override val mode: Set<OsuMode> = setOf(OsuMode.OSU)
            override val incompatible: Set<Mod> =
                setOf(Autoplay, Cinema, Relax, SpunOut, Magnetised, Repel, TouchDevice)
            override val value: Int = 8192
        }
    }

    class SpunOut : LazerMod() {
        @get:JsonProperty("acronym")
        override val acronym: String = type

        @get:JsonProperty("settings")
        override var settings: Any? = null

        @get:JsonProperty("color")
        override val color: String = "#B28850"

        companion object : Mod, ValueMod {
            override val type: String = "SO"
            override val mode: Set<OsuMode> = setOf(OsuMode.OSU)
            override val incompatible: Set<Mod> = setOf(TargetPractice, Autoplay, Cinema, Autopilot)
            override val value: Int = 4096
        }
    }

    class Transform : LazerMod() {
        @get:JsonProperty("acronym")
        override val acronym: String = type

        @get:JsonProperty("settings")
        override var settings: Any? = null

        @get:JsonProperty("color")
        override val color: String = FUN_MOD_COLOR

        companion object : Mod {
            override val type: String = "TR"
            override val mode: Set<OsuMode> = setOf(OsuMode.OSU)
            override val incompatible: Set<Mod> = setOf(Wiggle, Magnetised, Repel, FreezeFrame, Depth)

        }
    }

    class Wiggle(
        strength: Float? = null,
    ) : LazerMod() {
        @get:JsonProperty("acronym")
        override val acronym: String = type

        @get:JsonProperty("settings")
        override var settings: Any? = null

        @get:JsonProperty("color")
        override val color: String = FUN_MOD_COLOR

        @JsonProperty("settings")
        private fun setSettings(node: JsonNode) {
            settings = node.json<Value>()
        }

        @get:JsonIgnore
        @set:JsonIgnore
        var strength: Float?
            get() = settings?.let { (it as Value).strength }
            set(v) {
                if (settings == null) settings = Value(strength = v)
                else (settings as Value).strength = v
            }

        private data class Value(
            @JsonProperty("strength") var strength: Float? = null,
        )

        init {
            strength?.let { this.strength = it }
        }

        companion object : Mod {
            override val type: String = "WG"
            override val mode: Set<OsuMode> = setOf(OsuMode.OSU)
            override val incompatible: Set<Mod> = setOf(Transform, Magnetised, Repel, Depth)

        }
    }

    class SpinIn : LazerMod() {
        @get:JsonProperty("acronym")
        override val acronym: String = type

        @get:JsonProperty("settings")
        override var settings: Any? = null

        @get:JsonProperty("color")
        override val color: String = FUN_MOD_COLOR

        companion object : Mod {
            override val type: String = "SI"
            override val mode: Set<OsuMode> = setOf(OsuMode.OSU)
            override val incompatible: Set<Mod> = setOf(Hidden, Grow, Deflate, Traceable, ApproachDifferent, Depth)

        }
    }

    class Grow(
        startScale: Float? = null,
    ) : LazerMod() {
        @get:JsonProperty("acronym")
        override val acronym: String = type

        @get:JsonProperty("settings")
        override var settings: Any? = null

        @get:JsonProperty("color")
        override val color: String = FUN_MOD_COLOR

        @JsonProperty("settings")
        private fun setSettings(node: JsonNode) {
            settings = node.json<Value>()
        }

        @get:JsonIgnore
        @set:JsonIgnore
        var startScale: Float?
            get() = settings?.let { (it as Value).startScale }
            set(v) {
                if (settings == null) settings = Value(startScale = v)
                else (settings as Value).startScale = v
            }

        private data class Value(
            @JsonProperty("start_scale") var startScale: Float? = null,
        )

        init {
            startScale?.let { this.startScale = it }
        }

        companion object : Mod {
            override val type: String = "GR"
            override val mode: Set<OsuMode> = setOf(OsuMode.OSU)
            override val incompatible: Set<Mod> = setOf(SpinIn, Grow, Deflate, Traceable, ApproachDifferent, Depth)

        }
    }

    class Deflate(
        startScale: Float? = null,
    ) : LazerMod() {
        @get:JsonProperty("acronym")
        override val acronym: String = type

        @get:JsonProperty("settings")
        override var settings: Any? = null

        @get:JsonProperty("color")
        override val color: String = FUN_MOD_COLOR

        @JsonProperty("settings")
        private fun setSettings(node: JsonNode) {
            settings = node.json<Value>()
        }

        @get:JsonIgnore
        @set:JsonIgnore
        var startScale: Float?
            get() = settings?.let { (it as Value).startScale }
            set(v) {
                if (settings == null) settings = Value(startScale = v)
                else (settings as Value).startScale = v
            }

        private data class Value(
            @JsonProperty("start_scale") var startScale: Float? = null,
        )

        init {
            startScale?.let { this.startScale = it }
        }

        companion object : Mod {
            override val type: String = "DF"
            override val mode: Set<OsuMode> = setOf(OsuMode.OSU)
            override val incompatible: Set<Mod> = setOf(SpinIn, Grow, Deflate, Traceable, ApproachDifferent, Depth)

        }
    }

    class WindUp(
        initialRate: Float? = null,
        finalRate: Float? = null,
        adjustPitch: Boolean? = null,
    ) : LazerMod() {
        @get:JsonProperty("acronym")
        override val acronym: String = type

        @get:JsonProperty("settings")
        override var settings: Any? = null

        @get:JsonProperty("color")
        override val color: String = FUN_MOD_COLOR

        @JsonProperty("settings")
        private fun setSettings(node: JsonNode) {
            settings = node.json<Value>()
        }

        @get:JsonIgnore
        @set:JsonIgnore
        var initialRate: Float?
            get() = settings?.let { (it as Value).initialRate }
            set(v) {
                if (settings == null) settings = Value(initialRate = v)
                else (settings as Value).initialRate = v
            }

        @get:JsonIgnore
        @set:JsonIgnore
        var finalRate: Float?
            get() = settings?.let { (it as Value).finalRate }
            set(v) {
                if (settings == null) settings = Value(finalRate = v)
                else (settings as Value).finalRate = v
            }

        @get:JsonIgnore
        @set:JsonIgnore
        var adjustPitch: Boolean?
            get() = settings?.let { (it as Value).adjustPitch }
            set(v) {
                if (settings == null) settings = Value(adjustPitch = v)
                else (settings as Value).adjustPitch = v
            }

        private data class Value(
            @JsonProperty("initial_rate") var initialRate: Float? = null,
            @JsonProperty("final_rate") var finalRate: Float? = null,
            @JsonProperty("adjust_pitch") var adjustPitch: Boolean? = null,
        )

        init {
            initialRate?.let { this.initialRate = it }
            finalRate?.let { this.finalRate = it }
            adjustPitch?.let { this.adjustPitch = it }
        }

        companion object : Mod {
            override val type: String = "WU"
            override val mode: Set<OsuMode> = setOf(OsuMode.OSU, OsuMode.TAIKO, OsuMode.CATCH, OsuMode.MANIA)
            override val incompatible: Set<Mod> =
                setOf(HalfTime, Daycore, DoubleTime, Nightcore, WindDown, AdaptiveSpeed)

        }
    }

    class WindDown(
        initialRate: Float? = null,
        finalRate: Float? = null,
        adjustPitch: Boolean? = null,
    ) : LazerMod() {
        @get:JsonProperty("acronym")
        override val acronym: String = type

        @get:JsonProperty("settings")
        override var settings: Any? = null

        @get:JsonProperty("color")
        override val color: String = FUN_MOD_COLOR

        @JsonProperty("settings")
        private fun setSettings(node: JsonNode) {
            settings = node.json<Value>()
        }

        @get:JsonIgnore
        @set:JsonIgnore
        var initialRate: Float?
            get() = settings?.let { (it as Value).initialRate }
            set(v) {
                if (settings == null) settings = Value(initialRate = v)
                else (settings as Value).initialRate = v
            }

        @get:JsonIgnore
        @set:JsonIgnore
        var finalRate: Float?
            get() = settings?.let { (it as Value).finalRate }
            set(v) {
                if (settings == null) settings = Value(finalRate = v)
                else (settings as Value).finalRate = v
            }

        @get:JsonIgnore
        @set:JsonIgnore
        var adjustPitch: Boolean?
            get() = settings?.let { (it as Value).adjustPitch }
            set(v) {
                if (settings == null) settings = Value(adjustPitch = v)
                else (settings as Value).adjustPitch = v
            }

        private data class Value(
            @JsonProperty("initial_rate") var initialRate: Float? = null,
            @JsonProperty("final_rate") var finalRate: Float? = null,
            @JsonProperty("adjust_pitch") var adjustPitch: Boolean? = null,
        )

        init {
            initialRate?.let { this.initialRate = it }
            finalRate?.let { this.finalRate = it }
            adjustPitch?.let { this.adjustPitch = it }
        }

        companion object : Mod {
            override val type: String = "WD"
            override val mode: Set<OsuMode> = setOf(OsuMode.OSU, OsuMode.TAIKO, OsuMode.CATCH, OsuMode.MANIA)
            override val incompatible: Set<Mod> = setOf(HalfTime, Daycore, DoubleTime, Nightcore, WindUp, AdaptiveSpeed)

        }
    }

    class Traceable : LazerMod() {
        @get:JsonProperty("acronym")
        override val acronym: String = type

        @get:JsonProperty("settings")
        override var settings: Any? = null

        @get:JsonProperty("color")
        override val color: String = FUN_MOD_COLOR

        companion object : Mod {
            override val type: String = "TC"
            override val mode: Set<OsuMode> = setOf(OsuMode.OSU)
            override val incompatible: Set<Mod> = setOf(Hidden, TargetPractice, SpinIn, Grow, Deflate, Depth)

        }
    }

    class BarrelRoll(
        spinSpeed: Float? = null,
        direction: String? = null,
    ) : LazerMod() {
        @get:JsonProperty("acronym")
        override val acronym: String = type

        @get:JsonProperty("settings")
        override var settings: Any? = null

        @get:JsonProperty("color")
        override val color: String = FUN_MOD_COLOR

        @JsonProperty("settings")
        private fun setSettings(node: JsonNode) {
            settings = node.json<Value>()
        }

        @get:JsonIgnore
        @set:JsonIgnore
        var spinSpeed: Float?
            get() = settings?.let { (it as Value).spinSpeed }
            set(v) {
                if (settings == null) settings = Value(spinSpeed = v)
                else (settings as Value).spinSpeed = v
            }

        @get:JsonIgnore
        @set:JsonIgnore
        var direction: String?
            get() = settings?.let { (it as Value).direction }
            set(v) {
                if (settings == null) settings = Value(direction = v)
                else (settings as Value).direction = v
            }

        private data class Value(
            @JsonProperty("spin_speed") var spinSpeed: Float? = null,
            @JsonProperty("direction") var direction: String? = null,
        )

        init {
            spinSpeed?.let { this.spinSpeed = it }
            direction?.let { this.direction = it }
        }

        companion object : Mod {
            override val type: String = "BR"
            override val mode: Set<OsuMode> = setOf(OsuMode.OSU)
            override val incompatible: Set<Mod> = setOf(Bubbles)

        }
    }

    class ApproachDifferent(
        scale: Float? = null,
        style: String? = null,
    ) : LazerMod() {
        @get:JsonProperty("acronym")
        override val acronym: String = type

        @get:JsonProperty("settings")
        override var settings: Any? = null

        @get:JsonProperty("color")
        override val color: String = FUN_MOD_COLOR

        @JsonProperty("settings")
        private fun setSettings(node: JsonNode) {
            settings = node.json<Value>()
        }

        @get:JsonIgnore
        @set:JsonIgnore
        var scale: Float?
            get() = settings?.let { (it as Value).scale }
            set(v) {
                if (settings == null) settings = Value(scale = v)
                else (settings as Value).scale = v
            }

        @get:JsonIgnore
        @set:JsonIgnore
        var style: String?
            get() = settings?.let { (it as Value).style }
            set(v) {
                if (settings == null) settings = Value(style = v)
                else (settings as Value).style = v
            }

        private data class Value(
            @JsonProperty("scale") var scale: Float? = null,
            @JsonProperty("style") var style: String? = null,
        )

        init {
            scale?.let { this.scale = it }
            style?.let { this.style = it }
        }

        companion object : Mod {
            override val type: String = "AD"
            override val mode: Set<OsuMode> = setOf(OsuMode.OSU)
            override val incompatible: Set<Mod> = setOf(Hidden, TargetPractice, SpinIn, Grow, Deflate, FreezeFrame)

        }
    }

    class Muted(
        inverseMuting: Boolean? = null,
        enableMetronome: Boolean? = null,
        muteComboCount: Float? = null,
        affectsHitSounds: Boolean? = null,
    ) : LazerMod() {
        @get:JsonProperty("acronym")
        override val acronym: String = type

        @get:JsonProperty("settings")
        override var settings: Any? = null

        @get:JsonProperty("color")
        override val color: String = FUN_MOD_COLOR

        @JsonProperty("settings")
        private fun setSettings(node: JsonNode) {
            settings = node.json<Value>()
        }

        @get:JsonIgnore
        @set:JsonIgnore
        var inverseMuting: Boolean?
            get() = settings?.let { (it as Value).inverseMuting }
            set(v) {
                if (settings == null) settings = Value(inverseMuting = v)
                else (settings as Value).inverseMuting = v
            }

        @get:JsonIgnore
        @set:JsonIgnore
        var enableMetronome: Boolean?
            get() = settings?.let { (it as Value).enableMetronome }
            set(v) {
                if (settings == null) settings = Value(enableMetronome = v)
                else (settings as Value).enableMetronome = v
            }

        @get:JsonIgnore
        @set:JsonIgnore
        var muteComboCount: Float?
            get() = settings?.let { (it as Value).muteComboCount }
            set(v) {
                if (settings == null) settings = Value(muteComboCount = v)
                else (settings as Value).muteComboCount = v
            }

        @get:JsonIgnore
        @set:JsonIgnore
        var affectsHitSounds: Boolean?
            get() = settings?.let { (it as Value).affectsHitSounds }
            set(v) {
                if (settings == null) settings = Value(affectsHitSounds = v)
                else (settings as Value).affectsHitSounds = v
            }

        private data class Value(
            @JsonProperty("inverse_muting") var inverseMuting: Boolean? = null,
            @JsonProperty("enable_metronome") var enableMetronome: Boolean? = null,
            @JsonProperty("mute_combo_count") var muteComboCount: Float? = null,
            @JsonProperty("affects_hit_sounds") var affectsHitSounds: Boolean? = null,
        )

        init {
            inverseMuting?.let { this.inverseMuting = it }
            enableMetronome?.let { this.enableMetronome = it }
            muteComboCount?.let { this.muteComboCount = it }
            affectsHitSounds?.let { this.affectsHitSounds = it }
        }

        companion object : Mod {
            override val type: String = "MU"
            override val mode: Set<OsuMode> = setOf(OsuMode.OSU, OsuMode.TAIKO, OsuMode.CATCH, OsuMode.MANIA)
            override val incompatible: Set<Mod> = setOf()

        }
    }

    class NoScope(
        hiddenComboCount: Float? = null,
    ) : LazerMod() {
        @get:JsonProperty("acronym")
        override val acronym: String = type

        @get:JsonProperty("settings")
        override var settings: Any? = null

        @get:JsonProperty("color")
        override val color: String = FUN_MOD_COLOR

        @JsonProperty("settings")
        private fun setSettings(node: JsonNode) {
            settings = node.json<Value>()
        }

        @get:JsonIgnore
        @set:JsonIgnore
        var hiddenComboCount: Float?
            get() = settings?.let { (it as Value).hiddenComboCount }
            set(v) {
                if (settings == null) settings = Value(hiddenComboCount = v)
                else (settings as Value).hiddenComboCount = v
            }

        private data class Value(
            @JsonProperty("hidden_combo_count") var hiddenComboCount: Float? = null,
        )

        init {
            hiddenComboCount?.let { this.hiddenComboCount = it }
        }

        companion object : Mod {
            override val type: String = "NS"
            override val mode: Set<OsuMode> = setOf(OsuMode.OSU, OsuMode.CATCH)
            override val incompatible: Set<Mod> = setOf()

        }
    }

    class Magnetised(
        attractionStrength: Float? = null,
    ) : LazerMod() {
        @get:JsonProperty("acronym")
        override val acronym: String = type

        @get:JsonProperty("settings")
        override var settings: Any? = null

        @get:JsonProperty("color")
        override val color: String = FUN_MOD_COLOR

        @JsonProperty("settings")
        private fun setSettings(node: JsonNode) {
            settings = node.json<Value>()
        }

        @get:JsonIgnore
        @set:JsonIgnore
        var attractionStrength: Float?
            get() = settings?.let { (it as Value).attractionStrength }
            set(v) {
                if (settings == null) settings = Value(attractionStrength = v)
                else (settings as Value).attractionStrength = v
            }

        private data class Value(
            @JsonProperty("attraction_strength") var attractionStrength: Float? = null,
        )

        init {
            attractionStrength?.let { this.attractionStrength = it }
        }

        companion object : Mod {
            override val type: String = "MG"
            override val mode: Set<OsuMode> = setOf(OsuMode.OSU)
            override val incompatible: Set<Mod> =
                setOf(Autoplay, Cinema, Relax, Autopilot, Transform, Wiggle, Repel, Bubbles, Depth)

        }
    }

    class Repel(
        repulsionStrength: Float? = null,
    ) : LazerMod() {
        @get:JsonProperty("acronym")
        override val acronym: String = type

        @get:JsonProperty("settings")
        override var settings: Any? = null

        @get:JsonProperty("color")
        override val color: String = FUN_MOD_COLOR

        @JsonProperty("settings")
        private fun setSettings(node: JsonNode) {
            settings = node.json<Value>()
        }

        @get:JsonIgnore
        @set:JsonIgnore
        var repulsionStrength: Float?
            get() = settings?.let { (it as Value).repulsionStrength }
            set(v) {
                if (settings == null) settings = Value(repulsionStrength = v)
                else (settings as Value).repulsionStrength = v
            }

        private data class Value(
            @JsonProperty("repulsion_strength") var repulsionStrength: Float? = null,
        )

        init {
            repulsionStrength?.let { this.repulsionStrength = it }
        }

        companion object : Mod {
            override val type: String = "RP"
            override val mode: Set<OsuMode> = setOf(OsuMode.OSU)
            override val incompatible: Set<Mod> =
                setOf(Autoplay, Cinema, Autopilot, Transform, Wiggle, Magnetised, Bubbles, Depth)

        }
    }

    class AdaptiveSpeed(
        initialRate: Float? = null,
        adjustPitch: Boolean? = null,
    ) : LazerMod() {
        @get:JsonProperty("acronym")
        override val acronym: String = type

        @get:JsonProperty("settings")
        override var settings: Any? = null

        @get:JsonProperty("color")
        override val color: String = FUN_MOD_COLOR

        @JsonProperty("settings")
        private fun setSettings(node: JsonNode) {
            settings = node.json<Value>()
        }

        @get:JsonIgnore
        @set:JsonIgnore
        var initialRate: Float?
            get() = settings?.let { (it as Value).initialRate }
            set(v) {
                if (settings == null) settings = Value(initialRate = v)
                else (settings as Value).initialRate = v
            }

        @get:JsonIgnore
        @set:JsonIgnore
        var adjustPitch: Boolean?
            get() = settings?.let { (it as Value).adjustPitch }
            set(v) {
                if (settings == null) settings = Value(adjustPitch = v)
                else (settings as Value).adjustPitch = v
            }

        private data class Value(
            @JsonProperty("initial_rate") var initialRate: Float? = null,
            @JsonProperty("adjust_pitch") var adjustPitch: Boolean? = null,
        )

        init {
            initialRate?.let { this.initialRate = it }
            adjustPitch?.let { this.adjustPitch = it }
        }

        companion object : Mod {
            override val type: String = "AS"
            override val mode: Set<OsuMode> = setOf(OsuMode.OSU, OsuMode.TAIKO, OsuMode.MANIA)
            override val incompatible: Set<Mod> =
                setOf(HalfTime, Daycore, DoubleTime, Nightcore, Autoplay, Cinema, WindUp, WindDown)

        }
    }

    class FreezeFrame : LazerMod() {
        @get:JsonProperty("acronym")
        override val acronym: String = type

        @get:JsonProperty("settings")
        override var settings: Any? = null

        @get:JsonProperty("color")
        override val color: String = FUN_MOD_COLOR

        companion object : Mod {
            override val type: String = "FR"
            override val mode: Set<OsuMode> = setOf(OsuMode.OSU)
            override val incompatible: Set<Mod> = setOf(Transform, ApproachDifferent, Depth)

        }
    }

    class Bubbles : LazerMod() {
        @get:JsonProperty("acronym")
        override val acronym: String = type

        @get:JsonProperty("settings")
        override var settings: Any? = null

        @get:JsonProperty("color")
        override val color: String = FUN_MOD_COLOR

        companion object : Mod {
            override val type: String = "BU"
            override val mode: Set<OsuMode> = setOf(OsuMode.OSU)
            override val incompatible: Set<Mod> = setOf(BarrelRoll, Magnetised, Repel)

        }
    }

    class Synesthesia : LazerMod() {
        @get:JsonProperty("acronym")
        override val acronym: String = type

        @get:JsonProperty("settings")
        override var settings: Any? = null

        @get:JsonProperty("color")
        override val color: String = FUN_MOD_COLOR

        companion object : Mod {
            override val type: String = "SY"
            override val mode: Set<OsuMode> = setOf(OsuMode.OSU)
            override val incompatible: Set<Mod> = setOf()

        }
    }

    class Depth(
        maxDepth: Float? = null,
        showApproachCircles: Boolean? = null,
    ) : LazerMod() {
        @get:JsonProperty("acronym")
        override val acronym: String = type

        @get:JsonProperty("settings")
        override var settings: Any? = null

        @get:JsonProperty("color")
        override val color: String = FUN_MOD_COLOR

        @JsonProperty("settings")
        private fun setSettings(node: JsonNode) {
            settings = node.json<Value>()
        }

        @get:JsonIgnore
        @set:JsonIgnore
        var maxDepth: Float?
            get() = settings?.let { (it as Value).maxDepth }
            set(v) {
                if (settings == null) settings = Value(maxDepth = v)
                else (settings as Value).maxDepth = v
            }

        @get:JsonIgnore
        @set:JsonIgnore
        var showApproachCircles: Boolean?
            get() = settings?.let { (it as Value).showApproachCircles }
            set(v) {
                if (settings == null) settings = Value(showApproachCircles = v)
                else (settings as Value).showApproachCircles = v
            }

        private data class Value(
            @JsonProperty("max_depth") var maxDepth: Float? = null,
            @JsonProperty("show_approach_circles") var showApproachCircles: Boolean? = null,
        )

        init {
            maxDepth?.let { this.maxDepth = it }
            showApproachCircles?.let { this.showApproachCircles = it }
        }

        companion object : Mod {
            override val type: String = "DP"
            override val mode: Set<OsuMode> = setOf(OsuMode.OSU)
            override val incompatible: Set<Mod> = setOf(
                Hidden,
                TargetPractice,
                Transform,
                Wiggle,
                SpinIn,
                Grow,
                Deflate,
                Traceable,
                Magnetised,
                Repel,
                FreezeFrame,
                Depth
            )

        }
    }

    class TouchDevice : LazerMod() {
        @get:JsonProperty("acronym")
        override val acronym: String = type

        @get:JsonProperty("settings")
        override var settings: Any? = null

        @get:JsonProperty("color")
        override val color: String = "#7ECEF4"

        companion object : Mod, ValueMod {
            override val type: String = "TD"
            override val mode: Set<OsuMode> = setOf(OsuMode.OSU)
            override val incompatible: Set<Mod> = setOf(Autoplay, Cinema, Autopilot)
            override val value: Int = 4
        }
    }

    class Bloom : LazerMod() {
        @get:JsonProperty("acronym")
        override val acronym: String = type

        @get:JsonProperty("settings")
        override var settings: Any? = null

        @get:JsonProperty("color")
        override val color: String = "#9E005E"

        @JsonProperty("settings")
        private fun setSettings(node: JsonNode) {
            settings = node.json<Value>()
        }

        @get:JsonIgnore
        @set:JsonIgnore
        var maxSizeComboCount: Int?
            get() = settings?.let { (it as Value).maxSizeComboCount }
            set(v) {
                if (settings == null) settings = Value(maxSizeComboCount = v)
                else (settings as Value).maxSizeComboCount = v
            }

        @get:JsonIgnore
        @set:JsonIgnore
        var maxCursorSize: Float?
            get() = settings?.let { (it as Value).maxCursorSize }
            set(v) {
                if (settings == null) settings = Value(maxCursorSize = v)
                else (settings as Value).maxCursorSize = v
            }


        private data class Value(
            @JsonProperty("max_size_combo_count") var maxSizeComboCount: Int? = null,
            @JsonProperty("max_cursor_size") var maxCursorSize: Float? = null,
        )

        companion object : Mod {
            override val type: String = "BM"
            override val mode: Set<OsuMode> = setOf(OsuMode.OSU)
            override val incompatible: Set<Mod> = setOf(Flashlight, NoScope, TouchDevice)
        }
    }

    class ScoreV2 : LazerMod() {
        @get:JsonProperty("acronym")
        override val acronym: String = type

        @get:JsonProperty("settings")
        override var settings: Any? = null

        @get:JsonProperty("color")
        override val color: String = "#000000"

        companion object : Mod, ValueMod {
            override val type: String = "SV2"
            override val mode: Set<OsuMode> = setOf(OsuMode.OSU, OsuMode.TAIKO, OsuMode.CATCH, OsuMode.MANIA)
            override val incompatible: Set<Mod> = setOf()
            override val value: Int = 536870912
        }
    }

    class Swap : LazerMod() {
        @get:JsonProperty("acronym")
        override val acronym: String = type

        @get:JsonProperty("settings")
        override var settings: Any? = null

        @get:JsonProperty("color")
        override val color: String = "#7B0046"

        companion object : Mod {
            override val type: String = "SW"
            override val mode: Set<OsuMode> = setOf(OsuMode.TAIKO)
            override val incompatible: Set<Mod> = setOf(Random)

        }
    }

    class ConstantSpeed : LazerMod() {
        @get:JsonProperty("acronym")
        override val acronym: String = type

        @get:JsonProperty("settings")
        override var settings: Any? = null

        @get:JsonProperty("color")
        override val color: String = "#A086BF"

        companion object : Mod {
            override val type: String = "CS"
            override val mode: Set<OsuMode> = setOf(OsuMode.TAIKO, OsuMode.MANIA)
            override val incompatible: Set<Mod> = setOf()

        }
    }

    class FloatingFruits : LazerMod() {
        @get:JsonProperty("acronym")
        override val acronym: String = type

        @get:JsonProperty("settings")
        override var settings: Any? = null

        @get:JsonProperty("color")
        override val color: String = FUN_MOD_COLOR

        companion object : Mod {
            override val type: String = "FF"
            override val mode: Set<OsuMode> = setOf(OsuMode.CATCH)
            override val incompatible: Set<Mod> = setOf()

        }
    }

    class NoRelease : LazerMod() {
        @get:JsonProperty("acronym")
        override val acronym: String = type

        @get:JsonProperty("settings")
        override var settings: Any? = null

        @get:JsonProperty("color")
        override val color: String = FUN_MOD_COLOR

        companion object : Mod {
            override val type: String = "NR"
            override val mode: Set<OsuMode> = setOf(OsuMode.MANIA)
            override val incompatible: Set<Mod> = setOf(HoldOff)

        }
    }

    class FadeIn : LazerMod() {
        @get:JsonProperty("acronym")
        override val acronym: String = type

        @get:JsonProperty("settings")
        override var settings: Any? = null

        @get:JsonProperty("color")
        override val color: String = "#F8B551"

        companion object : Mod, ValueMod {
            override val type: String = "FI"
            override val mode: Set<OsuMode> = setOf(OsuMode.MANIA)
            override val incompatible: Set<Mod> = setOf(FadeIn, Hidden, Cover, Flashlight)
            override val value: Int = 1048576
        }
    }

    class Cover(
        coverage: Float? = null,
        direction: String? = null,
    ) : LazerMod() {
        @get:JsonProperty("acronym")
        override val acronym: String = type

        @get:JsonProperty("settings")
        override var settings: Any? = null

        @get:JsonProperty("color")
        override val color: String = "#F8B551"

        @JsonProperty("settings")
        private fun setSettings(node: JsonNode) {
            settings = node.json<Value>()
        }

        @get:JsonIgnore
        @set:JsonIgnore
        var coverage: Float?
            get() = settings?.let { (it as Value).coverage }
            set(v) {
                if (settings == null) settings = Value(coverage = v)
                else (settings as Value).coverage = v
            }

        @get:JsonIgnore
        @set:JsonIgnore
        var direction: String?
            get() = settings?.let { (it as Value).direction }
            set(v) {
                if (settings == null) settings = Value(direction = v)
                else (settings as Value).direction = v
            }

        private data class Value(
            @JsonProperty("coverage") var coverage: Float? = null,
            @JsonProperty("direction") var direction: String? = null,
        )

        init {
            coverage?.let { this.coverage = it }
            direction?.let { this.direction = it }
        }

        companion object : Mod {
            override val type: String = "CO"
            override val mode: Set<OsuMode> = setOf(OsuMode.MANIA)
            override val incompatible: Set<Mod> = setOf(FadeIn, Hidden, Flashlight)

        }
    }

    class DualStages : LazerMod() {
        @get:JsonProperty("acronym")
        override val acronym: String = type

        @get:JsonProperty("settings")
        override var settings: Any? = null

        @get:JsonProperty("color")
        override val color: String = "#9E005E"

        companion object : Mod {
            override val type: String = "DS"
            override val mode: Set<OsuMode> = setOf(OsuMode.MANIA)
            override val incompatible: Set<Mod> = setOf()

        }
    }

    class Invert : LazerMod() {
        @get:JsonProperty("acronym")
        override val acronym: String = type

        @get:JsonProperty("settings")
        override var settings: Any? = null

        @get:JsonProperty("color")
        override val color: String = "#5F5BA8"

        companion object : Mod {
            override val type: String = "IN"
            override val mode: Set<OsuMode> = setOf(OsuMode.MANIA)
            override val incompatible: Set<Mod> = setOf(HoldOff)

        }
    }

    class HoldOff : LazerMod() {
        @get:JsonProperty("acronym")
        override val acronym: String = type

        @get:JsonProperty("settings")
        override var settings: Any? = null

        @get:JsonProperty("color")
        override val color: String = "#8781BE"

        companion object : Mod {
            override val type: String = "HO"
            override val mode: Set<OsuMode> = setOf(OsuMode.MANIA)
            override val incompatible: Set<Mod> = setOf(NoRelease, Invert)

        }
    }

    class Key1 : LazerMod() {
        @get:JsonProperty("acronym")
        override val acronym: String = type

        @get:JsonProperty("settings")
        override var settings: Any? = null

        @get:JsonProperty("color")
        override val color: String = KEY_MOD_COLOR

        companion object : Mod, ValueMod {
            override val type: String = "1K"
            override val mode: Set<OsuMode> = setOf(OsuMode.MANIA)
            override val incompatible: Set<Mod> = setOf(Key2, Key3, Key4, Key5, Key6, Key7, Key8, Key9, Key10)
            override val value: Int = 67108864
        }
    }

    class Key2 : LazerMod() {
        @get:JsonProperty("acronym")
        override val acronym: String = type

        @get:JsonProperty("settings")
        override var settings: Any? = null

        @get:JsonProperty("color")
        override val color: String = KEY_MOD_COLOR

        companion object : Mod, ValueMod {
            override val type: String = "2K"
            override val mode: Set<OsuMode> = setOf(OsuMode.MANIA)
            override val incompatible: Set<Mod> = setOf(Key1, Key3, Key4, Key5, Key6, Key7, Key8, Key9, Key10)
            override val value: Int = 268435456
        }
    }

    class Key3 : LazerMod() {
        @get:JsonProperty("acronym")
        override val acronym: String = type

        @get:JsonProperty("settings")
        override var settings: Any? = null

        @get:JsonProperty("color")
        override val color: String = KEY_MOD_COLOR

        companion object : Mod, ValueMod {
            override val type: String = "3K"
            override val mode: Set<OsuMode> = setOf(OsuMode.MANIA)
            override val incompatible: Set<Mod> = setOf(Key1, Key2, Key4, Key5, Key6, Key7, Key8, Key9, Key10)
            override val value: Int = 134217728
        }
    }

    class Key4 : LazerMod() {
        @get:JsonProperty("acronym")
        override val acronym: String = type

        @get:JsonProperty("settings")
        override var settings: Any? = null

        @get:JsonProperty("color")
        override val color: String = KEY_MOD_COLOR

        companion object : Mod, ValueMod {
            override val type: String = "4K"
            override val mode: Set<OsuMode> = setOf(OsuMode.MANIA)
            override val incompatible: Set<Mod> = setOf(Key1, Key2, Key3, Key5, Key6, Key7, Key8, Key9, Key10)
            override val value: Int = 32768
        }
    }

    class Key5 : LazerMod() {
        @get:JsonProperty("acronym")
        override val acronym: String = type

        @get:JsonProperty("settings")
        override var settings: Any? = null

        @get:JsonProperty("color")
        override val color: String = KEY_MOD_COLOR

        companion object : Mod, ValueMod {
            override val type: String = "5K"
            override val mode: Set<OsuMode> = setOf(OsuMode.MANIA)
            override val incompatible: Set<Mod> = setOf(Key1, Key2, Key3, Key4, Key6, Key7, Key8, Key9, Key10)
            override val value: Int = 65536
        }
    }

    class Key6 : LazerMod() {
        @get:JsonProperty("acronym")
        override val acronym: String = type

        @get:JsonProperty("settings")
        override var settings: Any? = null

        @get:JsonProperty("color")
        override val color: String = KEY_MOD_COLOR

        companion object : Mod, ValueMod {
            override val type: String = "6K"
            override val mode: Set<OsuMode> = setOf(OsuMode.MANIA)
            override val incompatible: Set<Mod> = setOf(Key1, Key2, Key3, Key4, Key5, Key7, Key8, Key9, Key10)
            override val value: Int = 131072
        }
    }

    class Key7 : LazerMod() {
        @get:JsonProperty("acronym")
        override val acronym: String = type

        @get:JsonProperty("settings")
        override var settings: Any? = null

        @get:JsonProperty("color")
        override val color: String = KEY_MOD_COLOR

        companion object : Mod, ValueMod {
            override val type: String = "7K"
            override val mode: Set<OsuMode> = setOf(OsuMode.MANIA)
            override val incompatible: Set<Mod> = setOf(Key1, Key2, Key3, Key4, Key5, Key6, Key8, Key9, Key10)
            override val value: Int = 262144
        }
    }

    class Key8 : LazerMod() {
        @get:JsonProperty("acronym")
        override val acronym: String = type

        @get:JsonProperty("settings")
        override var settings: Any? = null

        @get:JsonProperty("color")
        override val color: String = KEY_MOD_COLOR

        companion object : Mod, ValueMod {
            override val type: String = "8K"
            override val mode: Set<OsuMode> = setOf(OsuMode.MANIA)
            override val incompatible: Set<Mod> = setOf(Key1, Key2, Key3, Key4, Key5, Key6, Key7, Key9, Key10)
            override val value: Int = 524288
        }
    }

    class Key9 : LazerMod() {
        @get:JsonProperty("acronym")
        override val acronym: String = type

        @get:JsonProperty("settings")
        override var settings: Any? = null

        @get:JsonProperty("color")
        override val color: String = KEY_MOD_COLOR

        companion object : Mod, ValueMod {
            override val type: String = "9K"
            override val mode: Set<OsuMode> = setOf(OsuMode.MANIA)
            override val incompatible: Set<Mod> = setOf(Key1, Key2, Key3, Key4, Key5, Key6, Key7, Key8, Key10)
            override val value: Int = 16777216
        }
    }

    class Key10 : LazerMod() {
        @get:JsonProperty("acronym")
        override val acronym: String = type

        @get:JsonProperty("settings")
        override var settings: Any? = null

        @get:JsonProperty("color")
        override val color: String = KEY_MOD_COLOR

        companion object : Mod {
            override val type: String = "10K"
            override val mode: Set<OsuMode> = setOf(OsuMode.MANIA)
            override val incompatible: Set<Mod> = setOf(Key1, Key2, Key3, Key4, Key5, Key6, Key7, Key8, Key9)
        }
    }

    class None : LazerMod() {
        @get:JsonProperty("acronym")
        override val acronym: String = type

        @get:JsonProperty("settings")
        override var settings: Any? = null

        @get:JsonProperty("color")
        override val color: String = "none"

        companion object : Mod {
            override val type: String = ""
            override val mode: Set<OsuMode> = setOf()
            override val incompatible: Set<Mod> = setOf()

        }
    }

    class NoMod : LazerMod() {
        @get:JsonProperty("acronym")
        override val acronym: String = type

        @get:JsonProperty("settings")
        override var settings: Any? = null

        @get:JsonProperty("color")
        override val color: String = "#22AC38"

        companion object : Mod {
            override val type: String = "NM"
            override val mode: Set<OsuMode> = setOf()
            override val incompatible: Set<Mod> = setOf(FreeMod)
        }
    }

    class Extra : LazerMod() {
        @get:JsonProperty("acronym")
        override val acronym: String = type

        @get:JsonProperty("settings")
        override var settings: Any? = null

        @get:JsonProperty("color")
        override val color: String = "#FF9800"

        companion object : Mod {
            override val type: String = "EX"
            override val mode: Set<OsuMode> = setOf()
            override val incompatible: Set<Mod> = setOf()

        }
    }

    class FreeMod : LazerMod() {
        @get:JsonProperty("acronym")
        override val acronym: String = type

        @get:JsonProperty("settings")
        override var settings: Any? = null

        @get:JsonProperty("color")
        override val color: String = "#9922EE"

        companion object : Mod, ValueMod {
            override val type: String = "FM"
            override val mode: Set<OsuMode> = setOf()
            override val incompatible: Set<Mod> = setOf(NoMod)
            override val value: Int = 522171579
        }
    }

    override fun toString() = acronym

    companion object {
        const val FUN_MOD_COLOR = "#EA68A2"
        const val KEY_MOD_COLOR = "#616161"

        inline fun <reified T : Mod> hasMod(mods: List<LazerMod>, type: Collection<T>): Boolean {
            val set = type.map { it.type }.toSet()
            return mods.any {
                set.contains(it.acronym)
            }
        }

        inline fun <reified T : Mod> hasMod(mods: List<LazerMod>, type: T): Boolean {
            return mods.any {
                it.acronym == type.type
            }
        }

        inline fun <reified T : Mod> hasModByString(mods: List<String>, type: T): Boolean {
            return mods.any {
                it.uppercase() == type.type
            }
        }

        // to 内 完全 含有 compare 的 mod
        inline fun <reified T : LazerMod> hasMod(compare: Collection<T>, to: Collection<T>): Boolean {
            if (compare.isEmpty() || to.isEmpty()) return false

            val compareSet = compare.filter { it.acronym != "CL" || it.acronym.isNotEmpty() }.map { it::class }.toSet()
            val toSet = to.filter { it.acronym != "CL" || it.acronym.isNotEmpty() }.map { it::class }.toSet()
            val intersectSet = compareSet.intersect(toSet)
            return intersectSet.size == compareSet.size
        }

        inline fun <reified T : LazerMod> hasMod(mods: List<LazerMod>): Boolean {
            return mods.any {
                it is T
            }
        }

        private val hiddenSet = setOf(
            Hidden::class,
            Flashlight::class,
            Blinds::class,
            FadeIn::class,
        )

        @JvmStatic
        fun containsHidden(mods: List<LazerMod>): Boolean {
            return mods.any { hiddenSet.contains(it::class) }
        }

        @JvmStatic
        fun getModFromAcronym(acronym: String?): LazerMod {
            return when (acronym?.uppercase()) {
                Easy.type -> Easy()
                NoFail.type -> NoFail()
                HalfTime.type -> HalfTime()
                Daycore.type -> Daycore()
                HardRock.type -> HardRock()
                SuddenDeath.type -> SuddenDeath()
                Perfect.type -> Perfect()
                DoubleTime.type -> DoubleTime()
                Nightcore.type -> Nightcore()
                Hidden.type -> Hidden()
                Flashlight.type -> Flashlight()
                Blinds.type -> Blinds()
                StrictTracking.type -> StrictTracking()
                AccuracyChallenge.type -> AccuracyChallenge()
                TargetPractice.type -> TargetPractice()
                DifficultyAdjust.type -> DifficultyAdjust()
                Classic.type -> Classic()
                Random.type -> Random()
                Mirror.type -> Mirror()
                Alternate.type -> Alternate()
                SingleTap.type -> SingleTap()
                Autoplay.type -> Autoplay()
                Cinema.type -> Cinema()
                Relax.type -> Relax()
                Autopilot.type -> Autopilot()
                SpunOut.type -> SpunOut()
                Transform.type -> Transform()
                Wiggle.type -> Wiggle()
                SpinIn.type -> SpinIn()
                Grow.type -> Grow()
                Deflate.type -> Deflate()
                WindUp.type -> WindUp()
                WindDown.type -> WindDown()
                Traceable.type -> Traceable()
                BarrelRoll.type -> BarrelRoll()
                ApproachDifferent.type -> ApproachDifferent()
                Muted.type -> Muted()
                NoScope.type -> NoScope()
                Magnetised.type -> Magnetised()
                Repel.type -> Repel()
                AdaptiveSpeed.type -> AdaptiveSpeed()
                FreezeFrame.type -> FreezeFrame()
                Bubbles.type -> Bubbles()
                Synesthesia.type -> Synesthesia()
                Depth.type -> Depth()
                TouchDevice.type -> TouchDevice()
                ScoreV2.type -> ScoreV2()
                Swap.type -> Swap()
                ConstantSpeed.type -> ConstantSpeed()
                FloatingFruits.type -> FloatingFruits()
                NoRelease.type -> NoRelease()
                FadeIn.type -> FadeIn()
                Cover.type -> Cover()
                DualStages.type -> DualStages()
                Invert.type -> Invert()
                HoldOff.type -> HoldOff()
                Key1.type -> Key1()
                Key2.type -> Key2()
                Key3.type -> Key3()
                Key4.type -> Key4()
                Key5.type -> Key5()
                Key6.type -> Key6()
                Key7.type -> Key7()
                Key8.type -> Key8()
                Key9.type -> Key9()
                Key10.type -> Key10()
                NoMod.type -> NoMod()
                FreeMod.type -> FreeMod()
                Extra.type -> Extra()
                else -> None()
            }
        }

        private val spaceRegex = "\\s+".toRegex()

        @JvmStatic
        fun splitModAcronyms(acronyms: String): List<String> {
            val newStr = acronyms.uppercase()
                .replace(spaceRegex, "")
            if (newStr.length % 2 != 0) {
                throw ModsException(ModsException.Type.MOD_Receive_CharNotPaired)
            }
            val list = newStr.chunked(2)
            return list
        }

        @JvmStatic
        fun getModsList(acronym: String?): List<LazerMod> {
            if (acronym.isNullOrBlank()) return emptyList()
            return getModsList(splitModAcronyms(acronym))
        }

        @JvmStatic
        fun getModsList(mods: List<String>?): List<LazerMod> {
            if (mods.isNullOrEmpty()) return emptyList()
            return mods
                .map { getModFromAcronym(it) }
                .distinctBy { it::class }
                .filter { it !is None }
        }

        @JvmStatic
        fun getModsValue(acronym: String?): Int {
            return getModsList(acronym)
                .mapNotNull {
                    val klass = it::class.companionObjectInstance
                    return@mapNotNull if (klass is ValueMod) {
                        klass.value
                    } else {
                        null
                    }
                }
                .reduceOrNull { sum, i -> sum or i } ?: 0
        }

        @JvmStatic
        fun getModsValue(mods: List<LazerMod>?): Int {
            if (mods.isNullOrEmpty()) return 0
            return mods.mapNotNull {
                val klass = it::class.companionObjectInstance
                return@mapNotNull if (klass is ValueMod) {
                    klass.value
                } else {
                    null
                }
            }.reduceOrNull { sum, i -> sum or i } ?: 0
        }

        /**
         * 原 speed 方法
         */
        @JvmStatic
        fun getSpeedChange(mod: LazerMod): Float? {
            return when (mod) {
                is HalfTime -> mod.speedChange ?: 0.75f
                is Daycore -> mod.speedChange ?: 0.7f
                is DoubleTime -> mod.speedChange ?: 1.5f
                is Nightcore -> mod.speedChange ?: 1.5f
                is WindUp -> mod.initialRate ?: 1f
                is WindDown -> mod.initialRate ?: 1f
                is AdaptiveSpeed -> mod.initialRate ?: 1f
                else -> null
            }
        }

        /**
         * 原 speed final 方法
         */
        @JvmStatic
        fun getFinalSpeed(mod: LazerMod): Float? {
            return when (mod) {
                is HalfTime -> mod.speedChange ?: 0.75f
                is Daycore -> mod.speedChange ?: 0.7f
                is DoubleTime -> mod.speedChange ?: 1.5f
                is Nightcore -> mod.speedChange ?: 1.5f
                is WindUp -> mod.initialRate ?: 1.5f
                is WindDown -> mod.initialRate ?: 0.75f
                is AdaptiveSpeed -> mod.initialRate ?: 1f // ?
                else -> null
            }
        }

        @JvmStatic
        fun getModSpeedForStarCalculate(mods: List<LazerMod>?): Float {
            if (mods.isNullOrEmpty()) return 1f
            var f: Float?
            for (mod in mods) {
                f = getFinalSpeed(mod)
                if (f != null) {
                    return f
                }
            }
            return 1f
        }

        @JvmStatic
        fun getModSpeed(mods: List<LazerMod>?): Float {
            if (mods.isNullOrEmpty()) return 1f
            var f: Float?
            for (mod in mods) {
                f = getSpeedChange(mod)
                if (f != null) {
                    return f
                }
            }
            return 1f
        }

        @JvmStatic
        fun hasStarRatingChange(mods: List<LazerMod>?): Boolean {
            if (mods.isNullOrEmpty()) return false
            return mods.any {
                it is Easy ||
                        it is HardRock ||
                        it is HalfTime ||
                        it is Daycore ||
                        it is DoubleTime ||
                        it is Nightcore ||
                        it is WindUp ||
                        it is WindDown ||
                        it is Flashlight ||
                        it is TouchDevice ||
                        it is DifficultyAdjust ||
                        it is AdaptiveSpeed
            }
        }

        @JvmStatic
        fun noStarRatingChange(mods: List<LazerMod>?) = hasStarRatingChange(mods).not()
    }
}