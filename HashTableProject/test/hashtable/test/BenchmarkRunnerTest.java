package hashtable.test;

import hashtable.bench.BenchmarkRunner;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class BenchmarkRunnerTest {

    private BenchmarkRunnerTest() {
    }

    public static void runAllTests() {
        testCountAndTimeCsvRows();
    }

    private static void testCountAndTimeCsvRows() {
        Path csv = null;
        try {
            csv = Files.createTempFile("benchmark-runner-test", ".csv");
            Files.delete(csv);

            String[] commonArgs = {
                "--rq", "RQ1", "--algo", "sc", "--m", "64", "--alpha", "0.5",
                "--seed", "42", "--repeat", "1", "--csv-only", "--out", csv.toString()
            };
            String[] countArgs = append(commonArgs, "--count");
            BenchmarkRunner.main(countArgs);
            BenchmarkRunner.main(append(commonArgs, "--mode", "time"));
            BenchmarkRunner.main(append(commonArgs, "--mode", "count", "--query", "miss"));

            List<String> lines = Files.readAllLines(csv, StandardCharsets.UTF_8);
            assertEquals(BenchmarkRunner.CSV_HEADER, lines.get(0), "CSV header");
            assertEquals(4, lines.size(), "CSV row count");

            Map<String, Integer> column = columns(lines.get(0));
            String[] countRow = lines.get(1).split(",", -1);
            String[] timeRow = lines.get(2).split(",", -1);
            String[] missCountRow = lines.get(3).split(",", -1);
            assertEquals(21, countRow.length, "count column count");
            assertEquals(21, timeRow.length, "time column count");
            assertEquals(21, missCountRow.length, "miss count column count");
            assertTrue(!countRow[0].equals(timeRow[0]), "count and time run IDs must be unique");
            assertTrue(!countRow[0].equals(missCountRow[0]), "hit and miss run IDs must be unique");

            assertEquals("", value(countRow, column, "elapsed_ms"), "count elapsed_ms");
            assertTrue(!value(countRow, column, "ops_count").isEmpty(), "count ops_count");
            assertEquals("", value(timeRow, column, "ops_count"), "time ops_count");
            assertTrue(!value(timeRow, column, "elapsed_ms").isEmpty(), "time elapsed_ms");

            for (String[] row : new String[][] {countRow, timeRow}) {
                Double.parseDouble(value(row, column, "build_ms"));
                Long.parseLong(value(row, column, "heap_before_bytes"));
                Long.parseLong(value(row, column, "heap_peak_bytes"));
                assertTrue(!value(row, column, "output_check").isEmpty(), "output_check");
                assertEquals("ok", value(row, column, "status"), "status");
            }
        } catch (Exception e) {
            throw new AssertionError("BenchmarkRunner CSV mode test failed", e);
        } finally {
            if (csv != null) {
                try {
                    Files.deleteIfExists(csv);
                } catch (Exception e) {
                    throw new AssertionError("Could not remove temporary benchmark CSV", e);
                }
            }
        }
    }

    private static String[] append(String[] args, String... extra) {
        String[] result = new String[args.length + extra.length];
        System.arraycopy(args, 0, result, 0, args.length);
        System.arraycopy(extra, 0, result, args.length, extra.length);
        return result;
    }

    private static Map<String, Integer> columns(String header) {
        String[] names = header.split(",", -1);
        Map<String, Integer> result = new HashMap<>();
        for (int i = 0; i < names.length; i++) {
            result.put(names[i], i);
        }
        return result;
    }

    private static String value(String[] row, Map<String, Integer> columns, String name) {
        return row[columns.get(name)];
    }

    private static void assertEquals(Object expected, Object actual, String message) {
        if (!expected.equals(actual)) {
            throw new AssertionError(message + ": expected " + expected + ", got " + actual);
        }
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
