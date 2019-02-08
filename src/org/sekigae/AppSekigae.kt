package org.sekigae

import javafx.application.Platform
import javafx.collections.ListChangeListener
import javafx.embed.swing.SwingFXUtils
import javafx.scene.control.ButtonType
import javafx.scene.control.Dialog
import javafx.scene.control.TextField
import javafx.scene.control.TextFormatter
import javafx.scene.effect.*
import javafx.scene.input.KeyCode
import javafx.scene.paint.Color
import javafx.scene.text.TextAlignment
import javafx.stage.FileChooser
import javafx.stage.Stage
import javafx.stage.StageStyle
import org.gakeutil.*
import org.gakeutil.event.ApplicationStartEvent
import org.gakeutil.event.LoadAudiosEvent
import org.gakeutil.event.LoadTexturesEvent
import org.gakeutil.render.IRenderable
import org.gakeutil.render.Renderer
import org.gakeutil.util.*
import java.lang.IllegalStateException
import java.net.URI
import java.nio.file.Files
import java.nio.file.Paths
import java.util.function.UnaryOperator
import java.util.logging.Logger
import java.util.stream.Stream
import javax.imageio.ImageIO
import kotlin.random.Random

fun main(args: Array<String>) = AppCore.launch(AppSekigae::class, args)

open class Person private constructor(val isMan: Boolean, val index: Int/* 0 .. 19 */) {

    companion object {

        val array = Array(40) { Person(it < 20, it % 20) }

        val men = array.filter { it.isMan }.toTypedArray()

        val women = array.filter { !it.isMan }.toTypedArray()

        init {

        }

        operator fun get(isMan: Boolean, bango: Int) = array[(bango-1) + if(isMan) 0 else 20]

        fun boy(bango: Int) = get(true, bango)

        fun girl(bango: Int) = get(false, bango)

        fun getDummy(value: String): Person = DummyPerson(value)

    }

    internal class DummyPerson(val value: String): Person(false, -1)

    override fun toString() = "Person($isMan, $index)"

    override fun equals(other: Any?) = if(other !is Person)
        false
    else
        isMan == other.isMan && index == other.index

    override fun hashCode() = 31 * isMan.hashCode() + index

    fun getSide(array: Array<Person?>): Int {

        val i0 = array.indexOf(this)

        val i1 = i0 + if(i0 % 10 in 5..9) -5 else 5

        return i1

    }

}

class AppSekigae : AppCore("null/", "席替え $Version", scaleOf(1280.0, 720.0), logger = Logger.getAnonymousLogger()) {

    inner class Whiteboard : IGameObj, IRenderable {
        override var align = Align.CENTER
        override var position = posOf(displayInnerScale.w/2.0, displayInnerScale.h - 100.0)
        override var scale = scaleOf(200.0, 60.0)

        override fun render(renderer: Renderer) = renderer {

            stroke += Color.BLACK

            stroke.rect(alignedPos, scale)

            setTextCenter()

            fill += Color.DARKGRAY

            fill.text("W B", position, FontUtil.font(36.0))

        }

    }

