package ir.hoboho.shooting.core.date

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.abs

/**
 * Jalali (Persian solar) calendar conversions.
 *
 * **Storage rule:** the database only stores Gregorian epoch days. Jalali values
 * are a presentation concern, derived on demand; no Persian date string is ever
 * the primary representation of a date.
 *
 * **Why a table:** the civil Iranian calendar is not a simple 33-year arithmetic
 * cycle — the official leap years break that cycle (notably around 1403/1404).
 * The table below holds the Gregorian epoch day of Farvardin 1 for Jalali years
 * 1000..2001, generated from the well-tested `jdatetime` conversion tables. It
 * is small (~1000 longs), exact, and makes both conversion directions perfectly
 * inverse. Every anchor used by the app is covered with unit tests.
 */
object JalaliCalendar {

    /** First Jalali year the app accepts. */
    const val FIRST_YEAR = 1000

    /** Last Jalali year the app accepts (the table carries one extra Nowruz). */
    const val LAST_YEAR = 2000

    /**
     * Gregorian epoch day of Farvardin 1, from Jalali year 1000 to 2001.
     * Index `i` corresponds to year `FIRST_YEAR + i`.
     */
    private val NOWRUZ: LongArray = longArrayOf(
        -127390L, -127025L, -126660L, -126295L, -125929L, -125564L, -125199L, -124834L, -124468L, -124103L, -123738L, -123373L,
        -123008L, -122642L, -122277L, -121912L, -121547L, -121181L, -120816L, -120451L, -120086L, -119720L, -119355L, -118990L,
        -118625L, -118259L, -117894L, -117529L, -117164L, -116798L, -116433L, -116068L, -115703L, -115337L, -114972L, -114607L,
        -114242L, -113876L, -113511L, -113146L, -112781L, -112415L, -112050L, -111685L, -111320L, -110955L, -110589L, -110224L,
        -109859L, -109494L, -109128L, -108763L, -108398L, -108033L, -107667L, -107302L, -106937L, -106572L, -106206L, -105841L,
        -105476L, -105111L, -104745L, -104380L, -104015L, -103650L, -103284L, -102919L, -102554L, -102189L, -101823L, -101458L,
        -101093L, -100728L, -100362L, -99997L, -99632L, -99267L, -98902L, -98536L, -98171L, -97806L, -97441L, -97075L,
        -96710L, -96345L, -95980L, -95614L, -95249L, -94884L, -94519L, -94153L, -93788L, -93423L, -93058L, -92692L,
        -92327L, -91962L, -91597L, -91231L, -90866L, -90501L, -90136L, -89770L, -89405L, -89040L, -88675L, -88309L,
        -87944L, -87579L, -87214L, -86849L, -86483L, -86118L, -85753L, -85388L, -85022L, -84657L, -84292L, -83927L,
        -83561L, -83196L, -82831L, -82466L, -82100L, -81735L, -81370L, -81005L, -80639L, -80274L, -79909L, -79544L,
        -79178L, -78813L, -78448L, -78083L, -77717L, -77352L, -76987L, -76622L, -76256L, -75891L, -75526L, -75161L,
        -74796L, -74430L, -74065L, -73700L, -73335L, -72969L, -72604L, -72239L, -71874L, -71508L, -71143L, -70778L,
        -70413L, -70047L, -69682L, -69317L, -68952L, -68586L, -68221L, -67856L, -67491L, -67125L, -66760L, -66395L,
        -66030L, -65664L, -65299L, -64934L, -64569L, -64203L, -63838L, -63473L, -63108L, -62743L, -62377L, -62012L,
        -61647L, -61282L, -60916L, -60551L, -60186L, -59821L, -59455L, -59090L, -58725L, -58360L, -57994L, -57629L,
        -57264L, -56899L, -56533L, -56168L, -55803L, -55438L, -55072L, -54707L, -54342L, -53977L, -53611L, -53246L,
        -52881L, -52516L, -52150L, -51785L, -51420L, -51055L, -50690L, -50324L, -49959L, -49594L, -49229L, -48863L,
        -48498L, -48133L, -47768L, -47402L, -47037L, -46672L, -46307L, -45941L, -45576L, -45211L, -44846L, -44480L,
        -44115L, -43750L, -43385L, -43019L, -42654L, -42289L, -41924L, -41558L, -41193L, -40828L, -40463L, -40097L,
        -39732L, -39367L, -39002L, -38637L, -38271L, -37906L, -37541L, -37176L, -36810L, -36445L, -36080L, -35715L,
        -35349L, -34984L, -34619L, -34254L, -33888L, -33523L, -33158L, -32793L, -32427L, -32062L, -31697L, -31332L,
        -30966L, -30601L, -30236L, -29871L, -29505L, -29140L, -28775L, -28410L, -28044L, -27679L, -27314L, -26949L,
        -26584L, -26218L, -25853L, -25488L, -25123L, -24757L, -24392L, -24027L, -23662L, -23296L, -22931L, -22566L,
        -22201L, -21835L, -21470L, -21105L, -20740L, -20374L, -20009L, -19644L, -19279L, -18913L, -18548L, -18183L,
        -17818L, -17452L, -17087L, -16722L, -16357L, -15991L, -15626L, -15261L, -14896L, -14531L, -14165L, -13800L,
        -13435L, -13070L, -12704L, -12339L, -11974L, -11609L, -11243L, -10878L, -10513L, -10148L, -9782L, -9417L,
        -9052L, -8687L, -8321L, -7956L, -7591L, -7226L, -6860L, -6495L, -6130L, -5765L, -5399L, -5034L,
        -4669L, -4304L, -3938L, -3573L, -3208L, -2843L, -2478L, -2112L, -1747L, -1382L, -1017L, -651L,
        -286L, 79L, 444L, 810L, 1175L, 1540L, 1905L, 2271L, 2636L, 3001L, 3366L, 3732L,
        4097L, 4462L, 4827L, 5193L, 5558L, 5923L, 6288L, 6654L, 7019L, 7384L, 7749L, 8115L,
        8480L, 8845L, 9210L, 9575L, 9941L, 10306L, 10671L, 11036L, 11402L, 11767L, 12132L, 12497L,
        12863L, 13228L, 13593L, 13958L, 14324L, 14689L, 15054L, 15419L, 15785L, 16150L, 16515L, 16880L,
        17246L, 17611L, 17976L, 18341L, 18707L, 19072L, 19437L, 19802L, 20168L, 20533L, 20898L, 21263L,
        21628L, 21994L, 22359L, 22724L, 23089L, 23455L, 23820L, 24185L, 24550L, 24916L, 25281L, 25646L,
        26011L, 26377L, 26742L, 27107L, 27472L, 27838L, 28203L, 28568L, 28933L, 29299L, 29664L, 30029L,
        30394L, 30760L, 31125L, 31490L, 31855L, 32221L, 32586L, 32951L, 33316L, 33681L, 34047L, 34412L,
        34777L, 35142L, 35508L, 35873L, 36238L, 36603L, 36969L, 37334L, 37699L, 38064L, 38430L, 38795L,
        39160L, 39525L, 39891L, 40256L, 40621L, 40986L, 41352L, 41717L, 42082L, 42447L, 42813L, 43178L,
        43543L, 43908L, 44274L, 44639L, 45004L, 45369L, 45734L, 46100L, 46465L, 46830L, 47195L, 47561L,
        47926L, 48291L, 48656L, 49022L, 49387L, 49752L, 50117L, 50483L, 50848L, 51213L, 51578L, 51944L,
        52309L, 52674L, 53039L, 53405L, 53770L, 54135L, 54500L, 54866L, 55231L, 55596L, 55961L, 56327L,
        56692L, 57057L, 57422L, 57787L, 58153L, 58518L, 58883L, 59248L, 59614L, 59979L, 60344L, 60709L,
        61075L, 61440L, 61805L, 62170L, 62536L, 62901L, 63266L, 63631L, 63997L, 64362L, 64727L, 65092L,
        65458L, 65823L, 66188L, 66553L, 66919L, 67284L, 67649L, 68014L, 68380L, 68745L, 69110L, 69475L,
        69840L, 70206L, 70571L, 70936L, 71301L, 71667L, 72032L, 72397L, 72762L, 73128L, 73493L, 73858L,
        74223L, 74589L, 74954L, 75319L, 75684L, 76050L, 76415L, 76780L, 77145L, 77511L, 77876L, 78241L,
        78606L, 78972L, 79337L, 79702L, 80067L, 80433L, 80798L, 81163L, 81528L, 81893L, 82259L, 82624L,
        82989L, 83354L, 83720L, 84085L, 84450L, 84815L, 85181L, 85546L, 85911L, 86276L, 86642L, 87007L,
        87372L, 87737L, 88103L, 88468L, 88833L, 89198L, 89564L, 89929L, 90294L, 90659L, 91025L, 91390L,
        91755L, 92120L, 92486L, 92851L, 93216L, 93581L, 93946L, 94312L, 94677L, 95042L, 95407L, 95773L,
        96138L, 96503L, 96868L, 97234L, 97599L, 97964L, 98329L, 98695L, 99060L, 99425L, 99790L, 100156L,
        100521L, 100886L, 101251L, 101617L, 101982L, 102347L, 102712L, 103078L, 103443L, 103808L, 104173L, 104539L,
        104904L, 105269L, 105634L, 105999L, 106365L, 106730L, 107095L, 107460L, 107826L, 108191L, 108556L, 108921L,
        109287L, 109652L, 110017L, 110382L, 110748L, 111113L, 111478L, 111843L, 112209L, 112574L, 112939L, 113304L,
        113670L, 114035L, 114400L, 114765L, 115131L, 115496L, 115861L, 116226L, 116592L, 116957L, 117322L, 117687L,
        118052L, 118418L, 118783L, 119148L, 119513L, 119879L, 120244L, 120609L, 120974L, 121340L, 121705L, 122070L,
        122435L, 122801L, 123166L, 123531L, 123896L, 124262L, 124627L, 124992L, 125357L, 125723L, 126088L, 126453L,
        126818L, 127184L, 127549L, 127914L, 128279L, 128645L, 129010L, 129375L, 129740L, 130105L, 130471L, 130836L,
        131201L, 131566L, 131932L, 132297L, 132662L, 133027L, 133393L, 133758L, 134123L, 134488L, 134854L, 135219L,
        135584L, 135949L, 136315L, 136680L, 137045L, 137410L, 137776L, 138141L, 138506L, 138871L, 139237L, 139602L,
        139967L, 140332L, 140698L, 141063L, 141428L, 141793L, 142158L, 142524L, 142889L, 143254L, 143619L, 143985L,
        144350L, 144715L, 145080L, 145446L, 145811L, 146176L, 146541L, 146907L, 147272L, 147637L, 148002L, 148368L,
        148733L, 149098L, 149463L, 149829L, 150194L, 150559L, 150924L, 151290L, 151655L, 152020L, 152385L, 152751L,
        153116L, 153481L, 153846L, 154211L, 154577L, 154942L, 155307L, 155672L, 156038L, 156403L, 156768L, 157133L,
        157499L, 157864L, 158229L, 158594L, 158960L, 159325L, 159690L, 160055L, 160421L, 160786L, 161151L, 161516L,
        161882L, 162247L, 162612L, 162977L, 163343L, 163708L, 164073L, 164438L, 164804L, 165169L, 165534L, 165899L,
        166264L, 166630L, 166995L, 167360L, 167725L, 168091L, 168456L, 168821L, 169186L, 169552L, 169917L, 170282L,
        170647L, 171013L, 171378L, 171743L, 172108L, 172474L, 172839L, 173204L, 173569L, 173935L, 174300L, 174665L,
        175030L, 175396L, 175761L, 176126L, 176491L, 176857L, 177222L, 177587L, 177952L, 178317L, 178683L, 179048L,
        179413L, 179778L, 180144L, 180509L, 180874L, 181239L, 181605L, 181970L, 182335L, 182700L, 183066L, 183431L,
        183796L, 184161L, 184527L, 184892L, 185257L, 185622L, 185988L, 186353L, 186718L, 187083L, 187449L, 187814L,
        188179L, 188544L, 188910L, 189275L, 189640L, 190005L, 190370L, 190736L, 191101L, 191466L, 191831L, 192197L,
        192562L, 192927L, 193292L, 193658L, 194023L, 194388L, 194753L, 195119L, 195484L, 195849L, 196214L, 196580L,
        196945L, 197310L, 197675L, 198041L, 198406L, 198771L, 199136L, 199502L, 199867L, 200232L, 200597L, 200963L,
        201328L, 201693L, 202058L, 202423L, 202789L, 203154L, 203519L, 203884L, 204250L, 204615L, 204980L, 205345L,
        205711L, 206076L, 206441L, 206806L, 207172L, 207537L, 207902L, 208267L, 208633L, 208998L, 209363L, 209728L,
        210094L, 210459L, 210824L, 211189L, 211555L, 211920L, 212285L, 212650L, 213016L, 213381L, 213746L, 214111L,
        214476L, 214842L, 215207L, 215572L, 215937L, 216303L, 216668L, 217033L, 217398L, 217764L, 218129L, 218494L,
        218859L, 219225L, 219590L, 219955L, 220320L, 220686L, 221051L, 221416L, 221781L, 222147L, 222512L, 222877L,
        223242L, 223608L, 223973L, 224338L, 224703L, 225069L, 225434L, 225799L, 226164L, 226529L, 226895L, 227260L,
        227625L, 227990L, 228356L, 228721L, 229086L, 229451L, 229817L, 230182L, 230547L, 230912L, 231278L, 231643L,
        232008L, 232373L, 232739L, 233104L, 233469L, 233834L, 234200L, 234565L, 234930L, 235295L, 235661L, 236026L,
        236391L, 236756L, 237122L, 237487L, 237852L, 238217L,
    )

