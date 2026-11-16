package hu.szakdolgozat.borbely.slot;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * A naiv (O(n·k)) és a csúszóablakos (O(n)) slotkereső összehasonlító mérése JMH-val
 * (Java Microbenchmark Harness, az OpenJDK mérőkeretrendszere). A JMH bemelegíti a JIT-fordítót,
 * külön JVM-ben mér, és a mért értékek szórását is megadja.
 *
 * <p>A mért esetek (minden kombináció):
 * <ul>
 *   <li>{@code slots}: 96 = egy nap (a rendszer naponta számol), 96000 = mesterségesen nagy bemenet
 *       a futásidő növekedésének bemutatására;</li>
 *   <li>{@code k}: 2, 4, 8, 16, 32 slot = 30 perces, 1, 2, 4 és 8 órás szolgáltatás;</li>
 *   <li>{@code pattern}:
 *     <ul>
 *       <li>ALL_FREE: teljesen szabad nap. Szinte minden slot találat, ezért a mért időt nagyrészt
 *           a találati lista felépítése adja, ami a két változatnál azonos;</li>
 *       <li>RANDOM_70: a slotok kb. 70%-a szabad, rögzített véletlen-kezdőértékkel. A naiv változat
 *           az első foglalt slotnál kilép, ezért itt a gyakorlatban ő is közel lineáris;</li>
 *       <li>NEAR_MISS: mindig k−1 szabad slot, utána egy foglalt. Egyetlen találat sincs, a naiv
 *           változat mégis kezdőpontonként átlagosan (k+1)/2 slotot vizsgál (összesen kb. n·k/2 lépés),
 *           a csúszóablakos kb. 2n-et. Ez tisztán a két algoritmus különbségét méri.</li>
 *     </ul>
 *   </li>
 * </ul>
 * Mindkét változat a valódi {@link SlotFinder#findStarts} metódust futtatja.
 *
 * <p>Futtatás a {@code backend} mappából (kb. 17 perc; töltőn, és közben ne fusson más nehéz program):
 * <pre>
 * .\mvnw.cmd -Pbenchmark test-compile exec:exec
 * </pre>
 * Az eredmény a konzolon táblázatként, és a {@code backend/target/slot-benchmark.csv} fájlban jelenik meg.
 * A {@code mvnw test} nem futtatja, mert az osztály neve nem {@code Test}-re végződik.
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(2)
public class SlotFinderBenchmark {

    @Param({"naiv", "csuszoablak"})
    public String algorithm;

    @Param({"96", "96000"})
    public int slots;

    @Param({"2", "4", "8", "16", "32"})
    public int k;

    @Param({"ALL_FREE", "RANDOM_70", "NEAR_MISS"})
    public String pattern;

    private SlotFinder finder;
    private boolean[] free;

    @Setup
    public void prepare() {
        finder = switch (algorithm) {
            case "naiv" -> new NaiveSlotFinder();
            case "csuszoablak" -> new SlidingWindowSlotFinder();
            default -> throw new IllegalArgumentException("Ismeretlen algoritmus: " + algorithm);
        };
        free = new boolean[slots];
        Random random = new Random(42);
        for (int i = 0; i < slots; i++) {
            free[i] = switch (pattern) {
                case "ALL_FREE" -> true;
                case "RANDOM_70" -> random.nextDouble() < 0.7;
                case "NEAR_MISS" -> i % k != k - 1;
                default -> throw new IllegalArgumentException("Ismeretlen minta: " + pattern);
            };
        }
    }

    /** A visszaadott listát a JMH elnyeli, így a JIT nem hagyhatja ki a számítást. */
    @Benchmark
    public List<Integer> findStarts() {
        return finder.findStarts(free, k);
    }

    public static void main(String[] args) throws RunnerException {
        Path target = Files.isDirectory(Path.of("backend", "src")) ? Path.of("backend", "target") : Path.of("target");
        Path result = target.resolve("slot-benchmark.csv").toAbsolutePath();
        Options options = new OptionsBuilder()
                .include(SlotFinderBenchmark.class.getSimpleName())
                .resultFormat(ResultFormatType.CSV)
                .result(result.toString())
                .build();
        new Runner(options).run();
        System.out.println("Eredmény: " + result);
    }
}