    inner class Gosha(val index: Int) {

        val width = 300.0
        val height = 120.0

        init {

            require(index in 0..3) { "ERROR!" }

        }

        val offsetX = 60.0

        val offsetY = 40.0

        val offsetP = when(index) {
            0 -> posOf(displayInnerScale.w/2 - ((width + offsetX) * 1.5) - width, 250.0)
            1 -> posOf(displayInnerScale.w/2 - ((width + offsetX) * 0.5) - width, 250.0)
            2 -> posOf(displayInnerScale.w/2 + ((width + offsetX) * 0.5), 250.0)
            else -> posOf(displayInnerScale.w/2 + ((width + offsetX) * 1.5), 250.0)
        }

        fun render(renderer: Renderer, array: Array<String>) = renderer {

            stroke += Color.BLACK

            for(column in 0..5) {

                strokeLine(posOf(0.0, (height)*column) + offsetP, posOf(width, (height)*column) + offsetP)

                if(column != 5) {

                    val left = posOf(0.0, (height)*column) + offsetP + posOf(width/4, height/2)

                    val right = posOf(0.0, (height)*column) + offsetP + posOf(width/4 * 3, height/2)

                    fill += Color.BLACK

//                    fill.oval(left.alignTo(Align.CENTER, scaleOf(10.0)), scaleOf(10.0))
//
//                    fill.oval(right.alignTo(Align.CENTER, scaleOf(10.0)), scaleOf(10.0))

                    setTextCenter()

                    val indexL = (5-column)+(index*10)

                    val indexR = (5-column)+(index*10)+5

                    val scale = scaleOf(width/2, height)

                    if(hanchoSeats.contains(indexL-1)) {

                        fill += Color.GOLD
                        fill.rect(left.alignTo(Align.CENTER, scale), scale)

                    }

                    if(hanchoSeats.contains(indexR-1)) {

                        fill += Color.GOLD
                        fill.rect(right.alignTo(Align.CENTER, scale), scale)

                    }

                    fill += if((indexL-1) % 2 == 0) Color.DARKBLUE else Color.DARKRED

                    fill.text(array[indexL-1], left, FontUtil.font(24.0))

                    fill += if((indexR-1) % 2 == 0) Color.DARKBLUE else Color.DARKRED

                    fill.text(array[indexR-1], right, FontUtil.font(24.0))

                }

            }

            strokeLine(offsetP.plusX(0.0), posOf(0.0, height*5) + offsetP)
            strokeLine(offsetP.plusX(width/2), posOf(width/2, height*5) + offsetP)
            strokeLine(offsetP.plusX(width), posOf(width, height*5) + offsetP)

            setTextCenter()

//            fill += Color.DARKGRAY
//
//            fill.text("W B", position, FontUtil.font(36.0))

        }

    }

    data class HanGroup(
        val man0: Int,
        val man1: Int = man0 + if(isSpecial(man0)) 10 else 6,
        val woman0: Int = man1 - if(isSpecial(man0)) 5 else 1,
        val woman1: Int = man0 + if(isSpecial(man0)) 15 else 1) {

        companion object {

            fun isSpecial(i: Int) = i == 4 || i == 24

        }

        val men = man0 to man1

        val women = woman0 to woman1

    }

    companion object {

        val hans = arrayOf(

            HanGroup(0),
            HanGroup(2),
            HanGroup(10),
            HanGroup(12),
            HanGroup(4, 14, 9, 19),
            HanGroup(20),
            HanGroup(22),
            HanGroup(30),
            HanGroup(32),
            HanGroup(24, 34, 29, 39)

        )

        private val filterer: (Array<Person>)->Boolean = filterer@{

            return@filterer true

        }

    }

    object Version {

        const val major = 1
        const val minor = 0
        const val patch = 0

        override fun toString() = "$major.$minor.$patch"

    }

    val unmovables = mutableMapOf<Person, Int>()//person -> seat

    var hanchoSeats = IntArray(10)

    val pathNames = Paths.get("./names.map")

    val pathHanchos = Paths.get("./hanchos.map")

    val nameArrayMen = Array(20) { "男${it+1}" }
    val nameArrayWomen = Array(20) { "女${it+1}" }

    lateinit var stage: Stage
        private set

    var indexChecking = false

    val whiteboard = Whiteboard()

    val goshas = arrayOf( Gosha(0), Gosha(1), Gosha(2), Gosha(3) )

    val fileChooser = FileChooser().apply {

        extensionFilters.add(FileChooser.ExtensionFilter("Image", "png"))

        initialFileName = "ファイル名.png"

    }

    var loadedLinesHanchos: Array<String>? = null

    fun info(str: String) {

        Dialog<Boolean>().apply {
            contentText = str
            initOwner(stage)
            dialogPane.buttonTypes.add(ButtonType.OK)
        }.show()

    }