    init {
        require(NOWRUZ.size == LAST_YEAR - FIRST_YEAR + 2) {
            "Nowruz table must cover $FIRST_YEAR..${LAST_YEAR + 1}"
        }
        for (i in 1 until NOWRUZ.size) {
            require(NOWRUZ[i] > NOWRUZ[i - 1]) { "Nowruz table must be strictly increasing at $i" }
        }
    }

    /** Gregorian epoch day of Farvardin 1 of [year]. */
    fun nowruzEpochDay(year: Int): Long {
        require(year in FIRST_YEAR..(LAST_YEAR + 1)) { "Jalali year out of range: $year" }
        return NOWRUZ[year - FIRST_YEAR]
    }

    /** True when Esfand of [year] has 30 days. */
    fun isLeapYear(year: Int): Boolean {
        require(year in FIRST_YEAR..LAST_YEAR) { "Jalali year out of range: $year" }
        return nowruzEpochDay(year + 1) - nowruzEpochDay(year) == 366L
    }

    /** Days in a Jalali month: 31 for Farvardin..Shahrivar, 30 after, 29/30 Esfand. */
    fun daysInMonth(year: Int, month: Int): Int {
        require(month in 1..12) { "Jalali month must be in 1..12, was $month" }
        return when {
            month <= 6 -> 31
            month <= 11 -> 30
            isLeapYear(year) -> 30
            else -> 29
        }
    }

    /** Gregorian epoch day -> Jalali date. */
    fun toJalali(epochDay: Long): JalaliDate {
        require(epochDay >= NOWRUZ.first() && epochDay < NOWRUZ.last()) {
            "Epoch day $epochDay is outside the supported Jalali range"
        }
        // Last Nowruz <= epochDay.
        var low = 0
        var high = NOWRUZ.size - 1
        while (low < high) {
            val mid = (low + high + 1) / 2
            if (NOWRUZ[mid] <= epochDay) low = mid else high = mid - 1
        }
        val year = FIRST_YEAR + low
        val dayOfYear = (epochDay - NOWRUZ[low] + 1).toInt()
        return if (dayOfYear <= 186) {
            JalaliDate(year, (dayOfYear - 1) / 31 + 1, (dayOfYear - 1) % 31 + 1)
        } else {
            val rest = dayOfYear - 187
            JalaliDate(year, rest / 30 + 7, rest % 30 + 1)
        }
    }

    /** Jalali date -> Gregorian epoch day. */
    fun toEpochDay(jalali: JalaliDate): Long {
        require(jalali.year in FIRST_YEAR..LAST_YEAR) { "Jalali year out of range: ${jalali.year}" }
        require(jalali.day in 1..daysInMonth(jalali.year, jalali.month)) {
            "Invalid Jalali day: ${jalali.day} for ${jalali.year}/${jalali.month}"
        }
        val daysBeforeMonth = if (jalali.month <= 6) {
            (jalali.month - 1) * 31
        } else {
            186 + (jalali.month - 7) * 30
        }
        return nowruzEpochDay(jalali.year) + daysBeforeMonth + jalali.day - 1
    }