    fun warningAndExit(str: String): Nothing {

        stage.hide()

        Dialog<Boolean>().apply {
            contentText = str
            initOwner(stage)
            dialogPane.buttonTypes.add(ButtonType.OK)

        }.show()

        throw Exception()

    }

    fun Person.getName() =
        when {
            this is Person.DummyPerson -> value
            isMan -> nameArrayMen[index]
            else -> nameArrayWomen[index]
        }

    fun <T> Pair<T, T>.random() = if(Random.nextBoolean()) first else second

    override fun onLoadAudios(event: LoadAudiosEvent) { }

    override fun onLoadTextures(event: LoadTexturesEvent) { }

    override fun onStart(event: ApplicationStartEvent) {

        println("Run on $Version")

        stage = event.stage

        event.isFullScreen = false

        event.show()

        if(Files.exists(pathNames) && !Files.isDirectory(pathNames)) {

            val lines = Files.newBufferedReader(pathNames, Charsets.UTF_8).readLines()

            var isMan = true

            for(l in lines) {

                if(l == "~~~")
                    isMan = false

                if('=' !in l)
                    continue

                var (indexS, value) = l.split('=')

                val index = (indexS.toIntOrNull() ?: continue) - 1

                if(value.startsWith("$")) {

                    val tmp = value.drop(1).split(':')
                    value = tmp[0]
                    val person0 = Person[isMan, index+1]
                    val seat = tmp[1].toInt()
                    unmovables[person0] = seat
                    println("SET ${person0.getName()} UNMOVABLE @ $seat")

                }

                if(index !in 0..19 || value.isEmpty())
                    continue

                if(isMan)
                    nameArrayMen[index] = value

                else
                    nameArrayWomen[index] = value

            }

        }

        loadHanchos()

        var isFirstScene = true

        loop {

            if(keyManager.isKeyPressingFirst(KeyCode.SPACE)) {

                isFirstScene = false

                generate()

            }

            if(keyManager.isKeyPressing(KeyCode.CONTROL)) {

                if(keyManager.isKeyPressingFirst(KeyCode.H)) {

                    info("""
                        CTRL + S -> スクショ
                        CTRL + I -> //[DEBUG-indexChecking]
                        CTRL + L -> 班長席更新
                    """.trimIndent())

                }

                if(keyManager.isKeyPressingFirst(KeyCode.S)) {

                    saveScreenShot()

                }

                if(keyManager.isKeyPressingFirst(KeyCode.I)) {

                    isFirstScene = false

                    indexChecking = !indexChecking

                    generate()

                }

                if(keyManager.isKeyPressingFirst(KeyCode.L)) {

                    isFirstScene = false

                    loadHanchos()

                    generate()

                }

            }

            if(isFirstScene)
                renderFirst()

            else
                rerender()

        }.start()

    }

    var scale0 = scaleOf()

    var switch0 = true

    fun renderFirst() = renderer {

        if(switch0) {

            scale0 += scaleOf(0.0, 0.1)

            if(scale0.h > 10.0) {

                scale0 = scaleOf(3.0)
                switch0 = false

            }

        } else {

            scale0 += scaleOf(0.1, 0.0)

            if(scale0.w > 10.0) {

                scale0 = scaleOf(3.0)
                switch0 = true

            }

        }

        fill *= Color.GOLD

        lineWidth = 2.0

        both {

            fill += Color.WHITE
            stroke += Color.DARKGRAY

            setTextCenter()

            withEffect(BoxBlur(scale0.w, scale0.h, 1)) {

                text("Spaceキーでランダムに席ぎめ", posOf(innerScale/2.0), FontUtil.font(64.0))

                text("Ctrl+Hでヘルプ", posOf(innerScale/2.0).minusY(80.0), FontUtil.font(48.0))

            }

        }

        lineWidth = 0.5

    }