    /** Jalali date -> Gregorian epoch day, from raw components. */
    fun toEpochDay(year: Int, month: Int, day: Int): Long = toEpochDay(JalaliDate(year, month, day))

    /** Saturday = 0 ... Friday = 6. Epoch day 0 (1970-01-01) was a Thursday. */
    fun dayOfWeek(epochDay: Long): Int = Math.floorMod(epochDay + 5L, 7L).toInt()

    fun weekdayName(epochDay: Long): String = JalaliDate.weekdayNames[dayOfWeek(epochDay)]

    fun shortWeekdayName(epochDay: Long): String = JalaliDate.shortWeekdayNames[dayOfWeek(epochDay)]

    fun todayEpochDay(): Long = LocalDate.now().toEpochDay()

    /** Gregorian epoch day for an epoch-millisecond timestamp in the device time zone. */
    fun fromEpochMilli(epochMilli: Long): Long =
        Instant.ofEpochMilli(epochMilli).atZone(ZoneId.systemDefault()).toLocalDate().toEpochDay()

    fun todayJalali(): JalaliDate = toJalali(todayEpochDay())

    /** First day of the Jalali month containing [epochDay]. */
    fun firstDayOfMonth(epochDay: Long): Long {
        val j = toJalali(epochDay)
        return toEpochDay(j.year, j.month, 1)
    }

    /** Last day of the Jalali month containing [epochDay]. */
    fun lastDayOfMonth(epochDay: Long): Long {
        val j = toJalali(epochDay)
        return toEpochDay(j.year, j.month, daysInMonth(j.year, j.month))
    }

    /** First day of the Jalali year containing [epochDay]. */
    fun firstDayOfYear(epochDay: Long): Long = toEpochDay(toJalali(epochDay).year, 1, 1)

    /** Last day of the Jalali year containing [epochDay]. */
    fun lastDayOfYear(epochDay: Long): Long {
        val year = toJalali(epochDay).year
        return toEpochDay(year, 12, daysInMonth(year, 12))
    }

    /** Start of the Persian week (Saturday) containing [epochDay]. */
    fun startOfWeek(epochDay: Long): Long = epochDay - dayOfWeek(epochDay)

    /** Add Jalali months, clamping the day to the length of the target month. */
    fun plusMonths(epochDay: Long, months: Int): Long {
        val j = toJalali(epochDay)
        val total = j.year * 12 + (j.month - 1) + months
        val year = Math.floorDiv(total, 12)
        val month = Math.floorMod(total, 12) + 1
        val day = minOf(j.day, daysInMonth(year, month))
        return toEpochDay(year, month, day)
    }

    /** Number of days between two epoch days (inclusive of both ends). */
    fun inclusiveDays(startEpochDay: Long, endEpochDay: Long): Int {
        require(endEpochDay >= startEpochDay) { "End must not precede start" }
        return (abs(endEpochDay - startEpochDay) + 1).toInt()
    }
}