    fun rerender() = renderer {

        val array = if(indexChecking) Array(40) { it.toString() } else arrayForRender.clone()

        fill *= Color.WHITESMOKE

        whiteboard.render(this)

        goshas.forEach { it.render(this, array) }

        fill += Color.BLACK

        fill {

            setTextCenter()

            text("""
                班長
                1班 ${array[hanchoSeats[0]]}    6班:${array[hanchoSeats[5]]}
                2班:${array[hanchoSeats[1]]}    7班:${array[hanchoSeats[6]]}
                3班:${array[hanchoSeats[2]]}    8班:${array[hanchoSeats[7]]}
                4班:${array[hanchoSeats[3]]}    9班:${array[hanchoSeats[8]]}
                5班:${array[hanchoSeats[4]]}   10班:${array[hanchoSeats[9]]}
            """.trimIndent(), center, FontUtil.font(16.0))

        }

    }

    fun loadHanchos() {

        if(Files.exists(pathHanchos) && !Files.isDirectory(pathHanchos)) {

            if(loadedLinesHanchos == null)
                loadedLinesHanchos = Files.newBufferedReader(pathHanchos).readLines().toTypedArray()

        } else {

            warningAndExit("班長リスト: hanchos.map がありません！")

        }

        val lines = loadedLinesHanchos!!

        try {

            var hanchoCount = 0

            val array00 = List<Int>(10) { it }.shuffled()

            for(l in lines) {

                try {

                    if(l.startsWith('#'))
                        continue

                    val isMan = l.startsWith('m')

                    val hanGroup = array00[hanchoCount]

                    val index = l.drop(1).toInt() - 1

                    require(hanGroup in 0..9)

                    val seat = if(isMan)
                        hans[hanGroup].men.random()
                    else
                        hans[hanGroup].women.random()

                    val person0 = Person[isMan, index+1]

                    println("SET ${person0.getName()} @ SEAT-INDEX:$seat HANCHO")

                    unmovables[Person[isMan, index+1]] = seat

                    hanchoSeats[hanGroup] = seat

                    ++hanchoCount

                } catch(e: Throwable) {

                    continue

                }

            }

            require(hanchoCount == 10)

        } catch(e: Throwable) {

            e.printStackTrace()

            warningAndExit("班長リスト: hanchos.map で班長を(適切に)設定してください！")

        }

    }

    private var arrayForRender: Array<String> = emptyArray()

    fun generate() {

        println("START GENERATE")

        fun genSeats(): Array<Person> {

            val seats = arrayOfNulls<Person>(40) //index = SHUSSEKI-BANGO; value = NAME

            unmovables.forEach { person, seat ->

                require(if(person.isMan) seat % 2 == 0 else seat % 2 != 0)

                seats[seat] = person

            }

            Person.men.filter { it !in seats }.shuffled()

            val menNormal = Person.men.filter { it !in seats }.shuffled()

            var tmp00 = 0

            menNormal.forEach { s ->

                //index is 0..19

                while(seats[tmp00] != null) {

                    tmp00 += 2

                }

                require(tmp00 in 0..39 && tmp00 % 2 == 0)

                seats[tmp00] = s

            }

            val womenNormal = Person.women.filter { it !in seats }.shuffled()

            tmp00 = 1

            womenNormal.forEach { s ->

                //index is 0..19

                while(seats[tmp00] != null) {

                    tmp00 += 2

                }

                require(tmp00 in 0..39 && tmp00 % 2 != 0)

                seats[tmp00] = s

            }

            for(i in 0..39) {

                if(indexChecking)
                    seats[i] = Person.getDummy(i.toString())

            }

            return seats.mapIndexed { index, it -> it ?: warningAndExit("BUG!!") }.toTypedArray()

        }

        var safeCounter = 50000

        var seats: Array<Person>

        while(true) {

            if(safeCounter < 0) {

                info("TIME OUT...")
                return

            }

            --safeCounter

            seats = genSeats()

            if(filterer(seats))
                break

        }

        arrayForRender = seats.map { it.getName() }.toTypedArray()

        println("FINISHED GENERATE SUCCESSFULLY")

    }

    fun saveScreenShot() {

        val target = fileChooser.showSaveDialog(stage)

        ImageIO.write(SwingFXUtils.fromFXImage(renderer.snapshot(), null), "png", target)

    }

}